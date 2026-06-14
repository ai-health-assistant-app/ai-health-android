package com.ai_health.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.domain.model.User
import com.ai_health.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    val userProfile = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), User())

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus = _saveStatus.asStateFlow()

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    fun saveProfile(name: String, weight: String, height: String, gender: String) {
        val weightFloat = weight.replace(",", ".").toFloatOrNull()
        if (weightFloat == null || weightFloat <= 0f) {
            _saveStatus.value = SaveStatus.Error("Peso non valido")
            return
        }

        val heightInt = height.toIntOrNull()
        if (heightInt == null || heightInt <= 0) {
            _saveStatus.value = SaveStatus.Error("Altezza non valida")
            return
        }

        if (name.isBlank()) {
            _saveStatus.value = SaveStatus.Error("Il nome non può essere vuoto")
            return
        }

        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving
            val updatedProfile = userProfile.value.copy(
                name = name,
                weight = weightFloat,
                height = heightInt,
                gender = gender
            )
            repository.saveUser(updatedProfile)
            _saveStatus.value = SaveStatus.Success
        }
    }

    fun signIn(idToken: String) {
        android.util.Log.d("SettingsViewModel", "signIn: called with token length ${idToken.length}")
        viewModelScope.launch {
            repository.signInWithGoogle(idToken)
                .onFailure { e ->
                    android.util.Log.e("SettingsViewModel", "signIn: failure", e)
                    e.printStackTrace()
                }
                .onSuccess {
                    android.util.Log.d("SettingsViewModel", "signIn: success")
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}

sealed class SaveStatus {
    object Idle : SaveStatus()
    object Saving : SaveStatus()
    object Success : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}