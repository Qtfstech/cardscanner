package com.cardscanner.app.data

import java.io.File

class CardRepository(private val dao: CardDao) {
    fun observeProjects() = dao.observeProjects()
    fun observeProject(id: Long) = dao.observeProject(id)
    fun observeCards(projectId: Long) = dao.observeInProject(projectId)
    fun observe(id: Long) = dao.observe(id)
    suspend fun get(id: Long) = dao.get(id)
    suspend fun getCards(projectId: Long) = dao.getInProject(projectId)

    suspend fun createProject(name: String): Long = dao.insertProject(Project(name = name.trim()))

    suspend fun renameProject(id: Long, name: String) = dao.renameProject(id, name.trim())

    /** Deletes the project, its cards and their photos. */
    suspend fun deleteProject(id: Long) {
        val photos = dao.getInProject(id).mapNotNull { it.imagePath }
        dao.deleteCardsInProject(id)
        dao.deleteProject(id)
        photos.forEach { File(it).delete() }
    }

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
