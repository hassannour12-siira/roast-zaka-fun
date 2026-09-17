package com.example

import com.example.model.RoastIntensity
import com.example.model.Severity
import com.example.network.JobAdPromptHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Runs under Robolectric because the parser uses android.util.Log and org.json, both of
 * which are non-functional stubs in a plain JVM unit test: every parse would "fail"
 * regardless of the input.
 */
@RunWith(RobolectricTestRunner::class)
class JobAdPromptHelperTest {

    private val validJson = """
    {
      "job": { "title": "Rockstar Developer", "company": "Acme", "seniority": "Unclear" },
      "roast": {
        "openingLine": "This advert wants a rockstar but offers a tambourine.",
        "issues": [
          { "title": "Ten years of a five year old tool", "roast": "You want 10 years of a framework released in 2021.",
            "problem": "Nobody can meet this. Good people assume the rest is just as careless.", "severity": "high" },
          { "title": "No salary", "roast": "Competitive salary means competitive with what?",
            "problem": "Candidates skip adverts without pay ranges.", "severity": "medium" }
        ],
        "candidateFirstImpression": "Three jobs in one advert, and no money mentioned.",
        "buzzwords": ["rockstar", "fast-paced", "family"],
        "biggestRedFlag": "Ten years of experience demanded for a junior salary band."
      },
      "scores": { "clarity": 40, "honesty": 30, "inclusivity": 60, "realism": 20, "candidateAppeal": 35 },
      "rescue": {
        "topFixes": ["Publish a salary range", "Cut the requirements list to five", "Say which team this is"],
        "lineRewrites": [
          { "original": "We want a rockstar ninja developer", "improved": "We are hiring a backend developer for our payments team.",
            "recommendation": "Name the team and the tech you actually use." }
        ],
        "improvedOpening": "We are hiring a backend developer to work on payments."
      },
      "missingDetails": [
        { "item": "Salary range", "whyItMatters": "Most people will not apply without one." }
      ],
      "unrealisticAsks": ["10 years of a framework that is 4 years old"]
    }
    """.trimIndent()

    @Test
    fun `parses a well formed job advert analysis`() {
        val result = JobAdPromptHelper.parseJsonResponse(validJson, RoastIntensity.SPICY)

        assertNotNull(result)
        requireNotNull(result)
        assertEquals("Rockstar Developer", result.job.title)
        assertEquals(2, result.roast.issues.size)
        assertEquals(Severity.HIGH, result.roast.issues.first().severity)
        assertEquals(3, result.roast.buzzwords.size)
        assertEquals(3, result.rescue.topFixes.size)
        assertEquals(1, result.rescue.lineRewrites.size)
        assertEquals(1, result.missingDetails.size)
        assertEquals("Salary range", result.missingDetails.first().item)
        assertEquals(1, result.unrealisticAsks.size)
        assertEquals(RoastIntensity.SPICY, result.intensity)
    }

    @Test
    fun `overall score is derived from the categories, not invented`() {
        val result = requireNotNull(
            JobAdPromptHelper.parseJsonResponse(validJson, RoastIntensity.SPICY)
        )

        // clarity 40*1.3 + honesty 30*1.2 + inclusivity 60*1.0 + realism 20*1.4 + appeal 35*1.1
        // = 52 + 36 + 60 + 28 + 38.5 = 214.5 over a total weight of 6.0 -> 36
        assertEquals(36, result.scores.overall)
        assertEquals("This is scaring people off", result.scores.tierLabel)
    }

    @Test
    fun `survives a model that wraps its json in prose and code fences`() {
        val wrapped = "Sure! Here is the analysis you asked for:\n```json\n$validJson\n```\nHope that helps."

        val result = JobAdPromptHelper.parseJsonResponse(wrapped, RoastIntensity.LIGHT)

        assertNotNull(result)
        assertEquals("Rockstar Developer", requireNotNull(result).job.title)
    }

    @Test
    fun `returns null rather than a half built result when the json is broken`() {
        assertNull(JobAdPromptHelper.parseJsonResponse("not json at all", RoastIntensity.SPICY))
        assertNull(JobAdPromptHelper.parseJsonResponse("", RoastIntensity.SPICY))
    }

    @Test
    fun `missing optional sections do not crash the parser`() {
        val sparse = """{ "job": { "title": "Developer" } }"""

        val result = JobAdPromptHelper.parseJsonResponse(sparse, RoastIntensity.SPICY)

        assertNotNull(result)
        requireNotNull(result)
        assertEquals("Developer", result.job.title)
        assertTrue(result.roast.issues.isEmpty())
        assertTrue(result.missingDetails.isEmpty())
        assertTrue(result.rescue.topFixes.isEmpty())
    }

    @Test
    fun `the prompt forbids inventing details that are not in the advert`() {
        val prompt = JobAdPromptHelper.getSystemPrompt()

        assertTrue(prompt.contains("Never invent"))
        assertTrue(prompt.contains("PLAIN ENGLISH"))
        // The roast must stay off protected characteristics.
        assertTrue(prompt.contains("disability"))
    }

    @Test
    fun `the advert text is passed to the model inside its own tags`() {
        val prompt = JobAdPromptHelper.getUserPrompt(
            RoastIntensity.EXTRA_SPICY,
            "We need a ninja who thrives in chaos."
        )

        assertTrue(prompt.contains("<job_advert>"))
        assertTrue(prompt.contains("We need a ninja who thrives in chaos."))
        assertTrue(prompt.contains("Extra Spicy"))
    }
}
