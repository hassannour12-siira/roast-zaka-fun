package com.example.network

import android.util.Log
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

object AnalysisPromptHelper {

    fun getSystemPrompt(): String = """
You are the world's sharpest senior tech recruiter and resume doctor.
Task: Analyze the user's CV/resume or LinkedIn profile text. Provide a hilarious, constructive roast followed by an actionable, high-impact rescue.

CRITICAL RULES:
1. Ground every criticism strictly in the supplied CV/profile.
2. Roast the document and presentation, NEVER the human or any sensitive attributes (race, gender, age, religion, disability, etc.).
3. Never invent jobs, skills, achievements, numbers, education or certifications.
4. When rewriting bullets, NEVER fabricate achievements or metrics. If no metrics exist in the CV, rewrite cleanly without fabricating numbers, and add a clear recommendation indicating what metrics the candidate should insert.
5. If skills/experience are absent for a target role, state "Not demonstrated in your CV" rather than claiming the person lacks ability.
6. Return ONLY a single raw valid JSON object matching the requested schema with NO markdown formatting, no backticks, just raw JSON.

WRITE IN PLAIN ENGLISH. This matters as much as the rules above.
Readers include people early in their careers and people reading in a second language.
- Short sentences. Aim for 15 words or fewer. One idea per sentence.
- Everyday words. Write "shows" not "demonstrates", "use" not "utilise", "job" not
  "role positioning", "clear" not "granular", "proof" not "quantifiable substantiation".
- No recruiter or HR jargon unless you immediately explain it in the same sentence.
  Write "an ATS (the software that scans CVs before a human sees them)" the first time.
  Never leave bare jargon like "impact statements", "value proposition", "signal",
  "surface", "leverage", "delta", "top-of-funnel".
- Say exactly what to do. "Add a number showing how many users your app had" beats
  "enhance quantified impact articulation".
- The "problem" field is the teaching moment: explain it the way you would to a friend
  who has never written a CV. Two short sentences, no cleverness.
- The jokes in "roast" stay sharp and funny, but they must be understandable on one
  read. A joke nobody understands is not a joke.
- Never use the words "synergy", "paradigm", "holistic" or "utilise" in your own writing
  unless you are quoting the candidate's CV back at them.

Schema:
{
  "candidate": {
    "name": "Candidate Name or inferred",
    "headline": "Current title or professional headline",
    "yearsExperience": "e.g. 3+ years"
  },
  "roast": {
    "openingLine": "Witty headline opening punchline about this CV",
    "observations": [
      {
        "title": "Short Punchy Title (e.g. Buzzword Overload)",
        "roast": "Funny, specific comedic roast quoting or referencing actual CV lines",
        "problem": "Educational explanation of what is actually wrong in 1-2 sentences",
        "severity": "high" // or "medium" or "low"
      }
    ],
    "recruiterFirstImpression": "Simulated 6-second internal monologue of a recruiter reading this CV",
    "buzzwords": ["cliche1", "cliche2"],
    "biggestRedFlag": "The single biggest presentation issue found"
  },
  "scores": {
    "clarity": 70,
    "specificity": 55,
    "impact": 50,
    "readability": 75,
    "skillsEvidence": 60,
    "jobAlignment": 65
  },
  "rescue": {
    "topFixes": [
      "Top priority 1",
      "Top priority 2",
      "Top priority 3"
    ],
    "summary": {
      "original": "Current summary from CV or 'None provided'",
      "improved": "High impact, evidence-driven rewritten summary"
    },
    "bulletRewrites": [
      {
        "original": "Weak bullet line directly from the CV",
        "improved": "Action + Task + Impact rewrite without fabricated metrics",
        "recommendation": "Add evidence: specify metrics like % latency, user volume, or revenue impact if available"
      }
    ]
  },
  "skills": {
    "demonstrated": ["Skills backed by actual bullet points"],
    "mentionedButNotDemonstrated": ["Skills listed but never proven in work bullets"],
    "recommendedEmphasis": ["High-value skills to highlight more"]
  },
  "jobMatch": {
    "enabled": false,
    "strongMatches": ["Matching skill 1", "Matching skill 2"],
    "notDemonstrated": ["Requested skill not demonstrated in CV"],
    "keywordsPresent": ["Keyword 1", "Keyword 2"],
    "keywordsMissing": ["Missing keyword 1", "Missing keyword 2"]
  }
}
""".trimIndent()

    fun getUserPrompt(intensity: RoastIntensity, targetRole: String, cvText: String): String {
        val intensityPrompt = when (intensity) {
            RoastIntensity.LIGHT -> "Roast Intensity: Light (Friendly teasing, gentle observational humor)."
            RoastIntensity.SPICY -> "Roast Intensity: Spicy (Your recruiter said what we're all thinking, witty, sharp, constructive satire)."
            RoastIntensity.EXTRA_SPICY -> "Roast Intensity: Extra Spicy (Proceed at your own risk, ruthless comedic breakdown of buzzwords, vagueness and formatting clichés)."
        }

        return """
$intensityPrompt
${if (targetRole.isNotBlank()) "Target Job / Role: $targetRole" else "General Career Review"}

Candidate CV / Profile Text:
$cvText
        """.trimIndent()
    }

