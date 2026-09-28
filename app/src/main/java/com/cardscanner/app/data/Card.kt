package com.cardscanner.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A named batch of scanned cards, e.g. one trade show, exported to Excel together. */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 0,
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
)

/** Company first, as the card list and detail headline. */
val Card.headline: String
    get() = company.ifBlank { name.ifBlank { emails.firstOrNull() ?: "Unnamed card" } }

/** Kept outside the entity so Room doesn't treat it as a column. */
val Card.displayName: String
    get() = name.ifBlank { company.ifBlank { emails.firstOrNull() ?: "Unnamed card" } }
