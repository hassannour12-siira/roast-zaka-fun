package com.example.network

import com.example.model.FullAnalysisResult
import com.example.model.JobAdAnalysisResult
import com.example.model.RoastIntensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AnalysisService {
    /** Roast and rescue a candidate's CV or profile. */
    suspend fun analyzeProfile(
        cvText: String,
        targetRole: String,
        intensity: RoastIntensity,
        customApiKey: String? = null
    ): FullAnalysisResult

    /** Roast and rescue a recruiter's job advert. */
    suspend fun analyzeJobAd(
        jobAdText: String,
        intensity: RoastIntensity,
        customApiKey: String? = null
    ): JobAdAnalysisResult
}

/**
 * Everything the three providers share.
 *
 * Each provider only has to say how to turn a system prompt plus a user prompt into a
 * string of text. Both analyses are then built on top of that once, rather than each
 * provider carrying its own copy of the request, the error handling and the parsing.
 */
abstract class LlmAnalysisService : AnalysisService {

    /** Human-readable provider name, used in error messages shown to the user. */
    protected abstract val providerName: String

    /**
     * Send one prompt and return the model's text.
     * Throws [AnalysisException] for anything a user needs to be told about.
     */
    protected abstract fun complete(
        systemPrompt: String,
        userPrompt: String,
        customApiKey: String?
    ): String

    override suspend fun analyzeProfile(
        cvText: String,
        targetRole: String,
        intensity: RoastIntensity,
        customApiKey: String?
    ): FullAnalysisResult = withContext(Dispatchers.IO) {
        val text = complete(
            AnalysisPromptHelper.getSystemPrompt(),
            AnalysisPromptHelper.getUserPrompt(intensity, targetRole, cvText),
            customApiKey
        )
        AnalysisPromptHelper.parseJsonResponse(text, intensity, targetRole, cvText)
            ?: throw AnalysisErrors.unreadable()
    }

    override suspend fun analyzeJobAd(
        jobAdText: String,
        intensity: RoastIntensity,
        customApiKey: String?
    ): JobAdAnalysisResult = withContext(Dispatchers.IO) {
        val text = complete(
            JobAdPromptHelper.getSystemPrompt(),
            JobAdPromptHelper.getUserPrompt(intensity, jobAdText),
            customApiKey
        )
        JobAdPromptHelper.parseJsonResponse(text, intensity)
            ?: throw AnalysisErrors.unreadable()
    }

    /** A custom key beats the build-time one; a leftover placeholder counts as absent. */
    protected fun resolveApiKey(customApiKey: String?, buildConfigField: String): String {
        if (!customApiKey.isNullOrBlank()) return customApiKey.trim()
        val fromBuild = try {
            (Class.forName("com.example.BuildConfig")
                .getField(buildConfigField)
                .get(null) as? String)?.trim()
        } catch (_: Exception) {
            null
        }
        return fromBuild?.takeIf { it.isNotBlank() && !it.startsWith("MY_") }
            ?: throw AnalysisErrors.missingKey(providerName)
    }
}
