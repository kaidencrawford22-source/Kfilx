package com.kflix.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryItem(
    @PrimaryKey
    val contentId: String,
    val title: String,
    val type: String,
    val poster: String? = null,
    val banner: String? = null,
    val overview: String? = null,
    val rating: Double? = null,
    val year: String? = null,
    val profileId: String? = null,
    val addedAtMillis: Long = System.currentTimeMillis()
)
