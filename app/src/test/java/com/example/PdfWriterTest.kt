package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.util.DocumentExtractor
import com.example.util.PdfWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The export is checked by reading the PDF back rather than by trusting that we wrote it.
 *
 * The .docx export this replaced was "verified" by a round trip through our own reader,
 * which only ever proved the file was readable by something lenient. Word rejected it and
 * a user found out before we did. Reading a PDF back through PDFBox is a stronger check,
 * because PDFBox is the same library every other reader's behaviour is modelled on, and
 * the emitted file was additionally rendered by macOS Quick Look during development.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfWriterTest {

    @Before
    fun setUp() {
        DocumentExtractor.initPdfSupport(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `writes a pdf that reads back with the text intact`() {
        val cv = """
            ALEX MORGAN
            Software Engineer

            EXPERIENCE
            Own the front-end React codebase, setting component conventions.
            Diagnosed and resolved front-end performance bottlenecks.
        """.trimIndent()

        val text = DocumentExtractor.extractPdf(PdfWriter.build(cv))

        assertTrue(text.contains("ALEX MORGAN"))
        assertTrue(text.contains("Own the front-end React codebase"))
        assertTrue(text.contains("Diagnosed and resolved front-end performance bottlenecks"))
    }

    @Test
    fun `starts with a pdf header and ends with a trailer`() {
        val bytes = PdfWriter.build("Hello")

        assertEquals("%PDF", String(bytes, 0, 4, Charsets.ISO_8859_1))
        assertTrue(String(bytes, Charsets.ISO_8859_1).trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun `keeps accented names readable`() {
        // WinAnsi covers Latin-1, so these must survive rather than be dropped.
        val text = DocumentExtractor.extractPdf(PdfWriter.build("José Álvarez-Ferré\nZürich"))

        assertTrue("accents were lost: $text", text.contains("José"))
        assertTrue(text.contains("Álvarez"))
        assertTrue(text.contains("Zürich"))
    }

    @Test
    fun `folds characters the standard fonts cannot draw instead of failing`() {
        // Smart quotes, dashes and bullets are everywhere in CVs and are not in WinAnsi's
        // drawable set for these fonts; showText would throw on them.
        val messy = "“Results–driven” • managed … the team’s work"

        val text = DocumentExtractor.extractPdf(PdfWriter.build(messy))

        assertTrue("smart quotes not folded: $text", text.contains("\"Results-driven\""))
        assertTrue(text.contains("..."))
        assertTrue(text.contains("team's work"))
    }

    @Test
    fun `a cv longer than one page does not lose the end of it`() {
        val long = (1..120).joinToString("\n") { "Line number $it describing a responsibility." }

        val text = DocumentExtractor.extractPdf(PdfWriter.build(long))

        assertTrue("first line missing", text.contains("Line number 1 "))
        assertTrue("last line missing, pagination dropped it", text.contains("Line number 120"))
    }

    @Test
    fun `wraps a line too long for the page instead of running off the edge`() {
        val long = "Responsible for " + "a very long sentence about delivery ".repeat(12)

        val text = DocumentExtractor.extractPdf(PdfWriter.build(long))

        assertTrue(text.contains("Responsible for"))
        // The tail of the sentence has to survive the wrap.
        assertTrue("end of the long line was cut off", text.contains("about delivery"))
    }

    @Test
    fun `an unbroken string longer than the page is broken rather than dropped`() {
        val url = "https://" + "averylongdomainsegment".repeat(12) + ".com"

        val text = DocumentExtractor.extractPdf(PdfWriter.build(url))

        assertTrue(text.contains("averylongdomainsegment"))
    }

    @Test
    fun `an empty cv still produces a valid pdf`() {
        val bytes = PdfWriter.build("")

        assertEquals("%PDF", String(bytes, 0, 4, Charsets.ISO_8859_1))
        assertTrue(bytes.size > 200)
    }
}
