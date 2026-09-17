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

object ClaudeAnalysisService : LlmAnalysisService() {
    private const val TAG = "ClaudeAnalysisService"
    private const val MODEL_NAME = "claude-opus-5"
    private const val API_URL = "https://api.anthropic.com/v1/messages"
    private const val ANTHROPIC_VERSION = "2023-06-01"

    /**
     * Claude Opus 5 runs adaptive thinking by default and those thinking tokens count
     * against max_tokens. The previous 4000 was spent before the JSON finished, so every
     * reply arrived truncated and unparseable, which silently fell back to canned text.
     */
    private const val MAX_TOKENS = 16000

    override val providerName = "Claude"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun complete(
        systemPrompt: String,
        userPrompt: String,
        customApiKey: String?
    ): String {
        val apiKey = resolveApiKey(customApiKey, "ANTHROPIC_API_KEY")

        val requestJson = JSONObject().apply {
            put("model", MODEL_NAME)
            put("max_tokens", MAX_TOKENS)
            put("system", systemPrompt)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            }))
        }

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("content-type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val body = try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Claude API ${response.code}: $raw")
                    throw AnalysisErrors.http(providerName, response.code)
                }
                raw
            }
        } catch (e: IOException) {
            throw AnalysisErrors.network(e)
        }

        val root = JSONObject(body)

        // A refusal and a truncation both arrive as HTTP 200. Check before trusting content.
        when (root.optString("stop_reason")) {
            "refusal" -> throw AnalysisErrors.refused()
            "max_tokens" -> throw AnalysisErrors.truncated()
        }

        // Skip any thinking blocks; the JSON lives in the text block.
        val content = root.optJSONArray("content") ?: throw AnalysisErrors.unreadable()
        for (i in 0 until content.length()) {
            val block = content.optJSONObject(i) ?: continue
            if (block.optString("type") == "text") {
                val text = block.optString("text")
                if (text.isNotBlank()) return text
            }
        }

        Log.e(TAG, "No text block in Claude response: $body")
        throw AnalysisErrors.unreadable()
    }
}
