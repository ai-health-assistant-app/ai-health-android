package com.ai_health.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.data.local.entity.UserProfileEntity
import com.ai_health.core.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    val userProfile = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileEntity())

    fun saveProfile(name: String, weight: String, height: String, gender: String) {
        viewModelScope.launch {
            val weightFloat = weight.replace(",", ".").toFloatOrNull() ?: 0f
            val heightInt = height.toIntOrNull() ?: 0

            val updatedProfile = userProfile.value.copy(
                name = name,
                weight = weightFloat,
                height = heightInt,
                gender = gender
            )
            repository.saveUser(updatedProfile)
        }
    }
}