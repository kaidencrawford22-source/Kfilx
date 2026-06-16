package com.streamflixreborn.streamflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.streamflixreborn.streamflix.models.WatchRecord

@Dao
interface WatchRecordDao {

    @Insert
    suspend fun insert(record: WatchRecord)

    @Query("SELECT COUNT(*) FROM watch_records WHERE type = 'movie'")
    suspend fun getTotalMoviesWatched(): Int

    @Query("SELECT COUNT(*) FROM watch_records WHERE type = 'episode'")
    suspend fun getTotalEpisodesWatched(): Int

    @Query("SELECT COUNT(DISTINCT showTitle) FROM watch_records WHERE type = 'episode' AND showTitle IS NOT NULL")
    suspend fun getTotalShowsWatched(): Int

    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM watch_records")
    suspend fun getTotalWatchedMs(): Long

    @Query("SELECT title FROM watch_records WHERE type = 'movie' GROUP BY contentId ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getMostWatchedMovieTitle(): String?

    @Query("SELECT showTitle FROM watch_records WHERE type = 'episode' AND showTitle IS NOT NULL GROUP BY showTitle ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getMostWatchedShowTitle(): String?

    @Query("SELECT * FROM watch_records ORDER BY watchedDateMillis DESC")
    suspend fun getAllRecords(): List<WatchRecord>

    @Query("SELECT * FROM watch_records ORDER BY watchedDateMillis DESC")
    fun getAllRecordsSync(): List<WatchRecord>
}
