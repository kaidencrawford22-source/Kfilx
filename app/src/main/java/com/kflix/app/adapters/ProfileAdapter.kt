package com.kflix.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kflix.app.R
import com.kflix.app.databinding.ItemProfilePickerMobileBinding
import com.kflix.app.databinding.ItemProfilePickerTvBinding
import com.kflix.app.models.Profile

class ProfileAdapter(
    private val isTv: Boolean = false,
    private val onProfileClick: (Profile) -> Unit,
    private val onEditClick: ((Profile) -> Unit)? = null,
    private val onDeleteClick: ((Profile) -> Unit)? = null,
) : ListAdapter<Profile, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int = if (isTv) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            val binding = ItemProfilePickerTvBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            TvViewHolder(binding)
        } else {
            val binding = ItemProfilePickerMobileBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            MobileViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val profile = getItem(position)
        when (holder) {
            is MobileViewHolder -> holder.bind(profile)
            is TvViewHolder -> holder.bind(profile)
        }
    }

    inner class MobileViewHolder(private val binding: ItemProfilePickerMobileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            val resId = binding.root.context.resources.getIdentifier(
                profile.iconRes, "drawable", binding.root.context.packageName
            )
            Glide.with(binding.root)
                .load(if (resId != 0) resId else R.drawable.ic_profile_blue)
                .into(binding.ivProfileIcon)
            binding.tvProfileName.text = profile.name
            if (profile.age != null) {
                binding.tvProfileAge.text = binding.root.context.getString(
                    R.string.profile_age_display, profile.age
                )
                binding.tvProfileAge.visibility = android.view.View.VISIBLE
            } else {
                binding.tvProfileAge.visibility = android.view.View.GONE
            }
            binding.root.setOnClickListener { onProfileClick(profile) }
            binding.btnEdit.setOnClickListener { onEditClick?.invoke(profile) }
            binding.btnDelete.setOnClickListener { onDeleteClick?.invoke(profile) }
        }
    }

    inner class TvViewHolder(private val binding: ItemProfilePickerTvBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            val resId = binding.root.context.resources.getIdentifier(
                profile.iconRes, "drawable", binding.root.context.packageName
            )
            Glide.with(binding.root)
                .load(if (resId != 0) resId else R.drawable.ic_profile_blue)
                .into(binding.ivProfileIcon)
            binding.tvProfileName.text = profile.name
            if (profile.age != null) {
                binding.tvProfileAge.text = binding.root.context.getString(
                    R.string.profile_age_display, profile.age
                )
                binding.tvProfileAge.visibility = android.view.View.VISIBLE
            } else {
                binding.tvProfileAge.visibility = android.view.View.GONE
            }
            binding.root.setOnClickListener { onProfileClick(profile) }
            binding.btnEdit.setOnClickListener { onEditClick?.invoke(profile) }
            binding.btnDelete.setOnClickListener { onDeleteClick?.invoke(profile) }

            val hasEdit = onEditClick != null
            val hasDelete = onDeleteClick != null
            binding.root.onFocusChangeListener = null
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                binding.btnEdit.visibility = if (hasFocus && hasEdit) android.view.View.VISIBLE else android.view.View.GONE
                binding.btnDelete.visibility = if (hasFocus && hasDelete) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Profile>() {
        override fun areItemsTheSame(old: Profile, new: Profile): Boolean = old.id == new.id
        override fun areContentsTheSame(old: Profile, new: Profile): Boolean = old == new
    }
}