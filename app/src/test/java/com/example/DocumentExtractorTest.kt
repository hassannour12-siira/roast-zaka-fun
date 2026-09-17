package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.util.DocumentExtractor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real files, from the tools people actually build CVs with.
 *
 * The fixtures matter more than the assertions here. Every previous attempt at PDF
 * extraction passed its own unit tests, because those tests fed it synthetic strings like
 * "(Hello) Tj" that no real PDF contains. The Chrome-exported fixtures below use Type0
 * fonts with Identity-H encoding, which is what Google Docs, Word, Canva and LaTeX all
 * emit, and what every hand-rolled parser has choked on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentExtractorTest {

    @Before
    fun setUp() {
        DocumentExtractor.initPdfSupport(ApplicationProvider.getApplicationContext())
    }

    private fun fixture(name: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) {
            "missing test fixture: $name"
        }.use { it.readBytes() }

    // ------------------------------------------------------------------ PDF

    @Test
    fun `reads a pdf exported from a browser, the Google Docs and Word case`() {
        val text = DocumentExtractor.extractPdf(fixture("cv_chrome.pdf"))

        assertTrue("got only ${text.length} chars", text.length > 300)
        assertTrue("candidate name missing", text.contains("ALEX MORGAN"))
        assertTrue("employer missing", text.contains("Northwind Systems"))
        assertTrue(
            "bullet text missing",
            text.contains("Responsible for managing the front-end codebase")
        )
        assertTrue("skills missing", text.contains("TypeScript"))
        assertTrue("education missing", text.contains("University of Manchester"))
    }

    @Test
    fun `reads a simple uncompressed pdf too`() {
        val text = DocumentExtractor.extractPdf(fixture("cv_simple.pdf"))

        assertTrue("got only ${text.length} chars", text.length > 300)
        assertTrue(text.contains("ALEX MORGAN"))
        assertTrue(text.contains("Northwind"))
    }

    @Test
    fun `reads both columns of a two column cv`() {
        val text = DocumentExtractor.extractPdf(fixture("cv_twocol.pdf"))

        assertTrue("main column missing", text.contains("Meridian Analytics"))
        assertTrue("side column missing", text.contains("Airflow"))
        assertTrue("education missing", text.contains("MSc Computer Science"))
        assertTrue("name missing", text.contains("PRIYA RAGHAVAN"))

        // The sidebar must stay in one piece rather than being interleaved with the job
        // history, which is what sorting by position did.
        val skillsBlock = text.substringAfter("SKILLS").substringBefore("EXPERIENCE")
        assertTrue("sidebar was interleaved with the main column", skillsBlock.contains("Kafka"))
    }

    @Test
    fun `keeps accented characters and smart punctuation intact`() {
        val text = DocumentExtractor.extractPdf(fixture("cv_unicode.pdf"))

        assertTrue("accented name mangled: ${text.take(80)}", text.contains("José"))
        assertTrue("accented surname mangled", text.contains("Álvarez"))
        assertTrue("umlaut mangled", text.contains("Zürich"))
        // A CV full of replacement characters is worse than an honest failure.
        assertFalse("text decoded into replacement characters", text.contains("�"))
    }

    @Test
    fun `does not crash on bytes that are not really a pdf`() {
        // The old regex parser died with a StackOverflowError on binary input.
        val junk = ByteArray(50_000) { (it % 251).toByte() }

        val result = runCatching { DocumentExtractor.extractPdf(junk) }

        assertTrue(
            "should fail cleanly, not fatally: ${result.exceptionOrNull()}",
            result.isFailure || result.getOrNull()?.isEmpty() == true
        )
    }

    // ------------------------------------------------------------------ DOCX

    @Test
    fun `extracts readable text from a docx`() {
        val text = DocumentExtractor.extractDocx(fixture("cv.docx"))

        assertTrue("expected real content, got ${text.length} chars", text.length > 200)
        assertTrue("candidate name missing", text.contains("ALEX MORGAN"))
        assertTrue("experience section missing", text.contains("Northwind Systems"))
        assertFalse("xml markup leaked into the output", text.contains("<w:"))
    }

    @Test
    fun `docx paragraphs do not run together`() {
        val text = DocumentExtractor.extractDocx(fixture("cv.docx"))

        // Every bullet on its own line is what lets the model quote them back.
        assertTrue("paragraphs were flattened into one line", text.lines().size > 5)
    }
}
