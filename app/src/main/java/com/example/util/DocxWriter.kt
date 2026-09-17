package com.example.util

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a minimal, valid .docx from plain text.
 *
 * A .docx is a zip holding three parts: the content-type map, a relationship pointing at
 * the main document, and the document itself. That is little enough to build by hand and
 * saves pulling in a whole document library for one export.
 *
 * Plain text is all we have to give: the CV arrived as a PDF and only its text survived
 * extraction, so the original fonts and layout are gone either way. A .docx at least opens
 * in Word or Google Docs and can be restyled, which a .txt cannot.
 */
object DocxWriter {

    fun build(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.writeEntry("[Content_Types].xml", CONTENT_TYPES)
            zip.writeEntry("_rels/.rels", RELS)
            zip.writeEntry("word/document.xml", documentXml(text))
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.writeEntry(name: String, body: String) {
        putNextEntry(ZipEntry(name))
        write(body.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun documentXml(text: String): String {
        val paragraphs = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .joinToString("") { paragraph(it) }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>$paragraphs</w:body>
</w:document>"""
    }

    private fun paragraph(line: String): String {
        if (line.isBlank()) return "<w:p/>"
        // xml:space="preserve" keeps leading indentation on bullet lines.
        return "<w:p><w:r><w:t xml:space=\"preserve\">${escape(line)}</w:t></w:r></w:p>"
    }

    private fun escape(text: String): String = buildString(text.length) {
        for (c in text) {
            when {
                c == '&' -> append("&amp;")
                c == '<' -> append("&lt;")
                c == '>' -> append("&gt;")
                c == '"' -> append("&quot;")
                c == '\'' -> append("&apos;")
                c == '\t' -> append("    ")
                // Control characters are illegal in XML and would make Word reject the file.
                c.code < 0x20 -> append(' ')
                else -> append(c)
            }
        }
    }

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    private const val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
}
