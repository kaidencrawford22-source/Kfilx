package com.kflix.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_records")
data class WatchRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentId: String,
    val title: String,
    val poster: String?,
    val type: String, // "movie" or "episode"
    val providerName: String,
    val profileId: String,
    val watchedDateMillis: Long,
    val durationMillis: Long,
    val showTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)
