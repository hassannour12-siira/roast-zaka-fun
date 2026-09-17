package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.zip.Inflater
import java.util.zip.ZipInputStream

/**
 * Pulls plain text out of an uploaded CV.
 *
 * Three formats, three strategies:
 *  - DOCX: it is a zip; read word/document.xml and strip the markup. (Previously absent
 *    entirely, so picking a .docx fed raw zip bytes to the model as if they were a CV.)
 *  - PDF: inflate the FlateDecode content streams, then read the text-showing operators.
 *    (Previously the operators were regexed straight out of the raw file, which only ever
 *    works on uncompressed PDFs, almost none of which exist in the wild.)
 *  - Anything else: treat as text.
 */
object DocumentExtractor {

    private const val MIN_USEFUL_CHARS = 40

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

    suspend fun extractTextFromUri(context: Context, uri: Uri): Result<ExtractedDocument> =
        withContext(Dispatchers.IO) {
            val fileName = getFileName(context, uri)
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(Exception("Cannot open that file."))

                val text = when {
                    isDocx(fileName, bytes) -> extractDocx(bytes)
                    isPdf(fileName, bytes) -> extractPdf(bytes)
                    else -> tidy(String(bytes, Charsets.UTF_8))
                }

                if (text.length < MIN_USEFUL_CHARS) {
                    Result.failure(
                        Exception(
                            "We couldn't read enough text from this file. " +
                                "If it's a scanned PDF the text is really an image, so paste the CV text instead."
                        )
                    )
                } else {
                    Result.success(ExtractedDocument(fileName, text, text.length))
                }
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "Failed to read that file."))
            }
        }

    private fun isPdf(fileName: String, bytes: ByteArray): Boolean =
        fileName.endsWith(".pdf", ignoreCase = true) ||
            (bytes.size >= 4 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "%PDF")

    /** A docx is a zip. Check the zip magic bytes so a mislabelled file still works. */
    private fun isDocx(fileName: String, bytes: ByteArray): Boolean =
        fileName.endsWith(".docx", ignoreCase = true) ||
            (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte())

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

    // ---------------------------------------------------------------- PDF

    internal fun extractPdf(bytes: ByteArray): String {
        val builder = StringBuilder()

        val streams = contentStreams(bytes)
        for (stream in streams) {
            builder.append(readTextOperators(stream)).append('\n')
        }

        // Uncompressed PDFs put the operators straight in the file body.
        // If contentStreams found nothing, or if they were all images/garbage, try raw.
        if (builder.isBlank()) {
            builder.append(readTextOperators(String(bytes, Charsets.ISO_8859_1)))
        }
        return tidy(builder.toString())
    }

    /** Every `stream ... endstream` body that inflates as zlib/deflate. */
    private fun contentStreams(bytes: ByteArray): List<String> {
        val out = mutableListOf<String>()
        var index = 0

        val streamMarker = "stream".toByteArray(Charsets.ISO_8859_1)
        val endstreamMarker = "endstream".toByteArray(Charsets.ISO_8859_1)

        while (true) {
            val start = indexOf(bytes, streamMarker, index)
            if (start == -1) break
            val end = indexOf(bytes, endstreamMarker, start + streamMarker.size)
            if (end == -1) break

            // Skip the end-of-line that must follow the `stream` keyword.
            var from = start + streamMarker.size
            if (from < bytes.size && bytes[from] == '\r'.code.toByte()) from++
            if (from < bytes.size && bytes[from] == '\n'.code.toByte()) from++

            if (from < end) {
                val data = bytes.copyOfRange(from, end)
                inflate(data)?.let(out::add)
            }
            index = end + endstreamMarker.size
        }
        return out
    }

    private fun indexOf(src: ByteArray, target: ByteArray, start: Int): Int {
        for (i in start..src.size - target.size) {
            var found = true
            for (j in target.indices) {
                if (src[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun inflate(data: ByteArray): String? = try {
        val inflater = Inflater()
        inflater.setInput(data)
        val buffer = ByteArray(16 * 1024)
        val sink = StringBuilder()
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0) break
            sink.append(String(buffer, 0, n, Charsets.ISO_8859_1))
        }
        inflater.end()
        sink.toString().takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null // Not a deflate stream (an image, a font, or already plain). Skip it.
    }

    /** `(text) Tj` and `[(a) -200 (b)] TJ`, plus the line-positioning operators. */
    internal fun readTextOperators(content: String): String {
        val out = StringBuilder()
        // Strings (literal or hex) and text-showing/positioning operators.
        val token = Regex("""\((?:\\.|[^\\()])*\)|<[0-9A-Fa-f\s]*>|\bT[Jj]\b|\bT[Dd]\b|\bTm\b|\bT\*|'|"""")

        val pending = StringBuilder()
        for (match in token.findAll(content)) {
            val value = match.value
            when {
                value.startsWith("(") ->
                    pending.append(unescape(value.substring(1, value.length - 1)))
                value.startsWith("<") ->
                    pending.append(decodeHex(value.substring(1, value.length - 1)))
                value == "Tj" || value == "TJ" || value == "'" || value == "\"" -> {
                    out.append(pending)
                    if (value == "'" || value == "\"") out.append('\n')
                    pending.clear()
                }
                else -> {
                    // A positioning operator: treat it as a line break.
                    if (pending.isNotEmpty()) {
                        out.append(pending).append('\n')
                        pending.clear()
                    } else if (out.isNotEmpty() && out.last() != '\n') {
                        out.append('\n')
                    }
                }
            }
        }
        out.append(pending)
        return out.toString()
    }

    private fun decodeHex(hex: String): String {
        val clean = hex.replace(Regex("\\s"), "")
        if (clean.isEmpty()) return ""

        val bytes = try {
            val res = ByteArray((clean.length + 1) / 2)
            for (i in 0 until clean.length step 2) {
                val end = if (i + 2 <= clean.length) i + 2 else i + 1
                var part = clean.substring(i, end)
                if (part.length == 1) part += "0" // PDF spec: odd digits are zero-padded
                res[i / 2] = part.toInt(16).toByte()
            }
            res
        } catch (_: Exception) { return "" }

        return when {
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            else -> String(bytes, Charsets.ISO_8859_1)
        }
    }

    private fun unescape(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c != '\\') {
                out.append(c)
                i++
                continue
            }
            if (i + 1 >= text.length) break
            when (val next = text[i + 1]) {
                'n', 'r' -> { out.append('\n'); i += 2 }
                't' -> { out.append('\t'); i += 2 }
                'b', 'f' -> i += 2
                '(', ')', '\\' -> { out.append(next); i += 2 }
                in '0'..'7' -> {
                    var j = i + 1
                    val octal = StringBuilder()
                    while (j < text.length && octal.length < 3 && text[j] in '0'..'7') {
                        octal.append(text[j])
                        j++
                    }
                    out.append(octal.toString().toInt(8).toChar())
                    i = j
                }
                else -> { out.append(next); i += 2 }
            }
        }
        return out.toString()
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
