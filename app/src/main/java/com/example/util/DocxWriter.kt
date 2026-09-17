package com.example.util

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a .docx from plain text.
 *
 * An earlier version shipped only three parts: `[Content_Types].xml`, `_rels/.rels` and
 * `word/document.xml`. That is enough for lenient readers (macOS `textutil` opened it
 * happily) but Word rejects it as unreadable content. Word wants the main document part to
 * have its own relationships part, and it wants a styles part and document properties to
 * exist rather than be inferred.
 *
 * So all seven parts are written now:
 *
 *   [Content_Types].xml        what each part is
 *   _rels/.rels                package relationships: document, core props, app props
 *   word/document.xml          the text
 *   word/_rels/document.xml.rels  document relationships: styles   <- the missing one
 *   word/styles.xml            a real Normal style, so Word has something to lay out with
 *   docProps/core.xml          title and creator
 *   docProps/app.xml           producing application
 *
 * Plain text is all we have to give. The CV arrived as a PDF and only its text survived
 * extraction, so the original fonts, images and layout are gone either way. That is why
 * the export is a fraction of the size of the file it came from.
 */
object DocxWriter {

    fun build(text: String, title: String = "Rescued CV"): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            // Content types must come first so a reader knows how to treat what follows.
            zip.writeEntry("[Content_Types].xml", CONTENT_TYPES)
            zip.writeEntry("_rels/.rels", PACKAGE_RELS)
            zip.writeEntry("word/document.xml", documentXml(text))
            zip.writeEntry("word/_rels/document.xml.rels", DOCUMENT_RELS)
            zip.writeEntry("word/styles.xml", STYLES)
            zip.writeEntry("docProps/core.xml", coreProps(title))
            zip.writeEntry("docProps/app.xml", APP_PROPS)
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
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<w:body>$paragraphs$SECTION</w:body>
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
                // Control characters are illegal in XML and make Word reject the file.
                c.code < 0x20 -> append(' ')
                else -> append(c)
            }
        }
    }

    private fun coreProps(title: String): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:title>${escape(title)}</dc:title>
<dc:creator>Roastume</dc:creator>
<cp:lastModifiedBy>Roastume</cp:lastModifiedBy>
</cp:coreProperties>"""

    /** A4 with one-inch margins. Word will render without it, but it guesses. */
    private const val SECTION =
        """<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>""" +
            """<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" """ +
            """w:header="708" w:footer="708" w:gutter="0"/></w:sectPr>"""

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>"""

    private const val PACKAGE_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""

    /** Word treats a document part with no relationships part as damaged. */
    private const val DOCUMENT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults>
<w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/><w:sz w:val="22"/><w:szCs w:val="22"/></w:rPr></w:rPrDefault>
<w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="259" w:lineRule="auto"/></w:pPr></w:pPrDefault>
</w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal">
<w:name w:val="Normal"/>
<w:qFormat/>
</w:style>
</w:styles>"""

    private const val APP_PROPS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
<Application>Roastume</Application>
</Properties>"""
}
