package com.ultrastream.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesManager.getTheme(),
                preferencesManager.getHindiPriority(),
                preferencesManager.getAutoPlayNext(),
                preferencesManager.getParentalControl(),
                preferencesManager.getCurrentProfile()
            ) { theme, hindiPriority, autoPlayNext, parentalControl, currentProfile ->
                ProfileUiState(
                    theme = theme,
                    hindiPriority = hindiPriority,
                    autoPlayNext = autoPlayNext,
                    parentalControl = parentalControl,
                    currentProfile = currentProfile
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    suspend fun toggleTheme() {
        val current = _uiState.value.theme
        val newTheme = if (current == "dark") "light" else "dark"
        preferencesManager.setTheme(newTheme)
    }

    suspend fun toggleHindiPriority() {
        val current = _uiState.value.hindiPriority
        preferencesManager.setHindiPriority(!current)
    }

    suspend fun toggleAutoPlayNext() {
        val current = _uiState.value.autoPlayNext
        preferencesManager.setAutoPlayNext(!current)
    }

    suspend fun toggleParentalControl() {
        val current = _uiState.value.parentalControl
        preferencesManager.setParentalControl(!current)
    }

    suspend fun setCurrentProfile(profileId: String) {
        preferencesManager.setCurrentProfile(profileId)
    }

    data class ProfileUiState(
        val theme: String = "dark",
        val hindiPriority: Boolean = true,
        val autoPlayNext: Boolean = false,
        val parentalControl: Boolean = false,
        val currentProfile: String = "default"
    )
}
