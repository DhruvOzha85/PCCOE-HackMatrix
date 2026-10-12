package com.fin.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.data.remote.dto.UserProfileDto
import com.fin.mobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoadingProfile: Boolean = false,
    val isUpdatingProfile: Boolean = false,
    val isChangingPassword: Boolean = false,
    val isLoggingOut: Boolean = false,
    val user: UserDto? = null,
    val profile: UserProfileDto? = null,
    val language: String = "en",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val passwordError: String? = null,
    val passwordSuccess: String? = null,
    val isPasswordDialogOpen: Boolean = false,
    val isEditProfileDialogOpen: Boolean = false
)

class SettingsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(language = authRepository.getLanguage())
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProfile = true, errorMessage = null) }

            // 1. Restore current user
            val userResult = authRepository.verifyOrRestoreSession()
            val currentUser = (userResult as? NetworkResult.Success)?.data

            // 2. Fetch server-authoritative profile
            when (val profileResult = authRepository.getProfile()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            user = currentUser,
                            profile = profileResult.data,
                            language = authRepository.getLanguage(),
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            user = currentUser,
                            errorMessage = profileResult.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            user = currentUser,
                            errorMessage = "Unable to load profile from server. Offline or network timeout."
                        )
                    }
                }
            }
        }
    }

    fun openEditProfileDialog() {
        _uiState.update { it.copy(isEditProfileDialogOpen = true, errorMessage = null, successMessage = null) }
    }

    fun closeEditProfileDialog() {
        _uiState.update { it.copy(isEditProfileDialogOpen = false) }
    }

    fun updateProfile(
        phone: String?,
        state: String?,
        district: String?,
        occupation: String?,
        annualIncome: Double?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingProfile = true, errorMessage = null, successMessage = null) }

            val updatePayload = mutableMapOf<String, Any?>()
            if (!phone.isNullOrBlank()) updatePayload["phone"] = phone.trim()
            if (!state.isNullOrBlank()) updatePayload["state"] = state.trim()
            if (!district.isNullOrBlank()) updatePayload["district"] = district.trim()
            if (!occupation.isNullOrBlank()) updatePayload["occupation"] = occupation.trim()
            if (annualIncome != null && annualIncome >= 0) updatePayload["annual_income"] = annualIncome

            when (val result = authRepository.updateProfile(updatePayload)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            isEditProfileDialogOpen = false,
                            profile = result.data,
                            successMessage = "Profile updated successfully"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            errorMessage = "Failed to update profile: network error"
                        )
                    }
                }
            }
        }
    }

    fun openPasswordDialog() {
        _uiState.update {
            it.copy(
                isPasswordDialogOpen = true,
                passwordError = null,
                passwordSuccess = null
            )
        }
    }

    fun closePasswordDialog() {
        _uiState.update { it.copy(isPasswordDialogOpen = false, passwordError = null) }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        // Pre-validation matching backend and frontend contracts
        if (currentPassword.isBlank()) {
            _uiState.update { it.copy(passwordError = "Current password is required") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(passwordError = "New password must be at least 6 characters") }
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(passwordError = "New password and confirmation do not match") }
            return
        }
        if (newPassword == currentPassword) {
            _uiState.update { it.copy(passwordError = "New password must be different from current password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, passwordError = null) }

            when (val result = authRepository.changePassword(currentPassword, newPassword)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            isPasswordDialogOpen = false,
                            passwordSuccess = result.data,
                            successMessage = result.data
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            passwordError = result.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            passwordError = "Network error: Unable to change password"
                        )
                    }
                }
            }
        }
    }

    fun setLanguage(language: String) {
        authRepository.setLanguage(language)
        _uiState.update {
            it.copy(
                language = language,
                successMessage = "Language set to ${getLanguageDisplayName(language)}"
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
            onLoggedOut()
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null, passwordSuccess = null) }
    }

    private fun getLanguageDisplayName(code: String): String = when (code) {
        "hi" -> "हिंदी (Hindi)"
        "mr" -> "मराठी (Marathi)"
        "ta" -> "தமிழ் (Tamil)"
        "te" -> "తెలుగు (Telugu)"
        else -> "English"
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(authRepository) as T
                }
            }
    }
}
