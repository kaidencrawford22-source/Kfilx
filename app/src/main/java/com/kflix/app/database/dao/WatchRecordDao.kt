package com.kflix.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kflix.app.models.WatchRecord

@Dao
interface WatchRecordDao {

    @Insert
    suspend fun insert(record: WatchRecord)

    @Query("SELECT COUNT(*) FROM watch_records WHERE type = 'movie' AND profileId = :profileId")
    suspend fun getTotalMoviesWatched(profileId: String): Int

    @Query("SELECT COUNT(*) FROM watch_records WHERE type = 'episode' AND profileId = :profileId")
    suspend fun getTotalEpisodesWatched(profileId: String): Int

    @Query("SELECT COUNT(DISTINCT showTitle) FROM watch_records WHERE type = 'episode' AND showTitle IS NOT NULL AND profileId = :profileId")
    suspend fun getTotalShowsWatched(profileId: String): Int

    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM watch_records WHERE profileId = :profileId")
    suspend fun getTotalWatchedMs(profileId: String): Long

    @Query("SELECT title FROM watch_records WHERE type = 'movie' AND profileId = :profileId GROUP BY contentId ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getMostWatchedMovieTitle(profileId: String): String?

    @Query("SELECT showTitle FROM watch_records WHERE type = 'episode' AND showTitle IS NOT NULL AND profileId = :profileId GROUP BY showTitle ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getMostWatchedShowTitle(profileId: String): String?

    @Query("SELECT * FROM watch_records WHERE profileId = :profileId ORDER BY watchedDateMillis DESC")
    suspend fun getAllRecords(profileId: String): List<WatchRecord>

    @Query("SELECT * FROM watch_records WHERE profileId = :profileId ORDER BY watchedDateMillis DESC")
    fun getAllRecordsSync(profileId: String): List<WatchRecord>

    @Query("SELECT * FROM watch_records ORDER BY watchedDateMillis DESC")
    fun getAllRecordsSync(): List<WatchRecord>
}
