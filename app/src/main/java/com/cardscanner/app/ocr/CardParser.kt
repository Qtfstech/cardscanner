package com.cardscanner.app.ocr

/** One line of recognized text; [height] is the line's pixel height, used to spot the name. */
data class OcrLine(val text: String, val height: Int = 0)

data class ParsedCard(
    val name: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val website: String = "",
    val address: String = "",
    val rawText: String = "",
)

/**
 * Turns the loose lines of text found on a business card into contact fields.
 *
 * Pure Kotlin with no Android dependencies, so it is covered by plain JVM unit tests.
 */
object CardParser {

    private val EMAIL = Regex("""[A-Za-z0-9._%+\-]+\s?@\s?[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val URL = Regex(
        """(?i)\b((?:https?://)?(?:www\.)[A-Za-z0-9\-]+(?:\.[A-Za-z0-9\-]+)+(?:/\S*)?|(?:https?://)[A-Za-z0-9\-]+(?:\.[A-Za-z0-9\-]+)+(?:/\S*)?|[A-Za-z0-9\-]+\.(?:com|net|org|io|co|in|biz|info|us|uk|ca|au|de|ai|app|dev|tech|co\.in|co\.uk)\b(?:/\S*)?)"""
    )
    private val PHONE = Regex("""\+?\(?\d[\d\s().\-/]{5,}\d""")
    private val PHONE_LABEL = Regex(
        """(?i)^\s*(tel|telephone|phone|ph|mob|mobile|cell|cel|m|t|p|o|office|direct|d|w|work|h|home)\b\.?\s*[:.]?\s*"""
    )
    private val FAX = Regex("""(?i)\bfax\b|^\s*f\s*[:.]""")
    private val ZIP = Regex("""\b\d{5}(?:-\d{4})?\b|\b\d{3}\s?\d{3}\b|\b[A-Z]\d[A-Z]\s?\d[A-Z]\d\b|\b[A-Z]{1,2}\d[A-Z\d]?\s?\d[A-Z]{2}\b""")

    private val ADDRESS_WORDS = setOf(
        "street", "st", "road", "rd", "avenue", "ave", "blvd", "boulevard", "lane", "ln", "drive", "dr",
        "suite", "ste", "floor", "fl", "building", "bldg", "tower", "plaza", "highway", "hwy", "way",
        "court", "ct", "place", "pl", "square", "sq", "parkway", "pkwy", "block", "sector", "nagar",
        "marg", "colony", "layout", "phase", "cross", "main", "po", "box", "p.o.", "apt", "unit",
        "district", "city", "state", "usa", "india", "uk", "canada", "australia", "germany",
    )

    private val COMPANY_WORDS = setOf(
        "inc", "inc.", "llc", "ltd", "ltd.", "limited", "corp", "corp.", "corporation", "co.", "company",
        "pvt", "pvt.", "private", "plc", "gmbh", "ag", "sa", "bv", "llp", "group", "holdings",
        "technologies", "technology", "tech", "solutions", "systems", "software", "consulting",
        "consultants", "services", "industries", "enterprises", "labs", "studio", "studios", "agency",
        "partners", "associates", "bank", "hospital", "clinic", "university", "college", "institute",
        "foundation", "media", "global", "international", "ventures", "capital", "realty", "motors",
    )

    private val TITLE_WORDS = setOf(
        "ceo", "cto", "cfo", "coo", "cmo", "cio", "vp", "svp", "evp", "founder", "co-founder", "cofounder",
        "president", "chairman", "director", "manager", "head", "lead", "engineer", "developer",
        "designer", "architect", "consultant", "analyst", "officer", "executive", "associate",
        "specialist", "coordinator", "administrator", "assistant", "partner", "owner", "proprietor",
        "sales", "marketing", "representative", "advisor", "adviser", "attorney", "lawyer", "counsel",
        "doctor", "physician", "surgeon", "professor", "lecturer", "scientist",
        "accountant", "agent", "broker", "realtor", "principal", "senior", "sr.", "junior", "jr.",
        "intern", "supervisor", "technician", "officer", "secretary", "treasurer", "editor", "producer",
    )

    private val FREE_MAIL_DOMAINS = setOf(
        "gmail", "yahoo", "hotmail", "outlook", "live", "icloud", "aol", "protonmail", "proton",
        "rediffmail", "ymail", "msn", "mail", "gmx", "zoho",
    )

    fun parse(lines: List<OcrLine>): ParsedCard {
        val clean = lines
            .map { it.copy(text = it.text.replace(Regex("""\s+"""), " ").trim()) }
            .filter { it.text.isNotEmpty() }

        val emails = mutableListOf<String>()
        val phones = mutableListOf<String>()
        var website = ""
        val leftovers = mutableListOf<OcrLine>()

        for (line in clean) {
            var rest = line.text

            EMAIL.findAll(rest).forEach { m -> emails += m.value.replace(" ", "").lowercase() }
            rest = EMAIL.replace(rest, " ")

            URL.findAll(rest).forEach { m ->
                if (website.isEmpty()) website = m.value.trimEnd('.', ',', ';')
            }
            rest = URL.replace(rest, " ")

            if (!FAX.containsMatchIn(rest)) {
                PHONE.findAll(rest).forEach { m ->
                    val digits = m.value.count(Char::isDigit)
                    if (digits in 7..15) phones += m.value.trim()
                }
            }
            val hadPhone = PHONE.findAll(rest).any { it.value.count(Char::isDigit) in 7..15 }
            if (hadPhone) {
                rest = PHONE.replace(rest) { m ->
                    if (m.value.count(Char::isDigit) in 7..15) " " else m.value
                }
                rest = PHONE_LABEL.replace(rest, "")
                    .replace(Regex("""(?i)\b(fax|tel|mob|mobile|phone|cell|office|direct)\b\s*[:.]?"""), " ")
            }
            rest = rest.replace(Regex("""(?i)^\s*(e-?mail|email|web|website|w|e)\s*[:.]"""), "")
                .replace(Regex("""^[\s|•·,:;/\-]+|[\s|•·,:;/\-]+$"""), "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (rest.length >= 2 && rest.any(Char::isLetter)) {
                leftovers += line.copy(text = rest)
            }
        }

        val used = mutableSetOf<OcrLine>()

        // Address: lines that look like streets, cities or postcodes. Adjacent ones are merged.
        val addressLines = leftovers.filter { isAddressLine(it.text) }
        used += addressLines
        val address = addressLines.joinToString(", ") { it.text }

        val titleLine = leftovers.firstOrNull { it !in used && isTitleLine(it.text) }
        titleLine?.let { used += it }

        val emailDomain = emails.firstNotNullOfOrNull { companyDomain(it) }
            ?: companyDomain(website)

        var companyLine = leftovers.firstOrNull { it !in used && isCompanyLine(it.text) }
        if (companyLine == null && emailDomain != null) {
            companyLine = leftovers.firstOrNull {
                it !in used && it.text.lowercase().replace(" ", "").contains(emailDomain)
            }
        }
        companyLine?.let { used += it }

        val emailName = emails.firstOrNull()?.substringBefore('@')
            ?.split('.', '_', '-')
            ?.filter { it.length > 1 && it.all(Char::isLetter) }
            .orEmpty()

        val nameLine = leftovers
            .filter { it !in used && looksLikeName(it.text) }
            .maxByOrNull { nameScore(it, emailName, clean) }
            ?: leftovers.firstOrNull { it !in used && it.text.split(' ').size <= 4 && it.text.none(Char::isDigit) }
        nameLine?.let { used += it }

        val company = companyLine?.text
            ?: emailDomain?.replaceFirstChar { it.uppercase() }
            ?: ""

        return ParsedCard(
            name = nameLine?.text?.let(::tidyName).orEmpty(),
            jobTitle = titleLine?.text.orEmpty(),
            company = company,
            phones = phones.distinctBy { p -> p.filter(Char::isDigit) },
            emails = emails.distinct(),
            website = website,
            address = address,
            rawText = clean.joinToString("\n") { it.text },
        )
    }

    private fun words(text: String) =
        text.lowercase().split(Regex("""[\s,|/&]+""")).filter { it.isNotEmpty() }

    private fun isAddressLine(text: String): Boolean {
        val w = words(text).map { it.trimEnd('.', ',') }
        val hasAddressWord = w.any { it in ADDRESS_WORDS }
        val startsWithNumber = Regex("""^(#\s?)?\d+[A-Za-z]?[,\s]""").containsMatchIn(text)
        val hasZip = ZIP.containsMatchIn(text)
        return (hasAddressWord && (startsWithNumber || hasZip || text.any(Char::isDigit))) ||
            (startsWithNumber && w.size >= 3) ||
            (hasZip && w.size >= 2 && text.count(Char::isLetter) >= 3)
    }

    private fun isTitleLine(text: String): Boolean {
        val w = words(text)
        return w.size <= 7 && w.any { it in TITLE_WORDS }
    }

    private fun isCompanyLine(text: String): Boolean {
        val w = words(text).map { it.trimEnd(',') }
        return w.any { it in COMPANY_WORDS || it.trimEnd('.') in COMPANY_WORDS }
    }

    private fun looksLikeName(text: String): Boolean {
        val parts = text.split(' ').filter { it.isNotEmpty() }
        if (parts.size !in 1..4) return false
        if (text.any(Char::isDigit)) return false
        if (parts.size == 1 && parts[0].length < 3) return false
        return parts.all { p -> p.all { it.isLetter() || it in ".'-" } && p.first().isLetter() }
    }

    private fun nameScore(line: OcrLine, emailName: List<String>, all: List<OcrLine>): Double {
        val parts = line.text.split(' ')
        var score = 0.0
        val maxHeight = all.maxOfOrNull { it.height } ?: 0
        if (maxHeight > 0) score += 3.0 * line.height / maxHeight
        if (parts.size in 2..3) score += 2.0
        if (parts.all { it.first().isUpperCase() }) score += 1.0
        val lower = line.text.lowercase()
        score += 3.0 * emailName.count { lower.contains(it) }
        // Earlier lines are slightly more likely to be the name.
        score -= 0.05 * all.indexOfFirst { it.text.contains(line.text) }.coerceAtLeast(0)
        return score
    }

    /** "JOHN SMITH" -> "John Smith"; mixed-case names are left alone. */
    private fun tidyName(name: String): String =
        if (name == name.uppercase() && name.any(Char::isLetter)) {
            name.lowercase().split(' ').joinToString(" ") { part ->
                part.split('-').joinToString("-") { it.replaceFirstChar(Char::uppercase) }
            }
        } else name

    /** Returns the organisation part of an email or site ("acme" from jane@mail.acme.com). */
    private fun companyDomain(value: String): String? {
        if (value.isBlank()) return null
        val host = value.substringAfter('@').lowercase()
            .removePrefix("https://").removePrefix("http://").removePrefix("www.")
            .substringBefore('/')
        val labels = host.split('.').filter { it.isNotEmpty() }
        if (labels.size < 2) return null
        val secondLevel = setOf("co", "com", "org", "net", "ac", "gov")
        val name = if (labels.size >= 3 && labels[labels.size - 2] in secondLevel) {
            labels[labels.size - 3]
        } else {
            labels[labels.size - 2]
        }
        return name.takeIf { it !in FREE_MAIL_DOMAINS && it.length > 1 }
    }
}
