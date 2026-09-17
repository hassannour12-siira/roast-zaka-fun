package com.example.network

/**
 * Why an analysis could not be produced, in words we are willing to show a user.
 *
 * Previously every failure here was swallowed and replaced with a canned local roast,
 * so a bad key or a dropped connection looked exactly like a successful analysis. These
 * surface instead.
 */
class AnalysisException(
    val userMessage: String,
    cause: Throwable? = null
) : Exception(userMessage, cause)

internal object AnalysisErrors {
    fun missingKey(providerName: String) = AnalysisException(
        "No $providerName API key set. Add one under the settings icon, top right."
    )

    fun badKey(providerName: String) = AnalysisException(
        "That $providerName API key was rejected. Check it in settings and try again."
    )

    fun rateLimited(providerName: String) = AnalysisException(
        "$providerName is rate limiting us right now. Wait a moment and try again."
    )

    fun truncated() = AnalysisException(
        "The analysis was cut off before it finished. Try a shorter CV, or try again."
    )

    fun refused() = AnalysisException(
        "The model declined to analyse this document. Try a different CV."
    )

    fun unreadable() = AnalysisException(
        "We got a reply we couldn't read. Try again."
    )

    fun network(cause: Throwable) = AnalysisException(
        "Couldn't reach the AI service. Check your connection and try again.",
        cause
    )

    fun http(providerName: String, code: Int) = when (code) {
        401, 403 -> badKey(providerName)
        429 -> rateLimited(providerName)
        else -> AnalysisException("$providerName returned an error ($code). Try again.")
    }
}
