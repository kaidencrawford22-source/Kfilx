package com.kflix.app.fragments.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kflix.app.database.LibraryDatabase
import com.kflix.app.models.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProfilePickerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LibraryDatabase.getInstance(application)
    private val profileDao = db.profileDao()

    val profiles: StateFlow<List<Profile>> = profileDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun createProfile(name: String, iconRes: String, age: Int?) = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true
        val count = profileDao.getCount()
        profileDao.insert(
            Profile(
                id = UUID.randomUUID().toString(),
                name = name,
                iconRes = iconRes,
                age = age,
                isDefault = count == 0
            )
        )
        _isLoading.value = false
    }

    fun updateProfile(id: String, name: String, iconRes: String, age: Int?) = viewModelScope.launch(Dispatchers.IO) {
        profileDao.update(id, name, iconRes, age)
    }

    fun deleteProfile(id: String) = viewModelScope.launch(Dispatchers.IO) {
        profileDao.delete(id)
        val prefs = com.kflix.app.utils.UserPreferences
        if (prefs.currentProfileId == id) {
            prefs.currentProfileId = null
        }
        val remaining = profileDao.getAllSync()
        if (remaining.isNotEmpty() && !remaining.any { it.isDefault }) {
            profileDao.setDefault(remaining.first().id)
        }
    }
}
