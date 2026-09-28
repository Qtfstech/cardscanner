package com.cardscanner.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class ExcelWriterTest {

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val parts = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                parts[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return parts
    }

    @Test
    fun writesWellFormedWorkbookWithCompanyFirst() {
        val out = ByteArrayOutputStream()
        ExcelWriter.write(
            "Trade show: Sept/2026",
            listOf("Company", "Name"),
            listOf(listOf("Acme & Sons <Ltd>", "Jane \"JJ\" Doe"), listOf("Globex", "Line1\nLine2\u0001")),
            out,
        )
        val parts = unzip(out.toByteArray())
        assertEquals(
            setOf(
                "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
                "xl/styles.xml", "xl/worksheets/sheet1.xml",
            ),
            parts.keys,
        )
        val factory = DocumentBuilderFactory.newInstance()
        parts.values.forEach { xml -> factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray())) }

        val sheet = parts.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.indexOf(">Company<") < sheet.indexOf(">Name<"))
        assertTrue(sheet.contains("Acme &amp; Sons &lt;Ltd&gt;"))
        assertTrue(sheet.contains("""<c r="B3""""))
        // Sheet names can't contain : or /.
        assertTrue(parts.getValue("xl/workbook.xml").contains("name=\"Trade show  Sept 2026\""))
    }

    @Test
    fun columnNames() {
        assertEquals("A", ExcelWriter.columnName(0))
        assertEquals("Z", ExcelWriter.columnName(25))
        assertEquals("AA", ExcelWriter.columnName(26))
        assertEquals("AZ", ExcelWriter.columnName(51))
    }
}
