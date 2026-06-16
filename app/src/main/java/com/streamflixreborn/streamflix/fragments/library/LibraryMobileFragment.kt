package com.streamflixreborn.streamflix.fragments.library

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.streamflixreborn.streamflix.R
import com.streamflixreborn.streamflix.adapters.LibraryAdapter
import com.streamflixreborn.streamflix.database.LibraryDatabase
import com.streamflixreborn.streamflix.databinding.FragmentLibraryMobileBinding
import com.streamflixreborn.streamflix.databinding.ItemAddPlaylistBinding
import com.streamflixreborn.streamflix.databinding.ItemPlaylistChipBinding
import com.streamflixreborn.streamflix.models.LibraryGridItem
import com.streamflixreborn.streamflix.models.Playlist
import com.streamflixreborn.streamflix.models.Video
import com.streamflixreborn.streamflix.utils.dp
import com.streamflixreborn.streamflix.utils.viewModelsFactory
import com.streamflixreborn.streamflix.ui.SpacingItemDecoration
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class LibraryMobileFragment : Fragment() {

    private var _binding: FragmentLibraryMobileBinding? = null
    private val binding get() = _binding!!

    private val database by lazy { LibraryDatabase.getInstance(requireContext()) }
    private val viewModel by viewModelsFactory { LibraryViewModel(database) }

    private lateinit var libraryAdapter: LibraryAdapter
    private var playlistChipsAdapter: PlaylistChipsAdapter? = null
    private var carouselSnapHelper: LinearSnapHelper? = null

    private val localFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val mimeType = requireContext().contentResolver.getType(uri) ?: "video/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Play with"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGrid()
        setupPlaylistRow()
        setupLocalFileButton()
        setupSearchAndSort()
        setupObservers()
    }

    private fun setupGrid() {
        libraryAdapter = LibraryAdapter(
            onItemClick = { item ->
                when (item.type) {
                    "movie" -> {
                        val bundle = Bundle().apply { putString("id", item.contentId) }
                        findNavController().navigate(R.id.movie, bundle)
                    }
                    "tv_show" -> {
                        val bundle = Bundle().apply { putString("id", item.contentId) }
                        findNavController().navigate(R.id.tv_show, bundle)
                    }
                }
            },
            onItemLongClick = { item -> showItemLongPressDialog(item) }
        )

        binding.rvLibrary.apply {
            adapter = libraryAdapter
            addItemDecoration(SpacingItemDecoration(10.dp(requireContext())))
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (libraryAdapter.carouselMode) return false
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from < 0 || to < 0) return false
                val selId = viewModel.selectedPlaylistId.value
                if (selId != null && from >= 0 && to >= 0) {
                    val item = libraryAdapter.items.getOrNull(from) ?: return false
                    val playlistItemId = item.playlistItemId ?: return false
                    viewModel.moveItem(playlistItemId, to)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled() = !libraryAdapter.carouselMode && viewModel.selectedPlaylistId.value == null
        })
        touchHelper.attachToRecyclerView(binding.rvLibrary)
    }

    private fun updateLayoutForSelection(selId: Long?) {
        carouselSnapHelper?.attachToRecyclerView(null)
        carouselSnapHelper = null

        val isPlaylistMode = selId != null
        binding.etLibrarySearch.visibility = if (isPlaylistMode) View.GONE else View.VISIBLE
        binding.llFilterChips.visibility = if (isPlaylistMode) View.GONE else View.VISIBLE
        binding.llSortRow.visibility = if (isPlaylistMode) View.GONE else View.VISIBLE

        if (isPlaylistMode) {
            libraryAdapter.carouselMode = true
            binding.rvLibrary.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.rvLibrary.adapter = libraryAdapter
            binding.rvLibrary.clipToPadding = false
            val peek = 60.dp(requireContext())
            val vPad = 24.dp(requireContext())
            binding.rvLibrary.setPadding(peek, vPad, peek, vPad)
            LinearSnapHelper().also {
                it.attachToRecyclerView(binding.rvLibrary)
                carouselSnapHelper = it
            }
        } else {
            libraryAdapter.carouselMode = false
            binding.rvLibrary.layoutManager = GridLayoutManager(requireContext(), 3)
            binding.rvLibrary.adapter = libraryAdapter
            binding.rvLibrary.clipToPadding = true
            val pad = 10.dp(requireContext())
            binding.rvLibrary.setPadding(pad, pad, pad, pad)
        }
    }

    private fun showItemLongPressDialog(item: LibraryGridItem) {
        val selId = viewModel.selectedPlaylistId.value
        if (selId != null) {
            AlertDialog.Builder(requireContext())
                .setTitle(item.title)
                .setItems(arrayOf("Remove from Playlist")) { _, which ->
                    if (which == 0) {
                        viewModel.removeFromPlaylist(selId, item.contentId)
                        Toast.makeText(requireContext(), "Removed from playlist", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            showAddToPlaylistDialog(item)
        }
    }

    private fun setupLocalFileButton() {
        binding.btnLocalFile.setOnClickListener {
            localFileLauncher.launch(arrayOf("video/*", "application/x-mpegURL", "audio/*"))
        }
    }

    private fun setupSearchAndSort() {
        binding.etLibrarySearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnFilterAll.setOnClickListener {
            viewModel.setTypeFilter(null)
            updateFilterChips(null)
        }
        binding.btnFilterMovies.setOnClickListener {
            viewModel.setTypeFilter("movie")
            updateFilterChips("movie")
        }
        binding.btnFilterTvshows.setOnClickListener {
            viewModel.setTypeFilter("tv_show")
            updateFilterChips("tv_show")
        }

        binding.btnSortNewest.setOnClickListener {
            viewModel.setSortOrder(SortOrder.NEWEST)
            updateSortButtons(SortOrder.NEWEST)
        }
        binding.btnSortTitle.setOnClickListener {
            viewModel.setSortOrder(SortOrder.TITLE)
            updateSortButtons(SortOrder.TITLE)
        }
        binding.btnSortRating.setOnClickListener {
            viewModel.setSortOrder(SortOrder.RATING)
            updateSortButtons(SortOrder.RATING)
        }
        binding.btnSortYear.setOnClickListener {
            viewModel.setSortOrder(SortOrder.YEAR)
            updateSortButtons(SortOrder.YEAR)
        }
    }

    private fun updateFilterChips(selected: String?) {
        val accentColor = run {
            val tv = TypedValue()
            requireContext().theme.resolveAttribute(R.attr.y2k_accent_color, tv, true)
            tv.data
        }
        val defaultColor = 0xFFFFB6C1.toInt()
        binding.btnFilterAll.setTextColor(if (selected == null) accentColor else defaultColor)
        binding.btnFilterMovies.setTextColor(if (selected == "movie") accentColor else defaultColor)
        binding.btnFilterTvshows.setTextColor(if (selected == "tv_show") accentColor else defaultColor)
    }

    private fun updateSortButtons(selected: SortOrder) {
        val accentColor = run {
            val tv = TypedValue()
            requireContext().theme.resolveAttribute(R.attr.y2k_accent_color, tv, true)
            tv.data
        }
        val defaultColor = 0xFFFFB6C1.toInt()
        binding.btnSortNewest.setTextColor(if (selected == SortOrder.NEWEST) accentColor else defaultColor)
        binding.btnSortTitle.setTextColor(if (selected == SortOrder.TITLE) accentColor else defaultColor)
        binding.btnSortRating.setTextColor(if (selected == SortOrder.RATING) accentColor else defaultColor)
        binding.btnSortYear.setTextColor(if (selected == SortOrder.YEAR) accentColor else defaultColor)
    }

    private fun setupPlaylistRow() {
        val chipsAdapter = PlaylistChipsAdapter(
            onClick = { playlist -> viewModel.selectPlaylist(playlist.id) },
            onLongClick = { playlist -> showPlaylistDialog(playlist) },
            onAddClick = { showCreatePlaylistDialog() }
        )
        playlistChipsAdapter = chipsAdapter
        binding.rvPlaylists.adapter = chipsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.playlists,
                viewModel.selectedPlaylistId
            ) { playlists, selId ->
                Pair(playlists, selId)
            }.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
             .collect { (playlists, selId) ->
                 chipsAdapter.submitList(playlists, selId)
             }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedPlaylistId
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .distinctUntilChanged()
                .collect { selId ->
                    updateLayoutForSelection(selId)
                    if (selId == null) {
                        binding.rvLibrary.itemAnimator?.changeDuration = 0
                        libraryAdapter.submitList(viewModel.gridItems.value)
                    }
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.gridItems
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { items ->
                    if (viewModel.selectedPlaylistId.value == null) {
                        libraryAdapter.submitList(items)
                        updateEmptyState(items.isEmpty())
                    }
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.playlistGridItems
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { items ->
                    if (viewModel.selectedPlaylistId.value != null) {
                        libraryAdapter.submitList(items)
                        updateEmptyState(items.isEmpty())
                    }
                }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedPlaylistId
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { selId ->
                    binding.rvPlaylists.post {
                        (binding.rvPlaylists.adapter as? PlaylistChipsAdapter)?.notifyDataSetChanged()
                    }
                }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvLibraryEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvLibrary.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showCreatePlaylistDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            setHint("Playlist name")
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0x88FFFFFF.toInt())
        }
        AlertDialog.Builder(requireContext())
            .setTitle("New Playlist")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.createPlaylist(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPlaylistDialog(playlist: Playlist) {
        val items = arrayOf("Rename", "Delete")
        AlertDialog.Builder(requireContext())
            .setTitle(playlist.name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showRenameDialog(playlist)
                    1 -> confirmDeletePlaylist(playlist)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(playlist: Playlist) {
        val input = android.widget.EditText(requireContext()).apply {
            setText(playlist.name)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0x88FFFFFF.toInt())
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Rename Playlist")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.renamePlaylist(playlist.id, name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeletePlaylist(playlist: Playlist) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Playlist")
            .setMessage("Delete \"${playlist.name}\"?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deletePlaylist(playlist) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddToPlaylistDialog(item: LibraryGridItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val playlists = viewModel.playlists.value
            if (playlists.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("No Playlists")
                    .setMessage("Create a playlist first to add items.")
                    .setPositiveButton("Create") { _, _ -> showCreatePlaylistDialog() }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@launch
            }
            val names = playlists.map { it.name }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Add to Playlist")
                .setItems(names) { _, which ->
                    val playlist = playlists[which]
                    viewModel.addToPlaylist(
                        playlistId = playlist.id,
                        contentId = item.contentId,
                        title = item.title,
                        poster = item.poster,
                        type = item.type
                    )
                    Toast.makeText(requireContext(), "Added to \"${playlist.name}\"", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class PlaylistChipsAdapter(
    private val onClick: (Playlist) -> Unit,
    private val onLongClick: (Playlist) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var playlists: List<Playlist> = emptyList()
    private var selectedId: Long? = null

    companion object {
        private const val TYPE_PLAYLIST = 0
        private const val TYPE_ADD = 1
    }

    fun submitList(list: List<Playlist>, selId: Long?) {
        playlists = list
        selectedId = selId
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == playlists.size) TYPE_ADD else TYPE_PLAYLIST
    }

    override fun getItemCount() = playlists.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ADD -> {
                val b = ItemAddPlaylistBinding.inflate(inflater, parent, false)
                AddViewHolder(b)
            }
            else -> {
                val b = ItemPlaylistChipBinding.inflate(inflater, parent, false)
                ChipViewHolder(b)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChipViewHolder -> {
                val playlist = playlists[position]
                val isSelected = playlist.id == selectedId
                holder.bind(playlist, isSelected)
                holder.itemView.setOnClickListener { onClick(playlist) }
                holder.itemView.setOnLongClickListener {
                    onLongClick(playlist)
                    true
                }
            }
            is AddViewHolder -> {
                holder.itemView.setOnClickListener { onAddClick() }
            }
        }
    }

    class ChipViewHolder(private val binding: ItemPlaylistChipBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: Playlist, isSelected: Boolean) {
            binding.tvChipName.text = playlist.name
            binding.root.setCardBackgroundColor(
                if (isSelected) {
                    val tv = TypedValue()
                    itemView.context.theme.resolveAttribute(R.attr.y2k_accent_color, tv, true)
                    tv.data
                } else 0xFF2A2A2A.toInt()
            )
        }
    }

    class AddViewHolder(binding: ItemAddPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root)
}
