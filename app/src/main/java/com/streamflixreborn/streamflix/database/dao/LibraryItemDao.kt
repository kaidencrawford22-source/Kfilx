package com.streamflixreborn.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.streamflixreborn.streamflix.models.LibraryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryItemDao {

    @Query("SELECT * FROM library_items ORDER BY addedAtMillis DESC")
    fun getAll(): Flow<List<LibraryItem>>

    @Query("SELECT * FROM library_items ORDER BY addedAtMillis DESC")
    fun getAllSync(): List<LibraryItem>

    @Query("SELECT * FROM library_items WHERE contentId = :contentId")
    suspend fun getByContentId(contentId: String): LibraryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LibraryItem)

    @Query("DELETE FROM library_items WHERE contentId = :contentId")
    suspend fun delete(contentId: String)

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}
