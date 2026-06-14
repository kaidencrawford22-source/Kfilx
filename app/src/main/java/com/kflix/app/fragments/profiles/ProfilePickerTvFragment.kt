package com.kflix.app.fragments.profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kflix.app.R
import com.kflix.app.adapters.ProfileAdapter
import com.kflix.app.databinding.FragmentProfilePickerTvBinding
import com.kflix.app.database.LibraryDatabase
import com.kflix.app.ui.ProfileEditDialog
import com.kflix.app.utils.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilePickerTvFragment : Fragment() {

    private var _binding: FragmentProfilePickerTvBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfilePickerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePickerTvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val db = LibraryDatabase.getInstance(requireContext())
            val profiles = withContext(Dispatchers.IO) {
                db.profileDao().getAllSync()
            }
            val currentId = UserPreferences.currentProfileId

            if (currentId != null && profiles.any { it.id == currentId }) {
                findNavController().navigate(
                    ProfilePickerTvFragmentDirections.actionProfilePickerToHome()
                )
                return@launch
            }

            if (profiles.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
            }

            val adapter = ProfileAdapter(
                isTv = true,
                onProfileClick = { profile ->
                    UserPreferences.currentProfileId = profile.id
                    findNavController().navigate(
                        ProfilePickerTvFragmentDirections.actionProfilePickerToHome()
                    )
                },
                onEditClick = { profile ->
                    ProfileEditDialog.show(requireContext(), profile) { name, icon, age ->
                        viewModel.updateProfile(profile.id, name, icon, age)
                    }
                },
                onDeleteClick = { profile ->
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.profile_delete_title)
                        .setMessage(getString(R.string.profile_delete_message, profile.name))
                        .setPositiveButton(R.string.profile_delete) { _, _ ->
                            viewModel.deleteProfile(profile.id)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            )

            binding.rvProfiles.adapter = adapter
            binding.rvProfiles.requestFocus()

            lifecycleScope.launch {
                viewModel.profiles.collectLatest { profileList ->
                    adapter.submitList(profileList)
                    binding.emptyState.visibility =
                        if (profileList.isEmpty()) View.VISIBLE else View.GONE
                }
            }

            binding.btnAddProfile.setOnClickListener {
                ProfileEditDialog.show(requireContext()) { name, icon, age ->
                    viewModel.createProfile(name, icon, age)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
