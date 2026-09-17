package com.example.network

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiAnalysisService : LlmAnalysisService() {
    private const val TAG = "GeminiAnalysisService"
    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val MAX_TOKENS = 16000

    override val providerName = "Gemini"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun complete(
        systemPrompt: String,
        userPrompt: String,
        customApiKey: String?
    ): String {
        val apiKey = resolveApiKey(customApiKey, "GEMINI_API_KEY")

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            }))
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
                put("maxOutputTokens", MAX_TOKENS)
            })
        }

        val request = Request.Builder()
            // The key goes in a header, not the query string, so it stays out of logs.
            .url("$BASE_URL/$MODEL_NAME:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val body = try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API ${response.code}: $raw")
                    throw AnalysisErrors.http(providerName, response.code)
                }
                raw
            }
        } catch (e: IOException) {
            throw AnalysisErrors.network(e)
        }

        val candidate = JSONObject(body).optJSONArray("candidates")?.optJSONObject(0)
            ?: throw AnalysisErrors.unreadable()

        when (candidate.optString("finishReason")) {
            "MAX_TOKENS" -> throw AnalysisErrors.truncated()
            "SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST" -> throw AnalysisErrors.refused()
        }

        val text = candidate.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")

        if (text.isNullOrBlank()) {
            Log.e(TAG, "No text in Gemini response: $body")
            throw AnalysisErrors.unreadable()
        }
        return text
    }
}
