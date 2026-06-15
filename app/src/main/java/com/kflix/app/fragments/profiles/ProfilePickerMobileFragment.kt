package com.kflix.app.fragments.profiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.kflix.app.R
import com.kflix.app.adapters.ProfileAdapter
import com.kflix.app.databinding.FragmentProfilePickerMobileBinding
import com.kflix.app.ui.ProfileEditDialog
import com.kflix.app.utils.UserPreferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfilePickerMobileFragment : Fragment() {

    private var _binding: FragmentProfilePickerMobileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfilePickerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePickerMobileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ProfileAdapter(
            isTv = false,
            onProfileClick = { profile ->
                UserPreferences.currentProfileId = profile.id
                findNavController().navigate(
                    ProfilePickerMobileFragmentDirections.actionProfilePickerToHome()
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

        binding.rvProfiles.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProfiles.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profiles.collectLatest { profileList ->
                adapter.submitList(profileList)
                binding.emptyState.visibility =
                    if (profileList.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fabAddProfile.setOnClickListener {
            ProfileEditDialog.show(requireContext()) { name, icon, age ->
                viewModel.createProfile(name, icon, age)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
