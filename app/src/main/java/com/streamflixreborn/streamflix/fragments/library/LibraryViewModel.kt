package com.streamflixreborn.streamflix.fragments.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamflixreborn.streamflix.database.LibraryDatabase
import com.streamflixreborn.streamflix.models.LibraryGridItem
import com.streamflixreborn.streamflix.models.LibraryItem
import com.streamflixreborn.streamflix.models.Playlist
import com.streamflixreborn.streamflix.models.PlaylistItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { NEWEST, TITLE, RATING, YEAR }

class LibraryViewModel(
    database: LibraryDatabase
) : ViewModel() {

    private val libraryItemDao = database.libraryItemDao()
    private val playlistDao = database.playlistDao()
    private val playlistItemDao = database.playlistItemDao()

    val playlists: StateFlow<List<Playlist>> = playlistDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    private val _typeFilter = MutableStateFlow<String?>(null)

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }
    fun setTypeFilter(type: String?) { _typeFilter.value = type }
    fun clearFilters() {
        _searchQuery.value = ""
        _sortOrder.value = SortOrder.NEWEST
        _typeFilter.value = null
    }

    val allLibraryItems: StateFlow<List<LibraryGridItem>> = libraryItemDao.getAll()
        .map { items -> items.map { it.toGridItem() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    val filteredLibraryItems: StateFlow<List<LibraryGridItem>> = combine(
        allLibraryItems, _searchQuery, _sortOrder, _typeFilter
    ) { items, query, sort, type ->
        items
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .filter { type == null || it.type == type }
            .sortedWith(when (sort) {
                SortOrder.NEWEST -> compareByDescending<LibraryGridItem> { it.addedAtMillis }
                SortOrder.TITLE -> compareBy { it.title }
                SortOrder.RATING -> compareByDescending<LibraryGridItem> { it.rating ?: 0.0 }
                SortOrder.YEAR -> compareByDescending<LibraryGridItem> { it.year ?: "" }
            })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val gridItems: StateFlow<List<LibraryGridItem>> = combine(
        filteredLibraryItems,
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
    type = type,
    rating = rating,
    year = year,
    addedAtMillis = addedAtMillis
)

private fun PlaylistItem.toGridItem() = LibraryGridItem(
    contentId = contentId,
    title = title,
    poster = poster,
    type = type,
    addedAtMillis = 0L,
    playlistItemId = id
)
