package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenConfig(
    @Json(name = "temperature") val temperature: Float? = 0.3f,
    @Json(name = "topP") val topP: Float? = 0.95f,
    @Json(name = "topK") val topK: Int? = 40,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 500
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    suspend fun generateScreeningResponse(
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>> // role, text
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val geminiContents = conversationHistory.map { (role, text) ->
            GeminiContent(
                parts = listOf(GeminiPart(text = text)),
                role = if (role == "AI") "model" else "user"
            )
        }

        val systemContent = GeminiContent(
            parts = listOf(GeminiPart(text = systemPrompt))
        )

        val geminiRequest = GeminiRequest(
            contents = geminiContents,
            systemInstruction = systemContent,
            generationConfig = GeminiGenConfig(temperature = 0.2f, maxOutputTokens = 150)
        )

        val jsonBody = requestAdapter.toJson(geminiRequest)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                return@withContext null
            }
            val parsed = responseAdapter.fromJson(bodyString)
            parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
        } catch (e: Exception) {
            null
        }
    }
}
