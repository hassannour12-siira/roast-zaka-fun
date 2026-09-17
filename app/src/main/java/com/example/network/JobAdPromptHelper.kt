package com.example.network

import android.util.Log
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Prompt and parser for roasting a job advert.
 *
 * The audience here is the recruiter who wrote the advert, so the roast targets the
 * writing and the hiring process behind it, never the company's people.
 */
object JobAdPromptHelper {

    fun getSystemPrompt(): String = """
You are a blunt, very funny hiring consultant. Recruiters bring you their job adverts and
you tell them the truth about how the advert reads to the people they are trying to hire.

Your job:
1. THE ROAST: funny, specific observations about how this advert is written.
2. THE FIXES: practical rewrites that would get better candidates to apply.

WHAT TO ROAST
Fair game: meaningless buzzwords ("rockstar", "ninja", "wear many hats", "fast-paced",
"we're like a family", "work hard play hard"), impossible requirements (more years of a
technology than it has existed, ten years for a junior role), enormous wish-lists of
skills, no salary, unclear seniority, vague responsibilities, a list of demands with
nothing offered in return, and adverts that describe three separate jobs as one.

HARD RULES
1. Ground every criticism in text that actually appears in the advert. Quote the real line.
2. Never invent requirements, benefits, salary figures or company details that are not there.
3. Roast the WRITING and the ROLE DESIGN, never the company's staff, and never anything
   about the people who might apply.
4. Never suggest wording that would exclude or discourage anyone on the basis of age,
   sex, race, religion, disability, nationality, pregnancy or any other personal
   characteristic. If the advert already contains wording like that, flag it plainly as a
   legal and fairness problem, not as a joke.
5. If the advert leaves something out (salary, location, seniority, working pattern), list
   it under missingDetails. Do not guess what it should be.
6. If a requirement is impossible or self-contradictory, put it in unrealisticAsks and say
   why in plain words.
7. Return ONLY a single raw valid JSON object matching the schema. No markdown, no backticks.

WRITE IN PLAIN ENGLISH
- Short sentences, 15 words or fewer where you can.
- Everyday words. No consulting jargon, no HR jargon.
- The "problem" field explains the damage in two short sentences: who stops reading, and why.
- Jokes must be understandable on one read.

Schema:
{
  "job": {
    "title": "The role title as written, or 'Not stated'",
    "company": "Company name if given, else 'Not stated'",
    "seniority": "Junior / Mid / Senior / Unclear"
  },
  "roast": {
    "openingLine": "One witty line about this advert as a whole",
    "issues": [
      {
        "title": "Short punchy title",
        "roast": "Funny, specific line quoting the advert",
        "problem": "Plain explanation of who this puts off and why",
        "severity": "high"
      }
    ],
    "candidateFirstImpression": "What a good candidate thinks while skim-reading this",
    "buzzwords": ["rockstar", "fast-paced"],
    "biggestRedFlag": "The single thing most likely to stop good people applying"
  },
  "scores": {
    "clarity": 60,
    "honesty": 55,
    "inclusivity": 70,
    "realism": 40,
    "candidateAppeal": 45
  },
  "rescue": {
    "topFixes": ["Fix 1", "Fix 2", "Fix 3"],
    "lineRewrites": [
      {
        "original": "A weak line copied from the advert",
        "improved": "A clearer rewrite that invents nothing",
        "recommendation": "What the recruiter still needs to fill in themselves"
      }
    ],
    "improvedOpening": "A rewritten opening paragraph for the advert"
  },
  "missingDetails": [
    { "item": "Salary range", "whyItMatters": "Plain reason candidates care" }
  ],
  "unrealisticAsks": ["Requirement that cannot be met, and why in a few words"]
}

Produce 4 to 6 issues, exactly 3 topFixes, and 2 to 4 lineRewrites.
""".trimIndent()

    fun getUserPrompt(intensity: RoastIntensity, jobAdText: String): String {
        val tone = when (intensity) {
            RoastIntensity.LIGHT ->
                "Roast intensity: Light. Friendly and encouraging. You like this recruiter."
            RoastIntensity.SPICY ->
                "Roast intensity: Spicy. Say what candidates mutter when they read adverts like this."
            RoastIntensity.EXTRA_SPICY ->
                "Roast intensity: Extra Spicy. Merciless about the advert. Still fair, still useful."
        }

        return """
$tone

Here is the job advert to review. Work only with what is written here.

<job_advert>
$jobAdText
</job_advert>

Produce the full roast and fixes.
        """.trimIndent()
    }

