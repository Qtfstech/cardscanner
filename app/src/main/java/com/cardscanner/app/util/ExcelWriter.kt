package com.cardscanner.app.util

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a single-sheet .xlsx workbook with a bold header row.
 *
 * An .xlsx file is a zip of a few XML parts; writing them directly avoids a large spreadsheet library.
 */
object ExcelWriter {

    fun write(sheetName: String, header: List<String>, rows: List<List<String>>, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            fun part(path: String, xml: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            part("[Content_Types].xml", CONTENT_TYPES)
            part("_rels/.rels", ROOT_RELS)
            part("xl/workbook.xml", workbook(sheetName))
            part("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            part("xl/styles.xml", STYLES)
            part("xl/worksheets/sheet1.xml", sheet(header, rows))
        }
    }

    private fun sheet(header: List<String>, rows: List<List<String>>): String = buildString {
        val columnCount = maxOf(header.size, rows.maxOfOrNull { it.size } ?: 0)
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        // Freeze the header row so it stays visible while scrolling.
        append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        if (columnCount > 0) {
            append("<cols>")
            for (c in 1..columnCount) {
                val widest = (listOf(header) + rows).maxOf { it.getOrNull(c - 1)?.lines()?.maxOfOrNull(String::length) ?: 0 }
                append("""<col min="$c" max="$c" width="${(widest + 2).coerceIn(10, 60)}" customWidth="1"/>""")
            }
            append("</cols>")
        }
        append("<sheetData>")
        (listOf(header) + rows).forEachIndexed { r, row ->
            append("""<row r="${r + 1}">""")
            row.forEachIndexed { c, value ->
                val style = if (r == 0) 1 else 2
                append("""<c r="${columnName(c)}${r + 1}" t="inlineStr" s="$style"><is><t xml:space="preserve">""")
                append(escape(value))
                append("</t></is></c>")
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    /** 0 -> A, 25 -> Z, 26 -> AA. */
    fun columnName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.insert(0, 'A' + rem)
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun escape(s: String): String = buildString {
        for (ch in s) {
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                // XML 1.0 forbids most control characters.
                ch < ' ' && ch != '\n' && ch != '\t' -> Unit
                else -> append(ch)
            }
        }
    }

    private fun workbook(sheetName: String): String {
        val safe = escape(sheetName.replace(Regex("""[\[\]:*?/\\]"""), " ").take(31).ifBlank { "Cards" })
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
            """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
            """<sheets><sheet name="$safe" sheetId="1" r:id="rId1"/></sheets></workbook>"""
    }

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
            """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            """</Types>"""

    private const val ROOT_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private const val WORKBOOK_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            """</Relationships>"""

    // Style 0: default, 1: bold header, 2: wrapped text aligned to the top.
    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
            """<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>""" +
            """<fill><patternFill patternType="solid"><fgColor rgb="FFDCE6F1"/><bgColor indexed="64"/></patternFill></fill></fills>""" +
            """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>""" +
            """</cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
            """</styleSheet>"""
}
