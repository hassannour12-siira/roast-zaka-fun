package com.example.model

/**
 * The recruiter-facing side of the app: a job advert gets roasted, then rescued.
 *
 * Deliberately a separate shape from FullAnalysisResult. A CV and a job advert fail in
 * different ways, so sharing one model would have meant fields that are meaningless on
 * one side or the other.
 */

data class JobAdInfo(
    val title: String = "",
    val company: String = "",
    val seniority: String = ""
)

data class JobAdIssue(
    val title: String,
    val roast: String,
    val problem: String,
    val severity: Severity = Severity.MEDIUM
)

/** Something a candidate needs to know that the advert never says. */
data class MissingDetail(
    val item: String,
    val whyItMatters: String
)

data class JobAdRoast(
    val openingLine: String,
    val issues: List<JobAdIssue>,
    val candidateFirstImpression: String,
    val buzzwords: List<String>,
    val biggestRedFlag: String
)

data class JobAdScores(
    val clarity: Int,
    val honesty: Int,
    val inclusivity: Int,
    val realism: Int,
    val candidateAppeal: Int
) {
    /** Weighted mean, so the headline number always agrees with the bars beside it. */
    val overall: Int
        get() {
            val parts = listOf(
                clarity to 1.3,
                honesty to 1.2,
                inclusivity to 1.0,
                realism to 1.4,
                candidateAppeal to 1.1
            )
            val total = parts.sumOf { (value, weight) -> value.coerceIn(0, 100) * weight }
            val weight = parts.sumOf { it.second }
            return Math.round(total / weight).toInt()
        }

    val tierLabel: String
        get() = when (overall) {
            in 90..100 -> "Great advert"
            in 75..89 -> "Solid"
            in 60..74 -> "Needs work"
            in 40..59 -> "Candidates will scroll past"
            else -> "This is scaring people off"
        }
}

data class JobAdRescue(
    val topFixes: List<String>,
    val lineRewrites: List<BulletRewrite>,
    val improvedOpening: String
)

data class JobAdAnalysisResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val job: JobAdInfo,
    val roast: JobAdRoast,
    val scores: JobAdScores,
    val rescue: JobAdRescue,
    val missingDetails: List<MissingDetail>,
    val unrealisticAsks: List<String>,
    val intensity: RoastIntensity,
    val timestamp: Long = System.currentTimeMillis(),
    val isOfflineFallback: Boolean = false
)
