package com.cardscanner.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/** Card photos live in app-private storage under files/cards. */
object ImageStore {
    private fun dir(context: Context) = File(context.filesDir, "cards").apply { mkdirs() }

    fun newImageFile(context: Context): File = File(dir(context), "${UUID.randomUUID()}.jpg")

    /** Copies a picked gallery image into app storage so it survives the source being deleted. */
    fun importFromUri(context: Context, uri: Uri): File {
        val file = newImageFile(context)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open $uri" }
            file.outputStream().use { input.copyTo(it) }
        }
        return file
    }
}
