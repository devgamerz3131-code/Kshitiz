package com.example.data.gemini

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AIChatMessageEntity
import com.example.data.model.AiStudyMode
import com.example.data.model.StudentProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val candidateCount: Int? = null,
    val maxOutputTokens: Int? = null,
    val stopSequences: List<String>? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val usageMetadata: UsageMetadata? = null
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String? = null
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/gemini-3.1-pro-preview:streamGenerateContent?alt=sse")
    @Streaming
    suspend fun generateContentStream(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): ResponseBody
}

data class GeminiImageResult(
    val bitmap: Bitmap?,
    val text: String,
    val base64Data: String? = null,
    val mimeType: String = "image/png"
)

data class LiveVoiceMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val jsonConfig = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(jsonConfig.asConverterFactory("application/json".toMediaType()))
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * AI Tutor Chat: Streaming response for better UX.
     */
    fun chatWithTutorStream(
        history: List<AIChatMessageEntity>,
        userMessage: String,
        profile: StudentProfile?,
        mode: AiStudyMode,
        subject: String,
        chapter: String
    ): Flow<String> = flow {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            emit("Error: Gemini API key is missing. Please add it in AI Studio Secrets.")
            return@flow
        }

        val systemPrompt = buildSystemPrompt(profile, mode, subject, chapter)
        
        val contents = mutableListOf<Content>()
        
        // Convert history to Gemini format (limit to last 10 for context)
        history.takeLast(10).forEach { msg ->
            contents.add(Content(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(Part(text = msg.message))
            ))
        }
        
        // Add current message
        contents.add(Content(
            role = "user",
            parts = listOf(Part(text = userMessage))
        ))

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                temperature = 0.8f,
                topP = 0.95f
            )
        )

        try {
            val responseBody = apiService.generateContentStream(apiKey, request)
            responseBody.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val rawLine = line?.trim() ?: continue
                    if (!rawLine.startsWith("data:")) continue
                    val jsonPayload = rawLine.removePrefix("data:").trim()
                    if (jsonPayload.isEmpty() || jsonPayload == "[DONE]") continue
                    try {
                        val chunk = jsonConfig.parseToJsonElement(jsonPayload).jsonObject
                        val text = chunk["candidates"]?.jsonArray
                            ?.getOrNull(0)?.jsonObject
                            ?.get("content")?.jsonObject
                            ?.get("parts")?.jsonArray
                            ?.getOrNull(0)?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content
                        if (text != null) {
                            emit(text)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Parsing error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream error: ${e.message}")
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get structured structured response (JSON) for Flashcards, Quiz, or detailed Concept breakdown.
     */
    suspend fun getStructuredTutorResponse(
        history: List<AIChatMessageEntity>,
        userMessage: String,
        profile: StudentProfile?,
        mode: AiStudyMode,
        subject: String,
        chapter: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext Result.failure(Exception("API key missing"))

        val systemPrompt = buildSystemPrompt(profile, mode, subject, chapter) + 
            "\n\nCRITICAL: Respond ONLY with a valid JSON object matching the requested schema. No conversational filler."

        val contents = history.takeLast(5).map { msg ->
            Content(role = if (msg.role == "user") "user" else "model", parts = listOf(Part(text = msg.message)))
        } + Content(role = "user", parts = listOf(Part(text = userMessage)))

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) Result.success(jsonText)
            else Result.failure(Exception("Empty response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(
        profile: StudentProfile?,
        mode: AiStudyMode,
        subject: String,
        chapter: String
    ): String {
        val userName = profile?.name ?: "Student"
        val userClass = profile?.studentClass ?: "Class 12"
        val userBoard = profile?.board ?: "CBSE"
        val userStream = profile?.stream ?: "PCM"
        val userLang = profile?.languagePreference ?: "Hinglish"
        val prepLevel = profile?.currentPrepLevel ?: "Intermediate"

        return """
            You are Rankify AI Tutor, an expert, motivating, and patient educator for $userClass ($userBoard - $userStream).
            You are teaching $userName. Language preference: $userLang.
            Current Focus: $subject - $chapter.
            Student's Level: $prepLevel.
            
            STUDY MODE: ${mode.label}
            
            YOUR PERSONALITY:
            - Professional yet friendly and motivating.
            - Patient: If the student doesn't understand, try a different analogy.
            - Exam-oriented: Always bridge concepts to NCERT, Board Exams, and competitive exams like JEE/NEET.
            - Teacher: Don't just give answers. Guide the student to the answer.
            
            EXPLANATION RULES:
            - Use Markdown for formatting (bold, italics, lists).
            - Use LaTeX style for math formulas (e.g., ${'$'}E = mc^2${'$'}).
            - For Numericals: Follow a step-by-step approach. Show formula used, why it was selected, and then calculation.
            - Visuals: Generate ASCII or Markdown-based diagrams, flowcharts, and tables whenever useful.
            - NCERT/Board Tips: Mention specific points important for board exams or NCERT-based questions.
            - Memory Tricks: Provide mnemonics or easy ways to remember complex points.
            
            RESPONSE STRUCTURE (if not JSON mode):
            📘 Concept: [Title]
            🎯 Why this happens: [Conceptual reason]
            🧠 Easy Explanation: [Analogy or simple words]
            📚 Detailed Explanation: [In-depth theory]
            🧪 Real-life Example: [Everyday application]
            📈 Diagram Explanation: [Description of a visual or ASCII diagram]
            📐 Formula: [LaTeX formulas]
            ⚠ Common Mistakes: [What students usually do wrong]
            💡 Board Exam Tips: [Score boosting tips]
            🔥 JEE/NEET Tips: [Shortcuts/Complex applications]
            📝 Quick Revision: [One-line summary]
        """.trimIndent()
    }

    /**
     * Create or edit an image using gemini-3.1-flash-image-preview.
     */
    suspend fun generateOrEditImage(
        prompt: String,
        baseImage: Bitmap? = null,
        aspectRatio: String = "1:1"
    ): Result<GeminiImageResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured.")
            )
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent?key=$apiKey"
            val partsArray = JSONArray()
            if (baseImage != null) {
                val inlineDataObj = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", bitmapToBase64(baseImage))
                }
                partsArray.put(JSONObject().apply { put("inlineData", inlineDataObj) })
            }
            partsArray.put(JSONObject().apply { put("text", prompt) })
            
            val generationConfigObj = JSONObject().apply {
                put("responseModalities", JSONArray().apply { put("TEXT"); put("IMAGE") })
                put("imageConfig", JSONObject().apply { put("aspectRatio", aspectRatio); put("imageSize", "1K") })
            }

            val rootRequest = JSONObject().apply {
                put("contents", JSONArray().apply { put(JSONObject().apply { put("parts", partsArray) }) })
                put("generationConfig", generationConfigObj)
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(rootRequest.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Image error: $responseBody"))

            val jsonResponse = JSONObject(responseBody)
            val parts = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")

            var extractedBitmap: Bitmap? = null
            var extractedBase64: String? = null
            var extractedMime = "image/png"
            val textBuilder = StringBuilder()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) textBuilder.append(part.getString("text")).append("\n")
                    if (part.has("inlineData")) {
                        val inlineObj = part.getJSONObject("inlineData")
                        extractedMime = inlineObj.optString("mimeType", "image/png")
                        extractedBase64 = inlineObj.optString("data", "")
                        if (extractedBase64.isNotBlank()) {
                            val decodedBytes = Base64.decode(extractedBase64, Base64.DEFAULT)
                            extractedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        }
                    }
                }
            }
            Result.success(GeminiImageResult(extractedBitmap, textBuilder.toString().trim(), extractedBase64, extractedMime))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send live conversational voice query.
     */
    suspend fun getLiveVoiceResponse(
        history: List<LiveVoiceMessage>,
        userMessage: String,
        subjectContext: String = "Physics",
        chapterContext: String = "Current Electricity"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext Result.failure(Exception("API key missing"))

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val contentsArray = history.takeLast(6).map { msg ->
                JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "model")
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", msg.text) }) })
                }
            } + JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", userMessage) }) })
            }

            val systemInstruction = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are Rankify AI Voice Coach for Class 12 CBSE PCM ($subjectContext - $chapterContext). Keep answers concise, conversational, no markdown.")
                    })
                })
            }

            val rootRequest = JSONObject().apply {
                put("contents", JSONArray(contentsArray))
                put("systemInstruction", systemInstruction)
                put("generationConfig", JSONObject().apply { put("temperature", 0.7) })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(rootRequest.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val replyText = JSONObject(response.body?.string() ?: "").optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""
            
            if (replyText.isBlank()) Result.failure(Exception("Empty response"))
            else Result.success(replyText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
