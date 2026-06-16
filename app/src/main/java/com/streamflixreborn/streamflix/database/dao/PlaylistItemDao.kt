package com.streamflixreborn.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.streamflixreborn.streamflix.models.PlaylistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistItemDao {

    @Query("SELECT * FROM playlist_items ORDER BY playlistId, position ASC")
    fun getAllFlow(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items ORDER BY playlistId, position ASC")
    fun getAllSync(): List<PlaylistItem>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getByPlaylistId(playlistId: Long): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getByPlaylistIdOnce(playlistId: Long): List<PlaylistItem>

    @Insert
    suspend fun insert(item: PlaylistItem)

    @Delete
    suspend fun delete(item: PlaylistItem)

    @Query("UPDATE playlist_items SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getNextPosition(playlistId: Long): Int

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteAllForPlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_items WHERE contentId = :contentId AND playlistId = :playlistId")
    suspend fun deleteByContentId(playlistId: Long, contentId: String)

    @Query("SELECT * FROM playlist_items WHERE contentId = :contentId AND playlistId = :playlistId")
    suspend fun getByContentId(playlistId: Long, contentId: String): PlaylistItem?
}
