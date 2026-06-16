package com.streamflixreborn.streamflix.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.streamflixreborn.streamflix.R
import com.streamflixreborn.streamflix.databinding.ItemLibraryCarouselBinding
import com.streamflixreborn.streamflix.databinding.ItemLibraryDvdBinding
import com.streamflixreborn.streamflix.models.LibraryGridItem
import com.streamflixreborn.streamflix.service.VideoDownloadService

class LibraryAdapter(
    var items: List<LibraryGridItem> = emptyList(),
    private val onItemClick: (LibraryGridItem) -> Unit,
    private val onItemLongClick: ((LibraryGridItem) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onDragStarted: ((RecyclerView.ViewHolder) -> Unit)? = null
    var carouselMode: Boolean = false

    companion object {
        private const val TYPE_GRID = 0
        private const val TYPE_CAROUSEL = 1
    }

    fun submitList(newItems: List<LibraryGridItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (carouselMode) TYPE_CAROUSEL else TYPE_GRID
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_CAROUSEL) {
            val binding = ItemLibraryCarouselBinding.inflate(inflater, parent, false)
            CarouselViewHolder(binding, onItemClick)
        } else {
            val binding = ItemLibraryDvdBinding.inflate(inflater, parent, false)
            LibraryViewHolder(binding, onItemClick, onItemLongClick, onDragStarted)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CarouselViewHolder -> holder.bind(items[position])
            is LibraryViewHolder -> holder.bind(items[position])
        }
    }

    class CarouselViewHolder(
        private val binding: ItemLibraryCarouselBinding,
        private val onItemClick: (LibraryGridItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LibraryGridItem) {
            Glide.with(binding.root.context)
                .load(item.poster)
                .placeholder(R.drawable.bg_y2k_pill_hotpink)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivCarouselPoster)

            binding.tvCarouselTitle.text = item.title
            binding.root.setOnClickListener { onItemClick(item) }

            val isDownloading = VideoDownloadService.isDownloading(item.contentId)
            binding.pbCarouselDownload.visibility = if (isDownloading) View.VISIBLE else View.GONE
            if (isDownloading) {
                binding.pbCarouselDownload.progress = 0
            }
        }
    }

    class LibraryViewHolder(
        private val binding: ItemLibraryDvdBinding,
        private val onItemClick: (LibraryGridItem) -> Unit,
        private val onItemLongClick: ((LibraryGridItem) -> Unit)?,
        private val onDragStarted: ((RecyclerView.ViewHolder) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LibraryGridItem) {
            Glide.with(binding.root.context)
                .load(item.poster)
                .placeholder(R.drawable.bg_y2k_pill_hotpink)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivDvdCover)

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnAddPlaylist.visibility = View.GONE

            if (onItemLongClick != null) {
                binding.root.setOnLongClickListener {
                    onItemLongClick(item)
                    true
                }
            } else {
                binding.root.setOnLongClickListener(null)
            }
        }
    }
}
