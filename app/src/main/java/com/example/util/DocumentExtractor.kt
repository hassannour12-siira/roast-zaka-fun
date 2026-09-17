package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Pulls plain text out of an uploaded CV.
 *
 * PDF goes through PDFBox rather than anything hand-rolled. Two earlier attempts failed on
 * real files: regexing `(text) Tj` out of the raw bytes only works on uncompressed PDFs,
 * and inflating the streams first still produced nothing useful, because anything exported
 * from Google Docs, Word, Canva or LaTeX uses Type0 fonts with Identity-H encoding. There
 * the bytes inside `(...)` are two-byte glyph IDs, not characters, and turning them back
 * into text needs the font's ToUnicode CMap. On one Chrome-exported CV the old parser did
 * not merely return junk, it died with a StackOverflowError from regex backtracking over
 * the binary glyph data.
 *
 * PDFBox already understands encodings, CMaps, subset fonts and every stream filter, so
 * the extraction is now its problem rather than ours.
 *
 * DOCX stays hand-rolled because it is genuinely simple: a zip with an XML part inside.
 */
object DocumentExtractor {

    private const val MIN_USEFUL_CHARS = 40

    /** PDFBox needs this once per process before it touches a document. */
    @Volatile
    private var pdfBoxReady = false

    data class ExtractedDocument(
        val fileName: String,
        val text: String,
        val charCount: Int
    )

    fun getFileName(context: Context, uri: Uri): String {
        var name = "uploaded_cv"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment?.let { name = it }
        }
        return name
    }

    /** Safe to call repeatedly; only the first call does any work. */
    fun initPdfSupport(context: Context) {
        if (pdfBoxReady) return
        synchronized(this) {
            if (pdfBoxReady) return
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxReady = true
        }
    }

    suspend fun extractTextFromUri(context: Context, uri: Uri): Result<ExtractedDocument> =
        withContext(Dispatchers.IO) {
            val fileName = getFileName(context, uri)
            try {
                initPdfSupport(context)

                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(Exception("Cannot open that file."))

                if (bytes.isEmpty()) {
                    return@withContext Result.failure(Exception("That file is empty."))
                }

                val text = when {
                    isDocx(fileName, bytes) -> extractDocx(bytes)
                    isPdf(fileName, bytes) -> extractPdf(bytes)
                    else -> tidy(String(bytes, Charsets.UTF_8))
                }

                if (text.length < MIN_USEFUL_CHARS) {
                    Result.failure(Exception(unreadableMessage(fileName, bytes)))
                } else {
                    Result.success(ExtractedDocument(fileName, text, text.length))
                }
            } catch (e: OutOfMemoryError) {
                Result.failure(Exception("That file is too large to read on this device."))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "Failed to read that file."))
            }
        }

    /** Say something specific enough to act on, rather than one catch-all sentence. */
    private fun unreadableMessage(fileName: String, bytes: ByteArray): String = when {
        isPdf(fileName, bytes) ->
            "We couldn't find any text in this PDF. If it's a scan or an exported image, " +
                "the words are a picture, so copy your CV text and paste it instead."
        isDocx(fileName, bytes) ->
            "We couldn't find any text in this document. Try saving it as a PDF, " +
                "or paste your CV text instead."
        else ->
            "We couldn't read enough text from this file. Upload a PDF or DOCX, " +
                "or paste your CV text instead."
    }

    private fun isPdf(fileName: String, bytes: ByteArray): Boolean =
        fileName.endsWith(".pdf", ignoreCase = true) ||
            (bytes.size >= 4 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "%PDF")

    /** A docx is a zip. Check the zip magic bytes so a mislabelled file still works. */
    private fun isDocx(fileName: String, bytes: ByteArray): Boolean =
        fileName.endsWith(".docx", ignoreCase = true) ||
            (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte())

    // ---------------------------------------------------------------- PDF

    internal fun extractPdf(bytes: ByteArray): String =
        // A PDF locked with a user password throws here, and the catch upstream turns that
        // into a readable message. One locked only against editing still opens for reading.
        PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
            val stripper = PDFTextStripper().apply {
                // Content-stream order, not geometric order. Measured on a two-column CV:
                // sorting by position reads straight across the page and interleaves the
                // sidebar into the job history line by line ("SKILLS EXPERIENCE", "Python,
                // Spark, Airflow, Senior Data Engineer, Meridian..."). Keeping the emitted
                // order holds each column together, which is how CV exporters lay them out.
                sortByPosition = false
                paragraphStart = "\n"
                lineSeparator = "\n"
            }
            tidy(stripper.getText(document))
        }

    // ---------------------------------------------------------------- DOCX

    internal fun extractDocx(bytes: ByteArray): String {
        var documentXml: String? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "word/document.xml") {
                    documentXml = zip.readBytes().toString(Charsets.UTF_8)
                    break
                }
                zip.closeEntry()
            }
        }
        val xml = documentXml ?: return ""

        return tidy(
            xml
                // Paragraph and line breaks become real newlines before the tags are
                // stripped, otherwise every bullet runs into the next one.
                .replace(Regex("</w:p>"), "\n")
                .replace(Regex("<w:br[^>]*/>"), "\n")
                .replace(Regex("<w:tab[^>]*/>"), " ")
                .replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
        )
    }

    // ---------------------------------------------------------------- shared

    private fun tidy(input: String): String =
        input
            // Strip control characters, but keep newlines and tabs as real layout.
            .replace(Regex("[\\p{Cntrl}&&[^\n\t\r]]"), " ")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[ \\t\\u00a0]+"), " ")
            .lines()
            .joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
}
