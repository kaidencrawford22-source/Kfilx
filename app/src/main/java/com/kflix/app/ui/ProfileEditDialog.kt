package com.kflix.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.kflix.app.R
import com.kflix.app.databinding.DialogProfileEditBinding
import com.kflix.app.models.Profile

class ProfileEditDialog private constructor(
    private val context: Context,
    private val existingProfile: Profile? = null,
    private val onSave: (name: String, iconRes: String, age: Int?) -> Unit,
) {

    private var selectedIcon = existingProfile?.iconRes ?: "ic_profile_blue"

    fun show() {
        val binding = DialogProfileEditBinding.inflate(LayoutInflater.from(context))

        if (existingProfile != null) {
            binding.etProfileName.setText(existingProfile.name)
            selectedIcon = existingProfile.iconRes
            if (existingProfile.age != null) {
                binding.etProfileAge.setText(existingProfile.age.toString())
            }
        }

        val iconNames = listOf(
            "ic_profile_red",
            "ic_profile_blue",
            "ic_profile_green",
            "ic_profile_purple",
            "ic_profile_orange",
            "ic_profile_pink",
            "ic_profile_kid",
        )

        val iconGrid = binding.iconGrid
        iconGrid.removeAllViews()
        for (iconName in iconNames) {
            val iconView = ImageView(context).apply {
                val resId = context.resources.getIdentifier(
                    iconName, "drawable", context.packageName
                )
                setImageResource(if (resId != 0) resId else R.drawable.ic_profile_blue)
                layoutParams = ViewGroup.LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.profile_icon_picker_size),
                    context.resources.getDimensionPixelSize(R.dimen.profile_icon_picker_size),
                )
                setPadding(8, 8, 8, 8)
                scaleType = ImageView.ScaleType.CENTER_CROP
                isClickable = true
                isFocusable = true
                alpha = if (iconName == selectedIcon) 1.0f else 0.4f
                setOnClickListener {
                    selectedIcon = iconName
                    for (i in 0 until iconGrid.childCount) {
                        iconGrid.getChildAt(i).alpha = 0.4f
                    }
                    alpha = 1.0f
                }
            }
            iconGrid.addView(iconView)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(if (existingProfile != null) R.string.profile_edit_title else R.string.profile_create_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = binding.etProfileName.text?.toString()?.trim() ?: return@setPositiveButton
                if (name.isEmpty()) return@setPositiveButton
                val ageText = binding.etProfileAge.text?.toString()?.trim()
                val age = ageText?.toIntOrNull()
                onSave(name, selectedIcon, age)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.show()
    }

    companion object {
        fun show(
            context: Context,
            profile: Profile? = null,
            onSave: (name: String, iconRes: String, age: Int?) -> Unit,
        ) {
            ProfileEditDialog(context, profile, onSave).show()
        }
    }
}
