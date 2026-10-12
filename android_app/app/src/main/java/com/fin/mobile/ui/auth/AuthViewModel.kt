package com.fin.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AuthState {
    data object CheckingSession : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
    data class Loading(val action: String = "Please wait...") : AuthState()
    data class Authenticated(val user: UserDto, val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val mode: String = "citizen", // "citizen" or "reviewer"
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false
) {
    val isValid: Boolean get() = emailError == null && passwordError == null && email.isNotBlank() && password.isNotBlank()
}

data class SignupFormState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val fullNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false
) {
    val isValid: Boolean
        get() = fullNameError == null &&
                emailError == null &&
                phoneError == null &&
                passwordError == null &&
                confirmPasswordError == null &&
                fullName.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.CheckingSession)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginFormState())
    val loginState: StateFlow<LoginFormState> = _loginState.asStateFlow()

    private val _signupState = MutableStateFlow(SignupFormState())
    val signupState: StateFlow<SignupFormState> = _signupState.asStateFlow()

    init {
        restoreSession()
    }

    fun restoreSession() {
        viewModelScope.launch {
            _authState.value = AuthState.CheckingSession
            if (!authRepository.hasActiveSession()) {
                _authState.value = AuthState.Unauthenticated()
                return@launch
            }

            when (val result = authRepository.verifyOrRestoreSession()) {
                is NetworkResult.Success -> {
                    val user = result.data
                    val role = user.role ?: "user"
                    _authState.value = AuthState.Authenticated(user = user, role = role)
                }
                is NetworkResult.Error -> {
                    _authState.value = AuthState.Unauthenticated(
                        message = if (result.code == 401) "Session expired. Please log in again." else result.message
                    )
                }
                is NetworkResult.Exception -> {
                    // In case of network failure on startup, if a session exists locally, fall back to cached role
                    val cachedRole = authRepository.getStoredUserRole() ?: "user"
                    val cachedUserId = authRepository.getStoredUserId() ?: "offline-user"
                    val fallbackUser = UserDto(id = cachedUserId, role = cachedRole)
                    _authState.value = AuthState.Authenticated(user = fallbackUser, role = cachedRole)
                }
            }
        }
    }

    // --- Login Handlers ---
    fun onLoginEmailChanged(email: String) {
        val trimmed = email.trim()
        val error = when {
            trimmed.isEmpty() -> null
            !EMAIL_REGEX.matches(trimmed) -> "Invalid email format"
            else -> null
        }
        _loginState.update { it.copy(email = email, emailError = error, generalError = null) }
    }

    fun onLoginPasswordChanged(password: String) {
        val error = when {
            password.isEmpty() -> null
            password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
            else -> null
        }
        _loginState.update { it.copy(password = password, passwordError = error, generalError = null) }
    }

    fun onLoginModeChanged(mode: String) {
        _loginState.update { it.copy(mode = mode, generalError = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _loginState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submitLogin(onSuccess: (role: String) -> Unit = {}) {
        val email = _loginState.value.email.trim()
        val password = _loginState.value.password
        val mode = _loginState.value.mode

        // Validate before calling backend
        val emailError = if (email.isBlank() || !EMAIL_REGEX.matches(email)) "Valid email is required" else null
        val passwordError = if (password.length < MIN_PASSWORD_LENGTH) "Password must be at least $MIN_PASSWORD_LENGTH characters" else null

        if (emailError != null || passwordError != null) {
            _loginState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isSubmitting = true, generalError = null) }
            _authState.value = AuthState.Loading("Signing in...")

            when (val result = authRepository.login(email = email, password = password, mode = mode)) {
                is NetworkResult.Success -> {
                    val user = result.data.user ?: UserDto(id = "unknown", role = mode)
                    val role = user.role ?: mode
                    _loginState.update { it.copy(isSubmitting = false, generalError = null) }
                    _authState.value = AuthState.Authenticated(user = user, role = role)
                    onSuccess(role)
                }
                is NetworkResult.Error -> {
                    _loginState.update { it.copy(isSubmitting = false, generalError = result.message) }
                    _authState.value = AuthState.Error(result.message)
                }
                is NetworkResult.Exception -> {
                    val errorMsg = result.throwable.localizedMessage ?: "Unable to connect to server. Check your connection."
                    _loginState.update { it.copy(isSubmitting = false, generalError = errorMsg) }
                    _authState.value = AuthState.Error(errorMsg)
                }
            }
        }
    }

    // --- Signup Handlers ---
    fun onSignupFullNameChanged(name: String) {
        val error = if (name.isNotBlank() && name.trim().length < 2) "Name is too short" else null
        _signupState.update { it.copy(fullName = name, fullNameError = error, generalError = null) }
    }

    fun onSignupEmailChanged(email: String) {
        val trimmed = email.trim()
        val error = when {
            trimmed.isEmpty() -> null
            !EMAIL_REGEX.matches(trimmed) -> "Invalid email format"
            else -> null
        }
        _signupState.update { it.copy(email = email, emailError = error, generalError = null) }
    }

    fun onSignupPhoneChanged(phone: String) {
        val trimmed = phone.trim()
        val error = when {
            trimmed.isEmpty() -> null
            !PHONE_REGEX.matches(trimmed) -> "Invalid phone number format"
            else -> null
        }
        _signupState.update { it.copy(phone = phone, phoneError = error, generalError = null) }
    }

    fun onSignupPasswordChanged(password: String) {
        val passwordError = when {
            password.isEmpty() -> null
            password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
            else -> null
        }
        val confirmError = if (_signupState.value.confirmPassword.isNotEmpty() && _signupState.value.confirmPassword != password) {
            "Passwords do not match"
        } else null

        _signupState.update {
            it.copy(
                password = password,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
                generalError = null
            )
        }
    }

    fun onSignupConfirmPasswordChanged(confirmPassword: String) {
        val confirmError = when {
            confirmPassword.isEmpty() -> null
            confirmPassword != _signupState.value.password -> "Passwords do not match"
            else -> null
        }
        _signupState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = confirmError, generalError = null) }
    }

    fun toggleSignupPasswordVisibility() {
        _signupState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submitSignup(onSuccess: (role: String) -> Unit = {}) {
        val fullName = _signupState.value.fullName.trim()
        val email = _signupState.value.email.trim()
        val phone = _signupState.value.phone.trim().ifBlank { null }
        val password = _signupState.value.password
        val confirmPassword = _signupState.value.confirmPassword

        val nameError = if (fullName.isBlank()) "Full name is required" else null
        val emailError = if (email.isBlank() || !EMAIL_REGEX.matches(email)) "Valid email is required" else null
        val passwordError = if (password.length < MIN_PASSWORD_LENGTH) "Password must be at least $MIN_PASSWORD_LENGTH characters" else null
        val confirmError = if (confirmPassword != password) "Passwords do not match" else null

        if (nameError != null || emailError != null || passwordError != null || confirmError != null) {
            _signupState.update {
                it.copy(
                    fullNameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }

        viewModelScope.launch {
            _signupState.update { it.copy(isSubmitting = true, generalError = null) }
            _authState.value = AuthState.Loading("Creating account...")

            when (val result = authRepository.register(email = email, password = password, fullName = fullName, phone = phone)) {
                is NetworkResult.Success -> {
                    val user = result.data.user ?: UserDto(id = "unknown", role = "user")
                    val role = user.role ?: "user"
                    _signupState.update { it.copy(isSubmitting = false, generalError = null) }
                    _authState.value = AuthState.Authenticated(user = user, role = role)
                    onSuccess(role)
                }
                is NetworkResult.Error -> {
                    _signupState.update { it.copy(isSubmitting = false, generalError = result.message) }
                    _authState.value = AuthState.Error(result.message)
                }
                is NetworkResult.Exception -> {
                    val errorMsg = result.throwable.localizedMessage ?: "Unable to connect to server. Check your connection."
                    _signupState.update { it.copy(isSubmitting = false, generalError = errorMsg) }
                    _authState.value = AuthState.Error(errorMsg)
                }
            }
        }
    }

    fun logout(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading("Signing out...")
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated()
            _loginState.value = LoginFormState()
            _signupState.value = SignupFormState()
            onCompleted()
        }
    }

    fun clearErrors() {
        _loginState.update { it.copy(generalError = null) }
        _signupState.update { it.copy(generalError = null) }
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated()
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private val EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
        private val PHONE_REGEX = "^[0-9+()\\-\\s]{7,15}$".toRegex()

        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository) as T
                }
            }
    }
}
