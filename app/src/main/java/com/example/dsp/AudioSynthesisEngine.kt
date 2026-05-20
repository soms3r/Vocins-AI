package com.example.dsp

import android.content.Context
import com.example.api.SceneBlueprint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin
import kotlin.math.exp
import kotlin.random.Random

class AudioSynthesisEngine(private val context: Context) {

    private val PI_F = 3.1415926535f
    
    private fun sinF(angle: Float): Float {
        return sin(angle.toDouble()).toFloat()
    }
    
    private fun expF(power: Float): Float {
        return exp(power.toDouble()).toFloat()
    }

    // Simple Pink Noise implementation to make wind/ocean sound more warm and less harsh than white noise
    private class PinkNoiseGenerator {
        private var b0 = 0f
        private var b1 = 0f
        private var b2 = 0f
        private var b3 = 0f
        private var b4 = 0f
        private var b5 = 0f
        private var b6 = 0f

        fun nextValue(): Float {
            val white = Random.nextFloat() * 2f - 1f
            b0 = 0.99886f * b0 + white * 0.0555179f
            b1 = 0.99332f * b1 + white * 0.0750759f
            b2 = 0.96900f * b2 + white * 0.1538520f
            b3 = 0.86650f * b3 + white * 0.3104856f
            b4 = 0.55000f * b4 + white * 0.5329522f
            b5 = -0.7616f * b5 - white * 0.0168980f
            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f
            b6 = white * 0.115926f
            return pink * 0.11f // Scale to avoid clipping
        }
    }

