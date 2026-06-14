package com.kflix.app.models

data class LibraryGridItem(
    val contentId: String,
    val title: String,
    val poster: String?,
    val type: String,
    val playlistItemId: Long? = null
)
