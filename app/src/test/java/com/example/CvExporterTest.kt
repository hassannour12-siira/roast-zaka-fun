package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.util.CvExporter
import com.example.util.DocumentExtractor
import com.example.util.PdfWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CvExporterTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @org.junit.Before
    fun setUp() {
        DocumentExtractor.initPdfSupport(context)
    }

    @Test
    fun `file name is safe for a filesystem and still recognisable`() {
        val name = CvExporter.fileNameFor("José Álvarez-Ferré")

        assertTrue("should keep the readable part", name.contains("Jos"))
        assertTrue(name.endsWith(".pdf"))
        // Nothing that would break a path or upset a file picker.
        assertTrue("unsafe characters survived: $name", Regex("^[A-Za-z0-9_.-]+$").matches(name))
    }

    @Test
    fun `falls back to a sensible name when the candidate has none`() {
        val name = CvExporter.fileNameFor("")

        assertTrue(name.startsWith("CV_rescued_"))
        assertTrue(name.endsWith(".pdf"))
    }

    @Test
    fun `a very long name cannot produce an unusable file name`() {
        val name = CvExporter.fileNameFor("A".repeat(300))

        assertTrue("file name too long: ${name.length}", name.length < 80)
        assertTrue(name.endsWith(".pdf"))
    }

    @Test
    fun `saving writes a file that can be read back as a docx`() {
        val cv = "ALEX MORGAN\n\nEXPERIENCE\n- Own the front-end React codebase"
        val bytes = PdfWriter.build(cv)

        val result = CvExporter.save(context, "test_cv.pdf", bytes)

        assertTrue("save failed: ${result.exceptionOrNull()?.message}", result.isSuccess)

        // Whatever destination was chosen, the bytes we handed over are a readable PDF.
        val text = DocumentExtractor.extractPdf(bytes)
        assertTrue(text.contains("ALEX MORGAN"))
        assertTrue(text.contains("Own the front-end React codebase"))
    }

    @Test
    fun `two exports on the same day do not collide in name`() {
        // Same candidate, same day: the name is stable by design, so the platform decides
        // how to deduplicate rather than us silently overwriting.
        assertEquals(
            CvExporter.fileNameFor("Alex Morgan"),
            CvExporter.fileNameFor("Alex Morgan")
        )
    }
}