    /**
     * Synthesizes individual layers and mixes them into final WAV audio files.
     * Returns a Map containing files: full, vocal, nature, fx, and a path for the ZIP.
     */
    fun synthesizeSoundscape(
        blueprints: List<SceneBlueprint>,
        sampleRate: Int, // 44100 or 48000
        voiceStyle: String,
        natureBlend: Float, // 0 to 1f
        fxBlend: Float,     // 0 to 1f
        intensity: String,  // "Low", "Medium", "High"
        atmosphere: String,  // "Natural", "Spiritual", "Cinematic", "Dark", "Documentary"
        uploadedAudioPath: String? = null,
        uploadedMixVolume: Float = 0.8f,
        enableTextVfx: Boolean = true
    ): Map<String, File> {

        // Decode uploaded audio track if present
        var uploadedFloatingBuffer: FloatArray? = null
        if (uploadedAudioPath != null) {
            uploadedFloatingBuffer = decodeAudioToFloatArray(uploadedAudioPath, sampleRate)
        }

        // 1. Calculate durations and boundaries
        val blueprintDuration = calculateDuration(blueprints)
        val uploadedDuration = if (uploadedFloatingBuffer != null) {
            uploadedFloatingBuffer.size.toFloat() / sampleRate
        } else {
            0f
        }
        val totalDurationSeconds = if (uploadedDuration > 0f) uploadedDuration else blueprintDuration
        val numSamples = (sampleRate * totalDurationSeconds).toInt()

        // Float arrays to store individual layer data for isolated track export
        val mixedBuffer = FloatArray(numSamples)
        val pureMixedBuffer = FloatArray(numSamples)
        val vocalBuffer = FloatArray(numSamples)
        val natureBuffer = FloatArray(numSamples)
        val fxBuffer = FloatArray(numSamples)

        // Generators
        val pinkNoise = PinkNoiseGenerator()
        val random = Random(42)

        // Setup delay line for deep cinematic reverb/echo wash (450ms delay)
        val delaySamples = (sampleRate * 0.45f).toInt()
        val delayBufferVocal = FloatArray(delaySamples)
        var delayIdxVocal = 0

        val delayBufferFx = FloatArray(delaySamples)
        var delayIdxFx = 0

        // Determine coefficients based on parameters
        val intensityCoeff = when (intensity) {
            "Low" -> 0.6f
            "Medium" -> 1.0f
            "High" -> 1.4f
            else -> 1.0f
        }

        // Loop through each scene and calculate its samples
        var currentSampleMarker = 0
        for (scene in blueprints) {
            val sceneStartSec = parseTimeToSeconds(scene.startTime)
            var sceneEndSec = parseTimeToSeconds(scene.endTime)
            // If it's the last scene and our soundstage is longer, extend it!
            if (scene == blueprints.last() && totalDurationSeconds > blueprintDuration) {
                sceneEndSec = totalDurationSeconds
            }
            val sceneDurSec = (sceneEndSec - sceneStartSec).coerceAtLeast(0.1f)
            val sceneSamplesCount = (sampleRate * sceneDurSec).toInt()

            val endSampleMarker = (currentSampleMarker + sceneSamplesCount).coerceAtMost(numSamples)

            // Setup scene specific variables
            val emotion = scene.emotion.lowercase()
            
            // Generate vocal parameters
            val vocalBaseFreq = when (voiceStyle) {
                "Male Humming" -> 110f // A2 note
                "Female Humming" -> 220f // A3 note
                "Mixed Choir" -> 220f
                else -> 150f
            }

            // Generate nature parameters
            val rainDensity = if (scene.natureLayer.contains("rain", ignoreCase = true)) 0.0015f else 0.0003f
            
            // Loop through samples in this scene segment
            for (i in currentSampleMarker until endSampleMarker) {
                val relSampleIdx = i - currentSampleMarker
                val relTimeSec = relSampleIdx.toFloat() / sampleRate

                // Envelope to prevent pops at scene transistions
                val fadeInLength = (sampleRate * 0.5f).toInt() // 500ms fade in
                val fadeOutLength = (sampleRate * 0.5f).toInt() // 500ms fade out
                var env = 1.0f
                if (relSampleIdx < fadeInLength) {
                    env = relSampleIdx.toFloat() / fadeInLength
                } else if (relSampleIdx > sceneSamplesCount - fadeOutLength) {
                    env = (sceneSamplesCount - relSampleIdx).toFloat() / fadeOutLength
                }
                env = env.coerceIn(0f, 1f)

                // ==========================
                // 1. VOCAL LAYER SYNTHESIS
                // ==========================
                var vocalRaw = 0f
                when (voiceStyle) {
                    "Male Humming", "Female Humming" -> {
                        // Multi-carrier oscillator with slight detune for beautiful lush chorus
                        val f = vocalBaseFreq
                        val f1 = f * 1.004f
                        val f2 = f * 0.996f

                        val wave0 = sinF(2f * PI_F * f * relTimeSec)
                        val wave1 = sinF(2f * PI_F * f1 * relTimeSec)
                        val wave2 = sinF(2f * PI_F * f2 * relTimeSec)

                        // M-humming uses low-pass filtering. We can simulate it by reducing harmonics
                        val mainOsc = wave0 * 0.5f + wave1 * 0.3f + wave2 * 0.2f
                        
                        // Slowly modulate volume with breathing rate LFO (6-second cycle)
                        val breathLfo = 0.6f + 0.4f * sinF(2f * PI_F * 0.16f * relTimeSec)
                        
                        vocalRaw = mainOsc * breathLfo * env * 0.3f
                    }
                    "Mixed Choir" -> {
                        // Triad cord: Root (220Hz), Minor 3rd (261.63Hz), Perfect 5th (329.63Hz)
                        // This sounds extremely spiritual and cinematic!
                        val r = vocalBaseFreq
                        val m3 = r * 1.1892f // Minor 3rd ratio
                        val p5 = r * 1.5000f // 5th ratio

                        val note1 = sinF(2f * PI_F * r * relTimeSec)
                        val note2 = sinF(2f * PI_F * m3 * relTimeSec)
                        val note3 = sinF(2f * PI_F * p5 * relTimeSec)

                        // Add pitch vibrato via LFO (5Hz modulation)
                        val vibrato = 0.002f * sinF(2f * PI_F * 5.2f * relTimeSec)
                        val note1Detuned = sinF(2f * PI_F * r * (1.0f + vibrato) * relTimeSec)

                        val choirOsc = (note1Detuned * 0.4f + note2 * 0.3f + note3 * 0.3f)
                        val driftLfo = 0.5f + 0.5f * sinF(2f * PI_F * 0.1f * relTimeSec) // 10s slow rise/fall
                        
                        vocalRaw = choirOsc * driftLfo * env * 0.25f
                    }
                    "Breath Atmosphere" -> {
                        // Pure low-passed white/pink noise modulated by rapid soft breaths
                        val noiseVal = pinkNoise.nextValue()
                        // 4.5-second breath cycles
                        val inhaleExhale = sinF(2f * PI_F * (1f / 4.5f) * relTimeSec)
                        val breathMod = if (inhaleExhale > 0f) inhaleExhale * 0.4f else -inhaleExhale * 0.25f
                        
                        vocalRaw = noiseVal * (0.1f + breathMod) * env * 0.5f
                    }
                    "Ambient Voices" -> {
                        // Slow drifting pads: combinations of multiple high frequencies
                        val slowLfo1 = sinF(2f * PI_F * 0.05f * relTimeSec) // 20s cycle
                        val pitch = vocalBaseFreq * (1.2f + 0.1f * slowLfo1)
                        
                        val sine = sinF(2f * PI_F * pitch * relTimeSec)
                        vocalRaw = sine * 0.25f * env
                    }
                    else -> {
                        // Soft humming fallback
                        vocalRaw = sinF(2f * PI_F * vocalBaseFreq * relTimeSec) * 0.2f * env
                    }
                }

                // Vocal feedback circular delay wash (long cathedral tails)
                if (voiceStyle != "Breath Atmosphere") {
                    val delayedVScale = delayBufferVocal[delayIdxVocal]
                    val vocalCombined = vocalRaw + delayedVScale * 0.65f // High feedback tail
                    delayBufferVocal[delayIdxVocal] = vocalCombined
                    delayIdxVocal = (delayIdxVocal + 1) % delaySamples
                    vocalBuffer[i] = vocalCombined * 0.8f
                } else {
                    vocalBuffer[i] = vocalRaw
                }

                // ==========================
                // 2. NATURE LAYER SYNTHESIS
                // ==========================
                var natureRaw = 0f
                if (scene.natureLayer.contains("rain", ignoreCase = true)) {
                    // Rain background: pink noise + pitter patter spikes
                    var rainPatter = 0f
                    if (random.nextFloat() < rainDensity) {
                        rainPatter = (random.nextFloat() * 2f - 1f) * 0.7f // loud tap
                    }
                    natureRaw = (pinkNoise.nextValue() * 0.4f + rainPatter * 0.4f) * env
                } 
                else if (scene.natureLayer.contains("ocean", ignoreCase = true) || scene.natureLayer.contains("waves", ignoreCase = true)) {
                    // Ocean waves: Slow 9-second swells
                    val waveSwell = 0.5f + 0.5f * sinF(2f * PI_F * (1f / 9f) * relTimeSec)
                    natureRaw = pinkNoise.nextValue() * waveSwell * env * 0.7f
                } 
                else if (scene.natureLayer.contains("wind", ignoreCase = true) || scene.natureLayer.contains("breeze", ignoreCase = true)) {
                    // Wind gusts: Pink noise modulated by high speed pitch/volume sweeps (LFOs)
                    val windLfo = 0.4f + 0.4f * sinF(2f * PI_F * 0.08f * relTimeSec) + 0.2f * sinF(2f * PI_F * 0.3f * relTimeSec)
                    natureRaw = pinkNoise.nextValue() * windLfo * env * 0.6f
                } 
                else if (scene.natureLayer.contains("birds", ignoreCase = true) || scene.natureLayer.contains("forest", ignoreCase = true)) {
                    // Soft forest wind with occasional high pitch chirps
                    val windVal = pinkNoise.nextValue() * 0.25f
                    
                    // Periodic birds chirping: every 6 seconds, generate a 150ms pitch sweep chirp
                    val chirpPeriod = sampleRate * 6
                    val relativeChirpPos = i % chirpPeriod
                    var chirpSample = 0f
                    if (relativeChirpPos < (sampleRate * 0.2f)) {
                        // bird chirp active
                        val chirpT = relativeChirpPos.toFloat() / (sampleRate * 0.2f)
                        // Frequency sweeps rapidly upwards from 2500Hz to 3400Hz
                        val chirpFreq = 2500f + 900f * chirpT
                        val freqAccum = chirpFreq * (relativeChirpPos.toFloat() / sampleRate)
                        chirpSample = sinF(2f * PI_F * freqAccum) * (1f - chirpT) * 0.15f
                    }
                    natureRaw = (windVal + chirpSample) * env
                }
                else {
                    // Fallback to beautiful default cave/breeze room tone
                    val roomToneLfo = 0.5f + 0.3f * sinF(2f * PI_F * 0.15f * relTimeSec)
                    natureRaw = pinkNoise.nextValue() * roomToneLfo * 0.3f * env
                }

                natureBuffer[i] = natureRaw

                // ==========================
                // 3. CINEMATIC FX & NARRATIVE VFX LAYER SYNTHESIS
                // ==========================
                var fxRaw = 0f

                // Contextual Narrative VFX Detection (Footsteps, Creaks) triggered on demand or if present
                val hasFootsteps = enableTextVfx && (
                    scene.fxLayer.contains("footstep", ignoreCase = true) || 
                    scene.natureLayer.contains("footstep", ignoreCase = true) ||
                    scene.vocalLayer.contains("footstep", ignoreCase = true) ||
                    scene.fxLayer.contains("walk", ignoreCase = true) ||
                    scene.natureLayer.contains("road", ignoreCase = true)
                )

                val hasCreak = enableTextVfx && (
                    scene.fxLayer.contains("creak", ignoreCase = true) || 
                    scene.fxLayer.contains("door", ignoreCase = true)
                )

                if (hasFootsteps) {
                    // Footstep Generator on road/concrete: periodic impacts at 1.8Hz (approx step interval of 0.55s)
                    val stepPeriod = (sampleRate * 0.55f).toInt()
                    val relStepPos = relSampleIdx % stepPeriod
                    val stepLength = (sampleRate * 0.12f).toInt() // 120ms decaying impact

                    if (relStepPos < stepLength) {
                        val stepT = relStepPos.toFloat() / stepLength
                        val stepDecay = expF(-6.5f * stepT)
                        
                        // Mixed high-passed pink noise for shoe leather friction with deep low frequency thud.
                        val shoeFriction = (pinkNoise.nextValue() * (0.5f - 0.3f * stepT))
                        val pavementImpact = sinF(2f * PI_F * 85f * (relStepPos.toFloat() / sampleRate)) * 0.28f
                        
                        fxRaw += (shoeFriction + pavementImpact) * stepDecay * 0.45f * env
                    }
                }

                if (hasCreak) {
                    // Creaking door swing: dry friction clicks built and slowly decaying over first 2 seconds of the segment
                    if (relTimeSec < 2.0f) {
                        val creakDur = 1.4f
                        val ct = relTimeSec / creakDur
                        if (ct < 1.0f) {
                            val clickFreq = 16f + 22f * (1f - ct) // slowly decelerating friction clicks
                            val clickPeriod = (sampleRate / clickFreq).toInt().coerceAtLeast(10)
                            if (relSampleIdx % clickPeriod == 0) {
                                fxRaw += (random.nextFloat() * 2f - 1f) * 0.2f * (1f - ct) * env
                            }
                        }
                    }
                }

                // If specialized procedural effects don't fully override the track, integrate background cinematic elements too:
                if (scene.fxLayer.contains("whoosh", ignoreCase = true) || scene.fxLayer.contains("sweep", ignoreCase = true)) {
                    // Whoosh / sweep: Noise volume swells over 3 seconds and falls
                    val midPoint = sceneSamplesCount / 3
                    val whooshEnv = if (relSampleIdx < midPoint) {
                        relSampleIdx.toFloat() / midPoint
                    } else {
                        (sceneSamplesCount - relSampleIdx).toFloat() / (sceneSamplesCount - midPoint)
                    }
                    val sweeps = whooshEnv.cleanClamp()
                    // Sweep noise with sliding band filters (simulated by pitch-swept sine + pink noise mix)
                    val sweptSine = sinF(2f * PI_F * (120f + 1200f * sweeps) * relTimeSec) * 0.15f
                    fxRaw += (pinkNoise.nextValue() * 0.3f + sweptSine) * sweeps * env
                }
                else if (scene.fxLayer.contains("impact", ignoreCase = true) || emotion.contains("climax")) {
                    // Cinema Impact booming hit: Massive sub-bass drop sweeping from 130Hz down to 25Hz
                    // Decays exponentially over 2.5 seconds
                    val impactElapsed = relSampleIdx.toFloat() / sampleRate
                    
                    if (impactElapsed < 2.5f) {
                        val decay = expF(-2.2f * impactElapsed)
                        // Exploding sub-sine pitch Sweep
                        val currentFreq = 30f + 100f * expF(-4.5f * impactElapsed)
                        val sineSweep = sinF(2f * PI_F * currentFreq * impactElapsed)
                        
                        // Mixed with explosion noise crash
                        val crashNoise = pinkNoise.nextValue() * expF(-6.0f * impactElapsed) * 0.2f
                        
                        fxRaw += (sineSweep * 0.6f + crashNoise) * decay * env
                    }
                }
                else if (scene.fxLayer.contains("rise", ignoreCase = true) || scene.fxLayer.contains("tension", ignoreCase = true)) {
                    // Tension rise: Sine pitch exponentially scaling upwards
                    val ratio = relSampleIdx.toFloat() / sceneSamplesCount
                    val risingFreq = 50f + 300f * (ratio * ratio)
                    val sineRise = sinF(2f * PI_F * risingFreq * relTimeSec)
                    
                    // Modulate loudness with rapid pulsing heartbeat (approx 100bpm = 1.6Hz LFO)
                    val heartbeat = 0.5f + 0.4f * sinF(2f * PI_F * 1.6f * relTimeSec)
                    fxRaw += sineRise * ratio * heartbeat * env * 0.25f
                }
                else if (scene.fxLayer.contains("drone", ignoreCase = true) || scene.fxLayer.contains("swell", ignoreCase = true)) {
                    // Deep ominous tension drone: detuned low notes
                    // Frequencies generate pulsing binaural beat to increase cognitive suspense (A1 = 55Hz, BB1 = 58Hz)
                    val sine1 = sinF(2f * PI_F * 55f * relTimeSec)
                    val sine2 = sinF(2f * PI_F * 58.2f * relTimeSec)
                    fxRaw += (sine1 * 0.4f + sine2 * 0.4f) * env * 0.35f
                }
                else {
                    // Subtle background sub-atmosphere
                    if (!hasFootsteps && !hasCreak) {
                        fxRaw += sinF(2f * PI_F * 60f * relTimeSec) * 0.12f * env
                    }
                }

                // Add nice cavernous echo wash to FX layer
                val delayedFxScale = delayBufferFx[delayIdxFx]
                val fxCombined = fxRaw + delayedFxScale * 0.55f // moderate feedback echo
                delayBufferFx[delayIdxFx] = fxCombined
                delayIdxFx = (delayIdxFx + 1) % delaySamples
                fxBuffer[i] = fxCombined * 0.7f
            }

            currentSampleMarker = endSampleMarker
        }

        // ==========================
        // 4. MIXING & LIMITING
        // ==========================
        for (i in 0 until numSamples) {
            val v = vocalBuffer[i] * intensityCoeff
            val n = natureBuffer[i] * natureBlend * intensityCoeff
            val f = fxBuffer[i] * fxBlend * intensityCoeff

            var uploadSample = 0f
            if (uploadedFloatingBuffer != null && i < uploadedFloatingBuffer.size) {
                uploadSample = uploadedFloatingBuffer[i] * uploadedMixVolume
            }

            // Compute master mixed float stream
            val rawMixed = v + n + f + uploadSample
            val rawPureMixed = v + n + f

            // Fast Peak compressor / limiter (soft saturation: x / (1 + |x|))
            // This prevents hard visual and sonic clipping instantly!
            val limitedMixed = rawMixed / (1.0f + kotlin.math.abs(rawMixed) * 0.4f)
            mixedBuffer[i] = limitedMixed.coerceIn(-1.0f, 1.0f)

            val limitedPureMixed = rawPureMixed / (1.0f + kotlin.math.abs(rawPureMixed) * 0.4f)
            pureMixedBuffer[i] = limitedPureMixed.coerceIn(-1.0f, 1.0f)
        }

        // Create outputs
        val outputDir = File(context.cacheDir, "atmosforge_exports")
        if (!outputDir.exists()) outputDir.mkdirs()

        // Write files
        val fullWav = File(outputDir, "atmosforge_soundtrack_blended.wav")
        val pureWav = File(outputDir, "atmosforge_soundtrack_pure.wav")
        val vocalWav = File(outputDir, "layer_vocals.wav")
        val natureWav = File(outputDir, "layer_nature.wav")
        val fxWav = File(outputDir, "layer_vfx.wav")

        writeWavFile(fullWav, mixedBuffer, sampleRate)
        writeWavFile(pureWav, pureMixedBuffer, sampleRate)
        writeWavFile(vocalWav, vocalBuffer, sampleRate)
        writeWavFile(natureWav, natureBuffer, sampleRate)
        writeWavFile(fxWav, fxBuffer, sampleRate)

        // Zip files
        val zipFile = File(outputDir, "atmosforge_all_tracks.zip")
        val fileMap = mapOf(
            "soundtrack_blended" to fullWav,
            "soundtrack_pure" to pureWav,
            "vocal" to vocalWav,
            "nature" to natureWav,
            "vfx" to fxWav
        )
        zipFiles(zipFile, fileMap)

        return mapOf(
            "full" to fullWav,
            "pure" to pureWav,
            "vocal" to vocalWav,
            "nature" to natureWav,
            "fx" to fxWav,
            "zip" to zipFile
        )
    }