    fun parseJsonResponse(
        jsonString: String,
        intensity: RoastIntensity,
        targetRole: String,
        cvSnippet: String
    ): FullAnalysisResult? {
        if (jsonString.isBlank()) {
            Log.e("AnalysisPromptHelper", "Error parsing model JSON response: input string is blank")
            return null
        }
        try {
            var cleanJson = jsonString.trim()
            
            // Robustly extract JSON object if wrapped in conversational text or markdown blocks
            val firstBrace = cleanJson.indexOf('{')
            val lastBrace = cleanJson.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleanJson = cleanJson.substring(firstBrace, lastBrace + 1)
            } else {
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.removePrefix("```json")
                } else if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.removePrefix("```")
                }
                if (cleanJson.endsWith("```")) {
                    cleanJson = cleanJson.removeSuffix("```")
                }
                cleanJson = cleanJson.trim()
            }

            if (cleanJson.isBlank()) {
                Log.e("AnalysisPromptHelper", "Error parsing model JSON response: cleaned string is blank")
                return null
            }

            val json = JSONObject(cleanJson)

            val candObj = json.optJSONObject("candidate") ?: JSONObject()
            val candidate = CandidateInfo(
                name = candObj.optString("name", "Candidate"),
                headline = candObj.optString("headline", "Professional"),
                yearsExperience = if (candObj.has("yearsExperience") && !candObj.isNull("yearsExperience")) candObj.optString("yearsExperience") else null
            )

            val roastObj = json.optJSONObject("roast") ?: JSONObject()
            val obsArray = roastObj.optJSONArray("observations") ?: JSONArray()
            val observations = mutableListOf<RoastObservation>()
            for (i in 0 until obsArray.length()) {
                val o = obsArray.optJSONObject(i) ?: continue
                val sevStr = o.optString("severity", "medium").lowercase()
                val severity = when (sevStr) {
                    "high" -> Severity.HIGH
                    "low" -> Severity.LOW
                    else -> Severity.MEDIUM
                }
                observations.add(
                    RoastObservation(
                        title = o.optString("title", "Presentation Flaw"),
                        roast = o.optString("roast", ""),
                        problem = o.optString("problem", ""),
                        severity = severity
                    )
                )
            }

            val buzzArray = roastObj.optJSONArray("buzzwords") ?: JSONArray()
            val buzzwords = mutableListOf<String>()
            for (i in 0 until buzzArray.length()) {
                buzzwords.add(buzzArray.optString(i))
            }

            val roastData = RoastData(
                openingLine = roastObj.optString("openingLine", "Your CV thinks it's ready. The recruiters disagree."),
                observations = observations,
                recruiterFirstImpression = roastObj.optString("recruiterFirstImpression", "Scanned in 5 seconds. Needs more evidence."),
                buzzwords = buzzwords,
                biggestRedFlag = roastObj.optString("biggestRedFlag", "Too many generic adjectives without quantified achievements.")
            )

            val scoresObj = json.optJSONObject("scores") ?: JSONObject()
            val scores = Scores(
                clarity = scoresObj.optInt("clarity", 65),
                specificity = scoresObj.optInt("specificity", 50),
                impact = scoresObj.optInt("impact", 45),
                readability = scoresObj.optInt("readability", 75),
                skillsEvidence = scoresObj.optInt("skillsEvidence", 55),
                jobAlignment = if (scoresObj.has("jobAlignment") && !scoresObj.isNull("jobAlignment")) scoresObj.optInt("jobAlignment") else null
            )

            val rescueObj = json.optJSONObject("rescue") ?: JSONObject()
            val topFixesArray = rescueObj.optJSONArray("topFixes") ?: JSONArray()
            val topFixes = mutableListOf<String>()
            for (i in 0 until topFixesArray.length()) {
                topFixes.add(topFixesArray.optString(i))
            }

            val sumObj = rescueObj.optJSONObject("summary") ?: JSONObject()
            val summary = SummaryRescue(
                original = sumObj.optString("original", "Not provided"),
                improved = sumObj.optString("improved", "Results-oriented professional with demonstrated experience.")
            )

            val bulletsArray = rescueObj.optJSONArray("bulletRewrites") ?: JSONArray()
            val bulletRewrites = mutableListOf<BulletRewrite>()
            for (i in 0 until bulletsArray.length()) {
                val b = bulletsArray.optJSONObject(i) ?: continue
                bulletRewrites.add(
                    BulletRewrite(
                        original = b.optString("original", ""),
                        improved = b.optString("improved", ""),
                        recommendation = b.optString("recommendation", "")
                    )
                )
            }

            val rescueData = RescueData(
                topFixes = topFixes,
                summary = summary,
                bulletRewrites = bulletRewrites
            )

            val skillsObj = json.optJSONObject("skills") ?: JSONObject()
            val skills = SkillsAnalysis(
                demonstrated = jsonArrayToList(skillsObj.optJSONArray("demonstrated")),
                mentionedButNotDemonstrated = jsonArrayToList(skillsObj.optJSONArray("mentionedButNotDemonstrated")),
                recommendedEmphasis = jsonArrayToList(skillsObj.optJSONArray("recommendedEmphasis"))
            )

            val jobObj = json.optJSONObject("jobMatch") ?: JSONObject()
            val jobMatch = JobMatchData(
                enabled = targetRole.isNotBlank() || jobObj.optBoolean("enabled", false),
                strongMatches = jsonArrayToList(jobObj.optJSONArray("strongMatches")),
                notDemonstrated = jsonArrayToList(jobObj.optJSONArray("notDemonstrated")),
                keywordsPresent = jsonArrayToList(jobObj.optJSONArray("keywordsPresent")),
                keywordsMissing = jsonArrayToList(jobObj.optJSONArray("keywordsMissing"))
            )

            return FullAnalysisResult(
                candidate = candidate,
                roast = roastData,
                scores = scores,
                rescue = rescueData,
                skills = skills,
                jobMatch = jobMatch,
                intensity = intensity,
                jobTarget = targetRole,
                rawCvSnippet = cvSnippet.take(150)
            )
        } catch (e: Exception) {
            Log.e("AnalysisPromptHelper", "Error parsing model JSON response", e)
            return null
        }
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optString(i)
            if (item.isNotBlank()) list.add(item)
        }
        return list
    }
}
