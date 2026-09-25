package com.cardscanner.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun observe(id: Long): Flow<Card?>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun get(id: Long): Card?

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    suspend fun getAll(): List<Card>

    @Upsert
    suspend fun upsert(card: Card): Long

    @Delete
    suspend fun delete(card: Card)
}
