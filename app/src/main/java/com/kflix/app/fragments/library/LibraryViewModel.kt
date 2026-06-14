package com.kflix.app.fragments.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kflix.app.database.LibraryDatabase
import com.kflix.app.models.LibraryGridItem
import com.kflix.app.models.LibraryItem
import com.kflix.app.models.Playlist
import com.kflix.app.models.PlaylistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    database: LibraryDatabase
) : ViewModel() {

    private val libraryItemDao = database.libraryItemDao()
    private val playlistDao = database.playlistDao()
    private val playlistItemDao = database.playlistItemDao()

    val playlists: StateFlow<List<Playlist>> = playlistDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allLibraryItems: StateFlow<List<LibraryGridItem>> = libraryItemDao.getAll()
        .map { items -> items.map { it.toGridItem() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    val gridItems: StateFlow<List<LibraryGridItem>> = combine(
        allLibraryItems,
        _selectedPlaylistId
    ) { allItems, selId ->
        if (selId == null) allItems else emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlistItems: StateFlow<List<PlaylistItem>> = combine(
        playlistItemDao.getAllFlow(),
        _selectedPlaylistId
    ) { allItems, selId ->
        if (selId == null) emptyList()
        else allItems.filter { it.playlistId == selId }.sortedBy { it.position }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlistGridItems: StateFlow<List<LibraryGridItem>> = playlistItems
        .map { items -> items.map { it.toGridItem() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectPlaylist(id: Long?) {
        _selectedPlaylistId.value = id
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistDao.insert(Playlist(name = name))
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch {
            val existing = playlistDao.getById(id) ?: return@launch
            playlistDao.update(existing.copy(name = name))
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistDao.delete(playlist)
            if (_selectedPlaylistId.value == playlist.id) {
                _selectedPlaylistId.value = null
            }
        }
    }

    fun addToPlaylist(playlistId: Long, contentId: String, title: String, poster: String?, type: String) {
        viewModelScope.launch {
            val existing = playlistItemDao.getByContentId(playlistId, contentId)
            if (existing != null) return@launch
            val nextPos = playlistItemDao.getNextPosition(playlistId)
            playlistItemDao.insert(
                PlaylistItem(
                    playlistId = playlistId,
                    contentId = contentId,
                    title = title,
                    poster = poster,
                    type = type,
                    position = nextPos
                )
            )
        }
    }

    fun removeFromPlaylist(playlistId: Long, contentId: String) {
        viewModelScope.launch {
            playlistItemDao.deleteByContentId(playlistId, contentId)
        }
    }

    fun moveItem(itemId: Long, newPosition: Int) {
        viewModelScope.launch {
            playlistItemDao.updatePosition(itemId, newPosition)
        }
    }
}

private fun LibraryItem.toGridItem() = LibraryGridItem(
    contentId = contentId,
    title = title,
    poster = poster,
    type = type
)

private fun PlaylistItem.toGridItem() = LibraryGridItem(
    contentId = contentId,
    title = title,
    poster = poster,
    type = type,
    playlistItemId = id
)