    private fun Float.cleanClamp(): Float {
        return this.coerceIn(0f, 1f)
    }

    // Helper to extract duration from scene blueprint list
    private fun calculateDuration(blueprints: List<SceneBlueprint>): Float {
        if (blueprints.isEmpty()) return 20f
        val lastScene = blueprints.last()
        return parseTimeToSeconds(lastScene.endTime)
    }

    // Convert "MM:SS" back to float seconds (e.g., "01:20" -> 80.0)
    private fun parseTimeToSeconds(timeStr: String): Float {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 15f // Fallback
        val minutes = parts[0].toFloatOrNull() ?: 0f
        val seconds = parts[1].toFloatOrNull() ?: 15f
        return (minutes * 60f) + seconds
    }

    /**
     * Standard RIFF WAVE file writer for 16-bit PCM Linear Mono.
     */
    private fun writeWavFile(file: File, buffer: FloatArray, sampleRate: Int) {
        try {
            FileOutputStream(file).use { out ->
                val byteCount = buffer.size * 2 // 16-bit PCM = 2 bytes per sample
                val totalHeaderSize = 44
                val totalFileSize = byteCount + totalHeaderSize - 8

                // Build 44-byte WAV header stream
                val header = ByteBuffer.allocate(totalHeaderSize)
                header.order(ByteOrder.LITTLE_ENDIAN)

                header.put("RIFF".toByteArray()) // ChunkID
                header.putInt(totalFileSize)     // ChunkSize
                header.put("WAVE".toByteArray()) // Format

                header.put("fmt ".toByteArray()) // Subchunk1ID
                header.putInt(16)                // Subchunk1Size (16 for PCM)
                header.putShort(1.toShort())     // AudioFormat (1 = PCM)
                header.putShort(1.toShort())     // NumChannels (1 = Mono)
                header.putInt(sampleRate)        // SampleRate
                header.putInt(sampleRate * 2)    // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
                header.putShort(2.toShort())     // BlockAlign (NumChannels * BitsPerSample/8)
                header.putShort(16.toShort())    // BitsPerSample (16 bits)

                header.put("data".toByteArray()) // Subchunk2ID
                header.putInt(byteCount)         // Subchunk2Size

                out.write(header.array())

                // Write 16-bit standard short PCM audio samples
                val pcmBuffer = ByteBuffer.allocate(65536) // 64K write buffer
                pcmBuffer.order(ByteOrder.LITTLE_ENDIAN)

                for (sample in buffer) {
                    if (!pcmBuffer.hasRemaining()) {
                        out.write(pcmBuffer.array(), 0, pcmBuffer.position())
                        pcmBuffer.clear()
                    }
                    // Scale float (-1.0 to +1.0) to standard signed Short range (-32768 to +32767)
                    val shortVal = (sample * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                    pcmBuffer.putShort(shortVal)
                }

                if (pcmBuffer.position() > 0) {
                    out.write(pcmBuffer.array(), 0, pcmBuffer.position())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Compiles multiple files into a single ZIP archive for robust downloading.
     */
    private fun zipFiles(zipFile: File, files: Map<String, File>) {
        try {
            java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                for ((key, file) in files) {
                    if (!file.exists()) continue
                    val entryName = when (key) {
                        "soundtrack" -> "Vocins_Master_Soundtrack.wav"
                        "vocal" -> "Isolated_Vocal_Atmosphere_Layer.wav"
                        "nature" -> "Isolated_Nature_Bed_Layer.wav"
                        "vfx" -> "Isolated_Cinematic_VFX_Layer.wav"
                        else -> file.name
                    }
                    zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Decodes any Android-compatible media/audio file (MP3, WAV, AAC, M4A) directly into mono Float PCM space
     * using hardware-accelerated MediaCodec & MediaExtractor API.
     */
    private fun decodeAudioToFloatArray(filePath: String, targetSampleRate: Int): FloatArray? {
        val extractor = android.media.MediaExtractor()
        var codec: android.media.MediaCodec? = null
        try {
            extractor.setDataSource(filePath)
            val trackCount = extractor.trackCount
            var audioTrackIdx = -1
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = i
                    break
                }
            }
            if (audioTrackIdx == -1) return null

            extractor.selectTrack(audioTrackIdx)
            val format = extractor.getTrackFormat(audioTrackIdx)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: return null

            val codecInstance = android.media.MediaCodec.createDecoderByType(mime)
            codec = codecInstance
            codecInstance.configure(format, null, null, 0)
            codecInstance.start()

            val info = android.media.MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false

            val shortList = mutableListOf<Short>()

            while (!isOutputEOS) {
                if (!isInputEOS) {
                    val inputBufferIdx = codecInstance.dequeueInputBuffer(12000)
                    if (inputBufferIdx >= 0) {
                        val inputBuffer = codecInstance.getInputBuffer(inputBufferIdx) ?: break
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codecInstance.queueInputBuffer(inputBufferIdx, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isInputEOS = true
                        } else {
                            codecInstance.queueInputBuffer(inputBufferIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufferIdx = codecInstance.dequeueOutputBuffer(info, 12000)
                if (outputBufferIdx >= 0) {
                    val outputBuffer = codecInstance.getOutputBuffer(outputBufferIdx)
                    if (outputBuffer != null && info.size > 0) {
                        outputBuffer.position(info.offset)
                        val shortBuf = outputBuffer.asShortBuffer()
                        val shorts = ShortArray(info.size / 2)
                        shortBuf.get(shorts)
                        for (s in shorts) {
                            shortList.add(s)
                        }
                    }
                    codecInstance.releaseOutputBuffer(outputBufferIdx, false)
                    if ((info.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true
                    }
                } else if (outputBufferIdx == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Ignored format change transitions
                }
            }

            val inputSampleRate = format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
            val inputChannels = format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)

            val shorts = shortList.toShortArray()
            if (shorts.isEmpty()) return null

            val monoShorts = if (inputChannels > 1) {
                val size = shorts.size / inputChannels
                ShortArray(size) { j ->
                    var sum = 0
                    for (c in 0 until inputChannels) {
                        sum += shorts[j * inputChannels + c]
                    }
                    (sum / inputChannels).toShort()
                }
            } else {
                shorts
            }

            if (inputSampleRate == targetSampleRate) {
                return FloatArray(monoShorts.size) { monoShorts[it].toFloat() / 32768f }
            } else {
                val ratio = inputSampleRate.toDouble() / targetSampleRate
                val targetSize = (monoShorts.size / ratio).toInt()
                val outFloats = FloatArray(targetSize)
                for (j in 0 until targetSize) {
                    val sourceIdx = (j * ratio).toInt().coerceIn(0, monoShorts.lastIndex)
                    outFloats[j] = monoShorts[sourceIdx].toFloat() / 32768f
                }
                return outFloats
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: java.lang.Exception) {}
            try {
                extractor.release()
            } catch (e: java.lang.Exception) {}
        }
    }
}
