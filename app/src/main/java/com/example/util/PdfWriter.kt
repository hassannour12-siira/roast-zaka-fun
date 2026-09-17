package com.example.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream

/**
 * Writes the rewritten CV as a PDF.
 *
 * This replaced a hand-rolled .docx export that Word would not open. Two things make a PDF
 * the better answer rather than a better .docx: it is produced by PDFBox, a real library
 * we already depend on for reading, so its structure is not something we invented; and it
 * opens on any phone without an office app installed, which a .docx does not. It is also
 * the format a recruiter expects to receive.
 *
 * Layout is deliberately plain. The CV arrived as a PDF and only its text survived
 * extraction, so the original fonts and design are gone regardless; the job here is a
 * clean, readable document rather than an imitation of the original.
 */
object PdfWriter {

    private const val MARGIN = 56f          // ~20mm
    private const val BODY_SIZE = 10.5f
    private const val HEADING_SIZE = 13f
    private const val LINE_GAP = 1.35f

    fun build(text: String, title: String = "CV"): ByteArray {
        val document = PDDocument()
        try {
            document.documentInformation.title = title
            document.documentInformation.creator = "Roastume"

            val body: PDFont = PDType1Font.HELVETICA
            val bold: PDFont = PDType1Font.HELVETICA_BOLD

            val pageWidth = PDRectangle.A4.width
            val usableWidth = pageWidth - MARGIN * 2

            // Wrap every source line to the page width before laying anything out, so the
            // page-break logic only ever deals with lines that already fit.
            val lines = mutableListOf<Line>()
            for (raw in text.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
                val clean = sanitise(raw)
                if (clean.isBlank()) {
                    lines.add(Line("", false))
                    continue
                }
                val heading = looksLikeHeading(clean)
                val font = if (heading) bold else body
                val size = if (heading) HEADING_SIZE else BODY_SIZE
                for (part in wrap(clean, font, size, usableWidth)) {
                    lines.add(Line(part, heading))
                }
            }

            var page = newPage(document)
            var stream = PDPageContentStream(document, page)
            var y = PDRectangle.A4.height - MARGIN
            stream.beginText()
            stream.newLineAtOffset(MARGIN, y)
            var textOpen = true

            for (line in lines) {
                val size = if (line.heading) HEADING_SIZE else BODY_SIZE
                val step = size * LINE_GAP

                if (y - step < MARGIN) {
                    if (textOpen) stream.endText()
                    stream.close()
                    page = newPage(document)
                    stream = PDPageContentStream(document, page)
                    y = PDRectangle.A4.height - MARGIN
                    stream.beginText()
                    stream.newLineAtOffset(MARGIN, y)
                    textOpen = true
                }

                stream.setFont(if (line.heading) bold else body, size)
                stream.showText(line.text)
                stream.newLineAtOffset(0f, -step)
                y -= step
            }

            if (textOpen) stream.endText()
            stream.close()

            val out = ByteArrayOutputStream()
            document.save(out)
            return out.toByteArray()
        } finally {
            document.close()
        }
    }

    private data class Line(val text: String, val heading: Boolean)

    private fun newPage(document: PDDocument): PDPage =
        PDPage(PDRectangle.A4).also { document.addPage(it) }

    /** ALL CAPS short lines are how CV sections are almost always written. */
    private fun looksLikeHeading(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.length > 40) return false
        val letters = trimmed.filter { it.isLetter() }
        if (letters.length < 3) return false
        return letters.all { it.isUpperCase() }
    }

    private fun wrap(line: String, font: PDFont, size: Float, maxWidth: Float): List<String> {
        val words = line.split(' ')
        val out = mutableListOf<String>()
        var current = StringBuilder()

        fun width(s: String) = font.getStringWidth(s) / 1000f * size

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (width(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) out.add(current.toString())
                // A single word longer than the line (a URL, say) has to be broken by hand.
                if (width(word) > maxWidth) {
                    var chunk = StringBuilder()
                    for (c in word) {
                        if (width("$chunk$c") > maxWidth && chunk.isNotEmpty()) {
                            out.add(chunk.toString())
                            chunk = StringBuilder()
                        }
                        chunk.append(c)
                    }
                    current = chunk
                } else {
                    current = StringBuilder(word)
                }
            }
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out.ifEmpty { listOf("") }
    }

    /**
     * The standard PDF fonts only cover WinAnsi. Anything outside it makes showText throw,
     * which would fail the whole export, so unsupported characters are folded to the
     * nearest sensible ASCII rather than left to blow up. Accented Latin letters survive:
     * they are inside WinAnsi.
     */
    private fun sanitise(text: String): String = buildString(text.length) {
        for (c in text) {
            when (c) {
                '‘', '’', '‚', '′' -> append('\'')
                '“', '”', '„', '″' -> append('"')
                '–', '—', '−' -> append('-')
                '…' -> append("...")
                '•', '·', '●', '▪', '◦' -> append('-')
                ' ', ' ', ' ' -> append(' ')
                '™' -> append("(TM)")
                '\t' -> append("    ")
                else -> if (c.code in 0x20..0xFF || c == '\n') append(c) else Unit
            }
        }
    }
}
