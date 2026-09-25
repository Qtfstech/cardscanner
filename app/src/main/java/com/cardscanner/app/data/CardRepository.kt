package com.cardscanner.app.data

import java.io.File

class CardRepository(private val dao: CardDao) {
    fun observeAll() = dao.observeAll()
    fun observe(id: Long) = dao.observe(id)
    suspend fun get(id: Long) = dao.get(id)
    suspend fun getAll() = dao.getAll()

    /** Inserts or updates [card] and returns its id. */
    suspend fun save(card: Card): Long {
        val result = dao.upsert(card)
        // Upsert returns -1 when it updated an existing row.
        return if (result == -1L) card.id else result
    }

    suspend fun delete(card: Card) {
        dao.delete(card)
        card.imagePath?.let { File(it).delete() }
    }
}