    fun parseJsonResponse(jsonString: String, intensity: RoastIntensity): JobAdAnalysisResult? {
        if (jsonString.isBlank()) return null
        return try {
            val json = JSONObject(extractJsonObject(jsonString))

            val jobObj = json.optJSONObject("job") ?: JSONObject()
            val job = JobAdInfo(
                title = jobObj.optString("title", "Not stated"),
                company = jobObj.optString("company", "Not stated"),
                seniority = jobObj.optString("seniority", "Unclear")
            )

            val roastObj = json.optJSONObject("roast") ?: JSONObject()
            val issues = mutableListOf<JobAdIssue>()
            val issuesArray = roastObj.optJSONArray("issues") ?: JSONArray()
            for (i in 0 until issuesArray.length()) {
                val o = issuesArray.optJSONObject(i) ?: continue
                issues.add(
                    JobAdIssue(
                        title = o.optString("title", "Problem"),
                        roast = o.optString("roast", ""),
                        problem = o.optString("problem", ""),
                        severity = toSeverity(o.optString("severity", "medium"))
                    )
                )
            }

            val roast = JobAdRoast(
                openingLine = roastObj.optString(
                    "openingLine",
                    "This advert asks for a lot and says very little."
                ),
                issues = issues,
                candidateFirstImpression = roastObj.optString("candidateFirstImpression", ""),
                buzzwords = toList(roastObj.optJSONArray("buzzwords")),
                biggestRedFlag = roastObj.optString("biggestRedFlag", "")
            )

            val scoresObj = json.optJSONObject("scores") ?: JSONObject()
            val scores = JobAdScores(
                clarity = scoresObj.optInt("clarity", 50),
                honesty = scoresObj.optInt("honesty", 50),
                inclusivity = scoresObj.optInt("inclusivity", 50),
                realism = scoresObj.optInt("realism", 50),
                candidateAppeal = scoresObj.optInt("candidateAppeal", 50)
            )

            val rescueObj = json.optJSONObject("rescue") ?: JSONObject()
            val rewrites = mutableListOf<BulletRewrite>()
            val rewritesArray = rescueObj.optJSONArray("lineRewrites") ?: JSONArray()
            for (i in 0 until rewritesArray.length()) {
                val o = rewritesArray.optJSONObject(i) ?: continue
                rewrites.add(
                    BulletRewrite(
                        original = o.optString("original", ""),
                        improved = o.optString("improved", ""),
                        recommendation = o.optString("recommendation", "")
                    )
                )
            }
            val rescue = JobAdRescue(
                topFixes = toList(rescueObj.optJSONArray("topFixes")),
                lineRewrites = rewrites,
                improvedOpening = rescueObj.optString("improvedOpening", "")
            )

            val missing = mutableListOf<MissingDetail>()
            val missingArray = json.optJSONArray("missingDetails") ?: JSONArray()
            for (i in 0 until missingArray.length()) {
                val o = missingArray.optJSONObject(i) ?: continue
                val item = o.optString("item", "")
                if (item.isNotBlank()) {
                    missing.add(MissingDetail(item, o.optString("whyItMatters", "")))
                }
            }

            JobAdAnalysisResult(
                job = job,
                roast = roast,
                scores = scores,
                rescue = rescue,
                missingDetails = missing,
                unrealisticAsks = toList(json.optJSONArray("unrealisticAsks")),
                intensity = intensity
            )
        } catch (e: Exception) {
            Log.e("JobAdPromptHelper", "Error parsing job advert JSON", e)
            null
        }
    }

    private fun toSeverity(raw: String): Severity = when (raw.trim().lowercase()) {
        "high" -> Severity.HIGH
        "low" -> Severity.LOW
        else -> Severity.MEDIUM
    }

    private fun toList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length())
            .map { array.optString(it) }
            .filter { it.isNotBlank() }
    }

    /** Models sometimes wrap JSON in prose or code fences. Take the outermost object. */
    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
        val first = trimmed.indexOf('{')
        val last = trimmed.lastIndexOf('}')
        return if (first != -1 && last > first) trimmed.substring(first, last + 1) else trimmed
    }
}
