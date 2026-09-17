package com.example

import com.example.util.DocumentExtractor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feeds real files through the extractor.
 */
class DocumentExtractorTest {

    private fun fixture(name: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) {
            "missing test fixture: $name"
        }.use { it.readBytes() }

    @Test
    fun `extracts readable text from a docx`() {
        val text = DocumentExtractor.extractDocx(fixture("cv.docx"))

        assertTrue("expected real content, got ${text.length} chars", text.length > 200)
        assertTrue("candidate name missing", text.contains("ALEX MORGAN"))
        assertTrue("experience section missing", text.contains("Northwind Systems"))
        assertTrue("skills section missing", text.contains("React"))
        assertFalse("xml markup leaked into the output", text.contains("<w:"))
    }

    @Test
    fun `extracts readable text from a pdf`() {
        val text = DocumentExtractor.extractPdf(fixture("cv.pdf"))

        assertTrue("expected real content, got ${text.length} chars", text.length > 200)
        assertTrue("candidate name missing", text.contains("ALEX MORGAN"))
        assertTrue("experience section missing", text.contains("Northwind"))
        assertFalse("pdf object syntax leaked into the output", text.contains("endobj"))
    }

    @Test
    fun `docx paragraphs do not run together`() {
        val text = DocumentExtractor.extractDocx(fixture("cv.docx"))

        // Every bullet on its own line is what makes the model able to quote them back.
        assertTrue("paragraphs were flattened into one line", text.lines().size > 5)
    }
}
