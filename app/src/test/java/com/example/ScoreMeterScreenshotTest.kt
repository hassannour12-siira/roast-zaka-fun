package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.ScoreRadialMeter
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Was a screenshot of a "Hello Robolectric" placeholder, which told us nothing.
 * Now it guards the score dial, which is the one piece of custom drawing in the app
 * and the thing everybody looks at first on the results screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScoreMeterScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun scoreMeter_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        ScoreRadialMeter(score = 42, label = "Recruiter Confusion Zone")
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/score_meter.png")
  }
}
