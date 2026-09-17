package com.example

import com.example.data.SampleCVs
import com.example.model.RoastIntensity
import com.example.model.Severity
import com.example.network.OfflineSampleAnalysis
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun analysisGeneratesValidRoastAndRescue() {
        val sample = SampleCVs.samples.first()
        val result = OfflineSampleAnalysis.generateTailoredAnalysis(
            cvText = sample.cvText,
            targetRole = sample.targetRole,
            intensity = RoastIntensity.SPICY
        )

        assertNotNull(result)
        assertTrue(result.roast.observations.isNotEmpty())
        assertTrue(result.roast.buzzwords.isNotEmpty())
        assertTrue(result.scores.overall in 0..100)
        assertTrue(result.rescue.bulletRewrites.isNotEmpty())
        assertTrue(result.rescue.topFixes.isNotEmpty())
        assertTrue(result.skills.demonstrated.isNotEmpty())
        assertTrue(result.jobMatch.enabled)
        // Anything from the offline engine must identify itself as such.
        assertTrue(result.isOfflineFallback)
    }

    @Test
    fun extraSpicyIntensityChangesTone() {
        val sample = SampleCVs.samples.first()
        val result = OfflineSampleAnalysis.generateTailoredAnalysis(
            cvText = sample.cvText,
            targetRole = sample.targetRole,
            intensity = RoastIntensity.EXTRA_SPICY
        )

        assertEquals(RoastIntensity.EXTRA_SPICY, result.intensity)
        assertTrue(result.roast.openingLine.isNotBlank())
        assertTrue(result.roast.observations.any { it.severity == Severity.HIGH })
    }
}
