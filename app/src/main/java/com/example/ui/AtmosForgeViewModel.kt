package com.example.ui

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.SceneBlueprint
import com.example.api.GeminiPromptBuilder
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import com.example.dsp.AudioSynthesisEngine
import com.example.repository.ProjectRepository
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface GenerationUiState {
    object Idle : GenerationUiState
    data class Analyzing(val step: String) : GenerationUiState
    data class BlueprintReady(val blueprints: List<SceneBlueprint>) : GenerationUiState
    data class Synthesizing(val blueprints: List<SceneBlueprint>) : GenerationUiState
    data class Ready(val blueprints: List<SceneBlueprint>, val files: Map<String, File>) : GenerationUiState
    data class Error(val message: String) : GenerationUiState
}

class AtmosForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(db.projectDao())
    private val audioEngine = AudioSynthesisEngine(application)
    private val promptBuilder = GeminiPromptBuilder()

    // Database state reactive representation
    val projectHistory = repository.allProjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Editor field states
    val titleState = MutableStateFlow("Cinematic Soundtrack")
    val inputTextState = MutableStateFlow("A dark desert wind rumbles across the ruins. A soft, lonely humming of a distant traveler rises in the silence. Suddenly, a massive impact echoes, followed by a tense, rising metallic whistle.")
    val inputTypeState = MutableStateFlow("Script") // "Script", "Story", "Timeline", "Subtitle"
    val durationModeState = MutableStateFlow("Auto") // "Auto", "Custom"
    val customDurationSecondsState = MutableStateFlow(40)
    val intensityState = MutableStateFlow("Medium") // "Low", "Medium", "High"
    val atmosphereState = MutableStateFlow("Cinematic") // "Natural", "Spiritual", "Cinematic", "Dark", "Documentary"
    val voiceStyleState = MutableStateFlow("Female Humming") // "Male Humming", "Female Humming", "Mixed Choir", ...
    val natureBlendState = MutableStateFlow(65f) // 0-100f
    val fxBlendState = MutableStateFlow(80f) // 0-100f

    val enableTextVfxState = MutableStateFlow(true)
    val uploadedAudioPathState = MutableStateFlow<String?>(null)
    val uploadedAudioNameState = MutableStateFlow<String?>(null)
    val uploadedAudioDurationState = MutableStateFlow<Int?>(null)
    val uploadedMixVolumeState = MutableStateFlow(80f) // 0-100f
    val playbackOptionState = MutableStateFlow("blended") // "blended" or "pure"

    // Synthesis states
    private val _uiState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val uiState: StateFlow<GenerationUiState> = _uiState.asStateFlow()

    private val _sampleRateState = MutableStateFlow(44100) // Default 44.1kHz
    val sampleRateState: StateFlow<Int> = _sampleRateState.asStateFlow()

    // MediaPlayer related states
    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playProgress = MutableStateFlow(0f) // 0f to 1f
    val playProgress: StateFlow<Float> = _playProgress.asStateFlow()

    private val _playDurationMs = MutableStateFlow(0)
    val playDurationMs: StateFlow<Int> = _playDurationMs.asStateFlow()

    private val _playCurrentPositionMs = MutableStateFlow(0)
    val playCurrentPositionMs: StateFlow<Int> = _playCurrentPositionMs.asStateFlow()

    private var playbackTrackerJob: Job? = null

    init {
        // Automatically prefill sample text based on inputType changes
        viewModelScope.launch {
            inputTypeState.collect { type ->
                if (inputTextState.value.isBlank() || inputTextState.value.contains("Opening scene") || inputTextState.value.contains("desert wind")) {
                    inputTextState.value = getTemplateForType(type)
                }
            }
        }
    }

    private fun getTemplateForType(type: String): String {
        return when (type) {
            "Timeline" -> """
                00:00 Silent drone and distant whispering breeze
                00:15 Soft spiritual female humming begins
                00:30 Tension swell begins with heartbeat pulse
                00:45 Giant bass impact reverb tail cuts the quietness
            """.trimIndent()
            "Subtitle" -> """
                1
                00:00:01,000 --> 00:00:10,000
                [A deep cave echo wind is whistling]
                
                2
                00:00:10,500 --> 00:00:30,000
                [Gentle choir humming starts in the background]
                
                3
                00:00:31,000 --> 00:00:45,000
                [A soaring swoosh and final dark riser builds]
            """.trimIndent()
            "Story" -> """
                The old library was completely empty. Only the soft rustling of autumn leaves outside could be heard. She sat down, closed her eyes, and let out a deep, quiet breath. From nowhere, an ethereal humming filled the ancient stone arches, raising a powerful surge of tension.
            """.trimIndent()
            else -> "A dark desert wind rumbles across the ruins. A soft, lonely humming of a distant traveler rises in the silence. Suddenly, a massive impact echoes, followed by a tense, rising metallic whistle."
        }
    }

    fun setSampleRate(rate: Int) {
        _sampleRateState.value = rate
    }

    fun setUploadedAudio(path: String, name: String, durationSec: Int) {
        uploadedAudioPathState.value = path
        uploadedAudioNameState.value = name
        uploadedAudioDurationState.value = durationSec
        
        // Auto-configure soundscape length to match uploaded narration length
        durationModeState.value = "Custom"
        customDurationSecondsState.value = durationSec.coerceIn(15, 120)
    }

    fun clearUploadedAudio() {
        uploadedAudioPathState.value = null
        uploadedAudioNameState.value = null
        uploadedAudioDurationState.value = null
    }

    /**
     * Executes the full generation pipeline using Gemini API for Analysis + scene breakdown, and DSP engine for synthesizing.
     */
    fun startGenerationPipeline() {
        stopPlayback()
        playbackOptionState.value = "blended"
        
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            _uiState.value = GenerationUiState.Error(
                "Gemini API key is not configured in AI Studio's Secrets panel. " +
                "Please add a key named 'GEMINI_API_KEY' with your Google AI API key to proceed."
            )
            return
        }

        val text = inputTextState.value
        val title = titleState.value
        val inputType = inputTypeState.value
        val durationMode = durationModeState.value
        val customDuration = customDurationSecondsState.value
        val intensity = intensityState.value
        val atmosphere = atmosphereState.value
        val voiceStyle = voiceStyleState.value
        val natureBlend = natureBlendState.value / 100f
        val fxBlend = fxBlendState.value / 100f
        val sampleRate = sampleRateState.value

        val enableTextVfx = enableTextVfxState.value
        val uploadedAudioPath = uploadedAudioPathState.value
        val uploadedAudioName = uploadedAudioNameState.value
        val uploadedMixVolume = uploadedMixVolumeState.value / 100f

        viewModelScope.launch {
            try {
                // Step 1: Query Gemini API for structured JSON audio plan
                _uiState.value = GenerationUiState.Analyzing("Step 1/5: Analyzing Narrative Mood & Arc...")
                delay(400) // Brief aesthetic delay for transitions

                _uiState.value = GenerationUiState.Analyzing("Step 2/5: Extracting Scene Timestamps...")
                val systemInstruction = promptBuilder.buildSystemInstruction(intensity, atmosphere, voiceStyle)
                val analysisPrompt = promptBuilder.buildAnalysisPrompt(inputType, text, durationMode, customDuration, uploadedAudioName)

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = analysisPrompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.4f),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                )

                _uiState.value = GenerationUiState.Analyzing("Step 3/5: Running Sound Layer Classification...")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(key, request)
                }

                val jsonResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No response received from Gemini API.")

                _uiState.value = GenerationUiState.Analyzing("Step 4/5: Compiling Audio Blueprint...")
                
                // Parse JSON array using Moshi
                val scenesList = withContext(Dispatchers.Default) {
                    val listType = Types.newParameterizedType(List::class.java, SceneBlueprint::class.java)
                    val adapter = RetrofitClient.moshiInstance.adapter<List<SceneBlueprint>>(listType)
                    adapter.fromJson(jsonResponse.trim())
                } ?: throw Exception("Failed to compile structured audio blueprint. Invalid JSON response.")

                if (scenesList.isEmpty()) {
                    throw Exception("The narrative analysis did not yield any distinct scene segments.")
                }

                _uiState.value = GenerationUiState.Synthesizing(scenesList)

                // Step 5: Procedural sound synthesis mixing
                val files = withContext(Dispatchers.Default) {
                    audioEngine.synthesizeSoundscape(
                        blueprints = scenesList,
                        sampleRate = sampleRate,
                        voiceStyle = voiceStyle,
                        natureBlend = natureBlend,
                        fxBlend = fxBlend,
                        intensity = intensity,
                        atmosphere = atmosphere,
                        uploadedAudioPath = uploadedAudioPath,
                        uploadedMixVolume = uploadedMixVolume,
                        enableTextVfx = enableTextVfx
                    )
                }

                _uiState.value = GenerationUiState.Ready(scenesList, files)

                // Save to Room DB history runs
                val primaryWav = files["full"]?.absolutePath
                val projectRecord = ProjectEntity(
                    title = title,
                    inputText = text,
                    inputType = inputType,
                    durationMode = durationMode,
                    durationSeconds = customDuration,
                    intensity = intensity,
                    atmosphere = atmosphere,
                    voiceStyle = voiceStyle,
                    natureBlend = (natureBlend * 100f).toInt(),
                    fxBlend = (fxBlend * 100f).toInt(),
                    blueprintJson = jsonResponse,
                    audioFilePath = primaryWav
                )
                repository.insertProject(projectRecord)

                // Initialize player for immediate playback pre-view
                primaryWav?.let { initPlayer(it) }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = GenerationUiState.Error(
                    e.localizedMessage ?: "Sound generation failed due to an unexpected connection error."
                )
            }
        }
    }

    /**
     * Load an existing project record from history
     */
    fun selectProjectFromHistory(projectRecord: ProjectEntity) {
        stopPlayback()
        playbackOptionState.value = "blended"
        titleState.value = projectRecord.title
        inputTextState.value = projectRecord.inputText
        inputTypeState.value = projectRecord.inputType
        durationModeState.value = projectRecord.durationMode
        customDurationSecondsState.value = projectRecord.durationSeconds
        intensityState.value = projectRecord.intensity
        atmosphereState.value = projectRecord.atmosphere
        voiceStyleState.value = projectRecord.voiceStyle
        natureBlendState.value = projectRecord.natureBlend.toFloat()
        fxBlendState.value = projectRecord.fxBlend.toFloat()

        viewModelScope.launch {
            try {
                val listType = Types.newParameterizedType(List::class.java, SceneBlueprint::class.java)
                val adapter = RetrofitClient.moshiInstance.adapter<List<SceneBlueprint>>(listType)
                val scenes = withContext(Dispatchers.Default) {
                    adapter.fromJson(projectRecord.blueprintJson)
                } ?: emptyList()

                val path = projectRecord.audioFilePath
                if (path != null && File(path).exists()) {
                    // Files still exist locally, show ready state directly!
                    val outputDir = File(getApplication<Application>().cacheDir, "atmosforge_exports")
                    val parent = File(path).parentFile
                    val pureWav = File(parent, "atmosforge_soundtrack_pure.wav")
                    val files = mapOf(
                        "full" to File(path),
                        "pure" to if (pureWav.exists()) pureWav else File(path),
                        "vocal" to File(outputDir, "layer_vocals.wav"),
                        "nature" to File(outputDir, "layer_nature.wav"),
                        "fx" to File(outputDir, "layer_vfx.wav"),
                        "zip" to File(outputDir, "atmosforge_all_tracks.zip")
                    )
                    _uiState.value = GenerationUiState.Ready(scenes, files)
                    initPlayer(path)
                } else {
                    // Audio deleted or missing, but blueprint can be re-rendered! Show blueprint state
                    _uiState.value = GenerationUiState.BlueprintReady(scenes)
                }
            } catch (e: Exception) {
                _uiState.value = GenerationUiState.Idle
            }
        }
    }

    fun deleteProject(projectRecord: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(projectRecord)
            // Delete associated file
            projectRecord.audioFilePath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // ===================================
    // MEDIA PLAYER PLAYBACK CONTROL SECTION
    // ===================================

    fun setPlaybackOption(option: String) {
        if (playbackOptionState.value == option) return
        playbackOptionState.value = option
        
        // Stop playback and re-initialize player with the selected file
        val state = _uiState.value
        if (state is GenerationUiState.Ready) {
            val fileKey = if (option == "pure") "pure" else "full"
            val file = state.files[fileKey]
            if (file != null && file.exists()) {
                initPlayer(file.absolutePath)
            }
        }
    }

    private fun initPlayer(filePath: String) {
        stopPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playProgress.value = 1f
                    stopPlaybackTracker()
                }
            }
            _playDurationMs.value = mediaPlayer?.duration ?: 0
            _playCurrentPositionMs.value = 0
            _playProgress.value = 0f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayback() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopPlaybackTracker()
        } else {
            player.start()
            _isPlaying.value = true
            startPlaybackTracker()
        }
    }

    fun seekToPosition(progress: Float) {
        if (progress.isNaN() || progress.isInfinite()) return
        val player = mediaPlayer ?: return
        val duration = _playDurationMs.value
        val pos = if (duration > 0) {
            (progress * duration).toInt().coerceIn(0, duration)
        } else {
            0
        }
        player.seekTo(pos)
        _playCurrentPositionMs.value = pos
        _playProgress.value = progress.coerceIn(0f, 1f)
    }

    private fun startPlaybackTracker() {
        playbackTrackerJob?.cancel()
        playbackTrackerJob = viewModelScope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { player ->
                    val pos = player.currentPosition
                    val dur = _playDurationMs.value
                    _playCurrentPositionMs.value = pos
                    if (dur > 0) {
                        _playProgress.value = pos.toFloat() / dur
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopPlaybackTracker() {
        playbackTrackerJob?.cancel()
        playbackTrackerJob = null
    }

    private fun stopPlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) player.stop()
            player.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _playProgress.value = 0f
        _playCurrentPositionMs.value = 0
        stopPlaybackTracker()
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
