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

object OpenAiAnalysisService : LlmAnalysisService() {
    private const val TAG = "OpenAiAnalysisService"
    private const val MODEL_NAME = "gpt-4o"
    private const val API_URL = "https://api.openai.com/v1/chat/completions"
    private const val MAX_TOKENS = 8000

    override val providerName = "OpenAI"

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
        val apiKey = resolveApiKey(customApiKey, "OPENAI_API_KEY")

        val requestJson = JSONObject().apply {
            put("model", MODEL_NAME)
            put("max_tokens", MAX_TOKENS)
            put("temperature", 0.7)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val body = try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "OpenAI API ${response.code}: $raw")
                    throw AnalysisErrors.http(providerName, response.code)
                }
                raw
            }
        } catch (e: IOException) {
            throw AnalysisErrors.network(e)
        }

        val choice = JSONObject(body).optJSONArray("choices")?.optJSONObject(0)
            ?: throw AnalysisErrors.unreadable()

        if (choice.optString("finish_reason") == "length") throw AnalysisErrors.truncated()

        val text = choice.optJSONObject("message")?.optString("content")
        if (text.isNullOrBlank()) {
            Log.e(TAG, "No content in OpenAI response: $body")
            throw AnalysisErrors.unreadable()
        }
        return text
    }
}
