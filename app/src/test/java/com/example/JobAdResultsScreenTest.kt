package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.model.*
import com.example.ui.screens.JOB_AD_LIST_TAG
import com.example.ui.screens.JobAdResultsScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the recruiter results screen end to end against a fixture, so a composition
 * crash or a missing section shows up here rather than on someone's phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class JobAdResultsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val result = JobAdAnalysisResult(
        job = JobAdInfo(title = "Rockstar Developer", company = "Acme", seniority = "Unclear"),
        roast = JobAdRoast(
            openingLine = "This advert wants a rockstar but offers a tambourine.",
            issues = listOf(
                JobAdIssue(
                    title = "Ten years of a four year old tool",
                    roast = "You want a decade of a framework that is four years old.",
                    problem = "Nobody can meet this. Good people assume the rest is careless too.",
                    severity = Severity.HIGH
                )
            ),
            candidateFirstImpression = "Three jobs in one advert, and no money mentioned.",
            buzzwords = listOf("rockstar", "fast-paced"),
            biggestRedFlag = "No salary anywhere in the advert."
        ),
        scores = JobAdScores(
            clarity = 40,
            honesty = 30,
            inclusivity = 60,
            realism = 20,
            candidateAppeal = 35
        ),
        rescue = JobAdRescue(
            topFixes = listOf("Publish a salary range", "Cut the requirements", "Name the team"),
            lineRewrites = listOf(
                BulletRewrite(
                    original = "We want a rockstar ninja developer",
                    improved = "We are hiring a backend developer for our payments team.",
                    recommendation = "Name the tech you actually use."
                )
            ),
            improvedOpening = "We are hiring a backend developer to work on payments."
        ),
        missingDetails = listOf(
            MissingDetail("Salary range", "Most people will not apply without one.")
        ),
        unrealisticAsks = listOf("10 years of a framework that is 4 years old"),
        intensity = RoastIntensity.SPICY
    )

    /** LazyColumn only composes what is on screen, so scroll before asserting. */
    private fun scrollTo(text: String) {
        composeTestRule.onNodeWithTag(JOB_AD_LIST_TAG).performScrollToNode(hasText(text))
    }

    private fun render() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                JobAdResultsScreen(result = result, onStartOver = {})
            }
        }
    }

    @Test
    fun `shows the roast headline and the advert title`() {
        render()

        composeTestRule.onNodeWithText("Rockstar Developer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your job advert, roasted").assertIsDisplayed()
    }

    @Test
    fun `shows the computed score, not a number from the model`() {
        render()

        // The weighted mean of the five categories above.
        scrollTo("36")
        composeTestRule.onNodeWithText("36").assertIsDisplayed()
    }

    @Test
    fun `shows the recruiter specific sections`() {
        render()

        scrollTo("Asks that nobody can meet ⛔")
        composeTestRule.onNodeWithText("Asks that nobody can meet ⛔").assertIsDisplayed()

        scrollTo("You forgot to say")
        composeTestRule.onNodeWithText("You forgot to say").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salary range").assertIsDisplayed()

        scrollTo("A better opening")
        composeTestRule.onNodeWithText("A better opening").assertIsDisplayed()
    }

    @Test
    fun `shows the fixes`() {
        render()

        scrollTo("Fix these three first")
        composeTestRule.onNodeWithText("Fix these three first").assertIsDisplayed()
        composeTestRule.onNodeWithText("Publish a salary range").assertIsDisplayed()
    }
}
