package com.example

import com.example.model.*
import com.example.util.CvRewriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CvRewriterTest {

    private val originalCv = """
        ALEX MORGAN
        Software Engineer

        SUMMARY
        A hard-working, motivated and dynamic team player with excellent communication skills.

        EXPERIENCE
        Software Engineer, Northwind Systems (2021 - Present)
        - Responsible for managing the front-end codebase
        - Worked with React and JavaScript
        - Helped improve application performance

        SKILLS
        React, JavaScript, Docker, AWS
    """.trimIndent()

    private fun analysis(
        summary: SummaryRescue = SummaryRescue(
            original = "A hard-working, motivated and dynamic team player with excellent communication skills.",
            improved = "Front-end engineer with five years building production React applications."
        ),
        rewrites: List<BulletRewrite> = listOf(
            BulletRewrite(
                original = "Responsible for managing the front-end codebase",
                improved = "Own the front-end React codebase, setting component conventions.",
                recommendation = "Add codebase size."
            )
        )
    ) = FullAnalysisResult(
        candidate = CandidateInfo("Alex Morgan", "Software Engineer", "5"),
        roast = RoastData("", emptyList(), "", emptyList(), ""),
        scores = Scores(50, 50, 50, 50, 50, null),
        rescue = RescueData(topFixes = emptyList(), summary = summary, bulletRewrites = rewrites),
        skills = SkillsAnalysis(emptyList(), emptyList(), emptyList()),
        jobMatch = JobMatchData(),
        intensity = RoastIntensity.SPICY
    )

    @Test
    fun `swaps the weak summary for the rescued one`() {
        val result = CvRewriter.apply(originalCv, analysis())

        assertTrue(result.text.contains("Front-end engineer with five years"))
        assertFalse(result.text.contains("hard-working, motivated and dynamic"))
    }

    @Test
    fun `swaps a weak bullet for its rewrite`() {
        val result = CvRewriter.apply(originalCv, analysis())

        assertTrue(result.text.contains("Own the front-end React codebase"))
        assertFalse(result.text.contains("Responsible for managing the front-end codebase"))
    }

    @Test
    fun `leaves everything it was not asked to change alone`() {
        val result = CvRewriter.apply(originalCv, analysis())

        // Nothing invented, nothing dropped.
        assertTrue(result.text.contains("ALEX MORGAN"))
        assertTrue(result.text.contains("Northwind Systems (2021 - Present)"))
        assertTrue(result.text.contains("Worked with React and JavaScript"))
        assertTrue(result.text.contains("React, JavaScript, Docker, AWS"))
        assertTrue(result.text.contains("SKILLS"))
    }

    @Test
    fun `matches a bullet the pdf hard wrapped across two lines`() {
        // This is the common case: extraction breaks a long bullet, so the quoted original
        // never matches the document exactly.
        val wrapped = originalCv.replace(
            "- Responsible for managing the front-end codebase",
            "- Responsible for managing\n  the front-end codebase"
        )

        val result = CvRewriter.apply(wrapped, analysis())

        assertTrue("wrapped bullet was not matched", result.text.contains("Own the front-end React codebase"))
        assertTrue(result.applied.any { it.contains("Responsible for managing") })
    }

    @Test
    fun `reports suggestions it could not place instead of dropping them`() {
        val result = CvRewriter.apply(
            originalCv,
            analysis(
                rewrites = listOf(
                    BulletRewrite(
                        original = "Led a team of twelve engineers across three time zones",
                        improved = "Anything at all",
                        recommendation = ""
                    )
                )
            )
        )

        assertEquals(1, result.notApplied.size)
        assertTrue(result.notApplied.first().startsWith("Led a team of twelve"))
        assertFalse("text should be untouched", result.text.contains("Anything at all"))
    }

    @Test
    fun `counts what it applied`() {
        val result = CvRewriter.apply(originalCv, analysis())

        assertEquals(2, result.appliedCount) // summary + one bullet
        assertTrue(result.notApplied.isEmpty())
    }

    @Test
    fun `replaces only the first occurrence of a repeated line`() {
        val repeated = originalCv + "\n- Responsible for managing the front-end codebase"

        val result = CvRewriter.apply(repeated, analysis())

        assertEquals(
            "should not rewrite every copy",
            1,
            Regex("Responsible for managing the front-end codebase").findAll(result.text).count()
        )
    }

    @Test
    fun `handles an empty rescue without damaging the cv`() {
        val result = CvRewriter.apply(
            originalCv,
            analysis(summary = SummaryRescue("", ""), rewrites = emptyList())
        )

        assertEquals(originalCv, result.text)
        assertEquals(0, result.appliedCount)
    }
}
