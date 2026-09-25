package com.cardscanner.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardParserTest {

    private fun lines(vararg pairs: Pair<String, Int>) = pairs.map { OcrLine(it.first, it.second) }

    @Test
    fun parsesTypicalUsCard() {
        val card = CardParser.parse(
            lines(
                "Acme Technologies Inc." to 30,
                "JOHN SMITH" to 48,
                "Senior Software Engineer" to 22,
                "Mobile: +1 (415) 555-0132" to 20,
                "Tel: 415.555.0199" to 20,
                "john.smith@acmetech.com" to 20,
                "www.acmetech.com" to 20,
                "1200 Market Street, Suite 400" to 18,
                "San Francisco, CA 94103" to 18,
            )
        )
        assertEquals("John Smith", card.name)
        assertEquals("Senior Software Engineer", card.jobTitle)
        assertEquals("Acme Technologies Inc.", card.company)
        assertEquals(listOf("+1 (415) 555-0132", "415.555.0199"), card.phones)
        assertEquals(listOf("john.smith@acmetech.com"), card.emails)
        assertEquals("www.acmetech.com", card.website)
        assertEquals("1200 Market Street, Suite 400, San Francisco, CA 94103", card.address)
    }

    @Test
    fun parsesIndianCardAndSkipsFax() {
        val card = CardParser.parse(
            lines(
                "Priya Sharma" to 40,
                "Marketing Manager" to 20,
                "Infosys Pvt Ltd" to 26,
                "M: +91 98765 43210" to 18,
                "Fax: +91 80 2852 0362" to 18,
                "E: priya.sharma@infosys.com" to 18,
                "Plot 44, Electronics City Phase 1" to 16,
                "Bengaluru 560100" to 16,
            )
        )
        assertEquals("Priya Sharma", card.name)
        assertEquals("Marketing Manager", card.jobTitle)
        assertEquals("Infosys Pvt Ltd", card.company)
        assertEquals(listOf("+91 98765 43210"), card.phones)
        assertEquals(listOf("priya.sharma@infosys.com"), card.emails)
        assertTrue(card.address, card.address.contains("Electronics City"))
        assertTrue(card.address, card.address.contains("560100"))
    }

    @Test
    fun companyFallsBackToEmailDomain() {
        val card = CardParser.parse(
            lines(
                "Globex" to 34,
                "Maria Garcia" to 30,
                "Director of Operations" to 18,
                "maria@globex.co.uk" to 16,
                "020 7946 0958" to 16,
            )
        )
        assertEquals("Maria Garcia", card.name)
        assertEquals("Globex", card.company)
        assertEquals("Director of Operations", card.jobTitle)
        assertEquals(listOf("020 7946 0958"), card.phones)
    }

    @Test
    fun freeMailDoesNotBecomeCompany() {
        val card = CardParser.parse(
            lines(
                "Alex Chen" to 30,
                "Photographer" to 16,
                "alexchen.photo@gmail.com" to 14,
                "(212) 555-7788" to 14,
            )
        )
        assertEquals("Alex Chen", card.name)
        assertEquals("", card.company)
        assertEquals(listOf("(212) 555-7788"), card.phones)
    }

    @Test
    fun emptyInputGivesEmptyCard() {
        val card = CardParser.parse(emptyList())
        assertEquals("", card.name)
        assertTrue(card.phones.isEmpty())
    }
}
