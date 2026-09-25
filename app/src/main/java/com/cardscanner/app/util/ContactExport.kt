package com.cardscanner.app.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.content.FileProvider
import com.cardscanner.app.data.Card
import com.cardscanner.app.data.displayName
import java.io.File

object ContactExport {

    /** Opens the phone's Contacts app with the card pre-filled; the user confirms the save there. */
    fun addToContacts(context: Context, card: Card) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, card.name)
            putExtra(ContactsContract.Intents.Insert.COMPANY, card.company)
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE, card.jobTitle)
            putExtra(ContactsContract.Intents.Insert.POSTAL, card.address)
            putExtra(ContactsContract.Intents.Insert.NOTES, card.notes)

            val phoneKeys = listOf(
                ContactsContract.Intents.Insert.PHONE,
                ContactsContract.Intents.Insert.SECONDARY_PHONE,
                ContactsContract.Intents.Insert.TERTIARY_PHONE,
            )
            card.phones.zip(phoneKeys).forEach { (phone, key) -> putExtra(key, phone) }

            val emailKeys = listOf(
                ContactsContract.Intents.Insert.EMAIL,
                ContactsContract.Intents.Insert.SECONDARY_EMAIL,
                ContactsContract.Intents.Insert.TERTIARY_EMAIL,
            )
            card.emails.zip(emailKeys).forEach { (email, key) -> putExtra(key, email) }

            if (card.website.isNotBlank()) {
                val web = ContentValues().apply {
                    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.Website.URL, card.website)
                    put(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_WORK)
                }
                putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, arrayListOf(web))
            }
        }
        start(context, intent)
    }

    fun toVCard(card: Card): String = buildString {
        fun esc(s: String) = s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        val parts = card.name.trim().split(' ')
        val last = if (parts.size > 1) parts.last() else ""
        val first = if (parts.size > 1) parts.dropLast(1).joinToString(" ") else card.name.trim()
        appendLine("N:${esc(last)};${esc(first)};;;")
        appendLine("FN:${esc(card.name.ifBlank { card.displayName })}")
        if (card.company.isNotBlank()) appendLine("ORG:${esc(card.company)}")
        if (card.jobTitle.isNotBlank()) appendLine("TITLE:${esc(card.jobTitle)}")
        card.phones.forEach { appendLine("TEL;TYPE=WORK,VOICE:${esc(it)}") }
        card.emails.forEach { appendLine("EMAIL;TYPE=INTERNET,WORK:${esc(it)}") }
        if (card.website.isNotBlank()) appendLine("URL:${esc(card.website)}")
        if (card.address.isNotBlank()) appendLine("ADR;TYPE=WORK:;;${esc(card.address)};;;;")
        if (card.notes.isNotBlank()) appendLine("NOTE:${esc(card.notes)}")
        appendLine("END:VCARD")
    }

    fun toCsv(cards: List<Card>): String = buildString {
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        appendLine(listOf("Name", "Title", "Company", "Phones", "Emails", "Website", "Address", "Notes").joinToString(","))
        cards.forEach { c ->
            appendLine(
                listOf(c.name, c.jobTitle, c.company, c.phones.joinToString("; "), c.emails.joinToString("; "),
                    c.website, c.address, c.notes).joinToString(",") { q(it) }
            )
        }
    }

    fun shareVCard(context: Context, cards: List<Card>) {
        val name = if (cards.size == 1) safeFileName(cards[0].displayName) else "cards"
        shareFile(context, "$name.vcf", cards.joinToString("") { toVCard(it) }, "text/x-vcard")
    }

    fun shareCsv(context: Context, cards: List<Card>) =
        shareFile(context, "cards.csv", toCsv(cards), "text/csv")

    private fun shareFile(context: Context, fileName: String, content: String, mime: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(context, Intent.createChooser(send, "Share"))
    }

    private fun safeFileName(s: String) = s.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(40).ifBlank { "card" }

    fun dial(context: Context, phone: String) =
        start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))))

    fun email(context: Context, address: String) =
        start(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")))

    fun openWebsite(context: Context, site: String) {
        val url = if (site.startsWith("http", ignoreCase = true)) site else "https://$site"
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun openMap(context: Context, address: String) =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address))))

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to handle this", Toast.LENGTH_SHORT).show()
        }
    }
}
