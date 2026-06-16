package com.streamflixreborn.streamflix.models

data class LibraryGridItem(
    val contentId: String,
    val title: String,
    val poster: String?,
    val type: String,
    val rating: Double? = null,
    val year: String? = null,
    val addedAtMillis: Long = 0L,
    val playlistItemId: Long? = null
)
