package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// Scene model corresponding to the user request.
@JsonClass(generateAdapter = true)
data class SceneBlueprint(
    @Json(name = "scene_number") val sceneNumber: Int,
    @Json(name = "start_time") val startTime: String, // e.g. "00:00"
    @Json(name = "end_time") val endTime: String,     // e.g. "00:20"
    val emotion: String,                             // e.g. "hopeful"
    val energy: String,                              // e.g. "low"
    @Json(name = "vocal_layer") val vocalLayer: String, // Vocal design
    @Json(name = "nature_layer") val natureLayer: String, // Nature design
    @Json(name = "fx_layer") val fxLayer: String      // Cinematic FX design
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val moshiInstance: Moshi get() = moshi
}

class GeminiPromptBuilder {

    fun buildSystemInstruction(
        intensity: String,
        atmosphere: String,
        voiceStyle: String
    ): String {
        return """
            You are the Vocins AI Sound Architect. 
            Your role is to analyze narrative content and create a highly detailed, scene-by-scene audio blueprint.
            
            CREATIVE MANDATES:
            1. STRICTLY AVOID traditional musical instruments (no piano, guitar, violin, drums, synths, orchestra, brass, strings, percussion).
            2. For audio layers, you MUST ONLY specify elements from these approved lists:
               - Human Vocal Elements: Humming, Vocal pads, Vocal ambience, Choir-like textures, Breath sounds, Layered vocal atmospheres, Ethereal vocal tones, Vocal drones, Wordless voices.
               - Nature Elements: Wind, Rain, Water streams, Ocean waves, Forest ambience, Leaves movement, Birds, Thunder, Desert winds, Cave echoes, Fire ambience.
               - Environmental Sound Design: City ambience, Crowd ambience, Market ambience, Footsteps, Doors, Vehicles, Fabric movement, Mechanical ambience, Room tones, Atmospheric transitions.
               - Cinematic FX: Whooshes, Risers, Impacts, Swells, Reverse textures, Tension builds, Atmospheric drones, Transition effects.
            
            SYSTEM OVERRIDES:
            - User's Requested Intensity: $intensity
            - Base Atmosphere: $atmosphere
            - Active Voice Style: $voiceStyle
            
            You must output your audio blueprint in a STRICT valid raw JSON array format, containing NO markdown syntax, NO wrapper text, and NO trailing commas.
            The JSON array must be an array of objects matching this scheme exactly:
            {
              "scene_number": 1,
              "start_time": "00:00",
              "end_time": "00:20",
              "emotion": "hopeful",
              "energy": "low",
              "vocal_layer": "soft humming",
              "nature_layer": "forest breeze",
              "fx_layer": "gentle transition sweep"
            }
        """.trimIndent()
    }

    fun buildAnalysisPrompt(
        inputType: String,
        inputText: String,
        durationMode: String,
        customDurationSeconds: Int,
        uploadedAudioName: String? = null
    ): String {
        val durationDesc = if (durationMode == "Auto") {
            "Determine an natural dramatic duration based on standard reading speed of the text (approx 15-20 seconds per 50 words, maximum 90 seconds total)."
        } else {
            "Constrain the total soundscape duration to exactly $customDurationSeconds seconds."
        }
        
        val uploadContext = if (uploadedAudioName != null) {
            "CRITICAL: The user has uploaded a custom audio narration/poem recitation recording named '$uploadedAudioName'. The generated atmospheres, nature backgrounds, and cinematic FX layers must be designed to perfectly overlay and synchronize with this recitation track, maintaining elegant contrast so the vocals are clearly audible."
        } else {
            ""
        }
        
        return """
            Perform a unified 5-stage audio analysis and blueprint structure on the following user $inputType.
            
            $uploadContext
            
            ANALYSIS SUB-STAGES:
            - Stage 1: Analyze overall Mood, Emotion, Energy Level, Tension, Narrative Arc, and Environments.
            - Stage 2: Automatically identify logical scene segments and set start_time / end_time stamps in "MM:SS" format. $durationDesc.
            - Stage 3: Classify local emotions and scene energy level ("low", "medium", "high").
            - Stage 4: Generate specific cinematic sound layers (vocal_layer, nature_layer, fx_layer) avoiding traditional instruments.
            - Stage 5: Compile into a final audio design soundtrack specification.
            
            Format the output strictly as a JSON array of scene objects. Do not explain anything. Start directly with [ and end with ].
            
            USER INPUT [$inputType]:
            $inputText
        """.trimIndent()
    }
}
