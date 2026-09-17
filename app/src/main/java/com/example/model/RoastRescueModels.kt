package com.example.model

enum class RoastIntensity(val label: String, val tagline: String, val peppers: String) {
    LIGHT("Light", "Friendly teasing", "🌶️"),
    SPICY("Spicy", "Your recruiter said what we're all thinking", "🌶️🌶️"),
    EXTRA_SPICY("Extra Spicy", "Proceed at your own risk", "🌶️🌶️🌶️")
}

data class CandidateInfo(
    val name: String = "",
    val headline: String = "",
    val yearsExperience: String? = null
)

enum class Severity {
    LOW, MEDIUM, HIGH
}

data class RoastObservation(
    val title: String,
    val roast: String,
    val problem: String,
    val severity: Severity = Severity.MEDIUM
)

data class RoastData(
    val openingLine: String,
    val observations: List<RoastObservation>,
    val recruiterFirstImpression: String,
    val buzzwords: List<String>,
    val biggestRedFlag: String
)

data class Scores(
    val clarity: Int,
    val specificity: Int,
    val impact: Int,
    val readability: Int,
    val skillsEvidence: Int,
    val jobAlignment: Int? = null
) {
    /**
     * The headline score is a weighted mean of the categories rather than a number the
     * model picks separately. Previously the model returned "overall" on its own, so the
     * big number could disagree with the bars printed right next to it.
     */
    val overall: Int
        get() {
            var total = 0.0
            var weight = 0.0
            fun add(value: Int?, w: Double) {
                if (value == null) return
                total += value.coerceIn(0, 100) * w
                weight += w
            }
            add(clarity, 1.0)
            add(specificity, 1.4)
            add(impact, 1.6)
            add(readability, 0.8)
            add(skillsEvidence, 1.2)
            add(jobAlignment, 1.2)
            return if (weight == 0.0) 0 else Math.round(total / weight).toInt()
        }

    val tierLabel: String
        get() = when (overall) {
            in 90..100 -> "Recruiter Magnet"
            in 75..89 -> "Strong"
            in 60..74 -> "Needs Polish"
            in 40..59 -> "Recruiter Confusion Zone"
            else -> "We Need to Talk"
        }
}

data class BulletRewrite(
    val original: String,
    val improved: String,
    val recommendation: String
)

data class SummaryRescue(
    val original: String,
    val improved: String
)

data class RescueData(
    val topFixes: List<String>,
    val summary: SummaryRescue,
    val bulletRewrites: List<BulletRewrite>
)

data class SkillsAnalysis(
    val demonstrated: List<String>,
    val mentionedButNotDemonstrated: List<String>,
    val recommendedEmphasis: List<String>
)

data class JobMatchData(
    val enabled: Boolean = false,
    val strongMatches: List<String> = emptyList(),
    val notDemonstrated: List<String> = emptyList(),
    val keywordsPresent: List<String> = emptyList(),
    val keywordsMissing: List<String> = emptyList()
)

data class FullAnalysisResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val candidate: CandidateInfo,
    val roast: RoastData,
    val scores: Scores,
    val rescue: RescueData,
    val skills: SkillsAnalysis,
    val jobMatch: JobMatchData,
    val intensity: RoastIntensity,
    val jobTarget: String = "",
    val rawCvSnippet: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    /**
     * True when this came from the built-in offline sample engine rather than a real
     * model call. The UI must say so: a generic roast presented as real analysis is
     * worse than an honest error.
     */
    val isOfflineFallback: Boolean = false
)
