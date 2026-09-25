package com.cardscanner.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val website: String = "",
    val address: String = "",
    val notes: String = "",
    val imagePath: String? = null,
    val rawText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val displayName: String
        get() = name.ifBlank { company.ifBlank { emails.firstOrNull() ?: "Unnamed card" } }
}
