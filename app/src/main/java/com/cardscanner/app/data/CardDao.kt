package com.cardscanner.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class ProjectWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val cardCount: Int,
)

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeInProject(projectId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun observe(id: Long): Flow<Card?>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun get(id: Long): Card?

    @Query("SELECT * FROM cards WHERE projectId = :projectId ORDER BY createdAt DESC")
    suspend fun getInProject(projectId: Long): List<Card>

    @Upsert
    suspend fun upsert(card: Card): Long

    @Delete
    suspend fun delete(card: Card)

    @Query(
        """SELECT p.id, p.name, p.createdAt, COUNT(c.id) AS cardCount
           FROM projects p LEFT JOIN cards c ON c.projectId = p.id
           GROUP BY p.id ORDER BY p.createdAt DESC"""
    )
    fun observeProjects(): Flow<List<ProjectWithCount>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProject(id: Long): Flow<Project?>

    @Insert
    suspend fun insertProject(project: Project): Long

    @Query("UPDATE projects SET name = :name WHERE id = :id")
    suspend fun renameProject(id: Long, name: String)

    @Query("DELETE FROM cards WHERE projectId = :projectId")
    suspend fun deleteCardsInProject(projectId: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)
}
