package com.fin.mobile.data.repository

import com.fin.mobile.data.local.SessionStorage
import com.fin.mobile.data.remote.FinApiService
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.AuthResponseDto
import com.fin.mobile.data.remote.dto.ChangePasswordRequestDto
import com.fin.mobile.data.remote.dto.LoginRequestDto
import com.fin.mobile.data.remote.dto.RefreshTokenRequestDto
import com.fin.mobile.data.remote.dto.RegisterRequestDto
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.data.remote.dto.UserProfileDto
import com.fin.mobile.data.remote.safeApiCall
import com.squareup.moshi.Moshi

interface AuthRepository {
    suspend fun login(email: String, password: String, mode: String = "citizen"): NetworkResult<AuthResponseDto>
    suspend fun register(email: String, password: String, fullName: String, phone: String? = null): NetworkResult<AuthResponseDto>
    suspend fun verifyOrRestoreSession(): NetworkResult<UserDto>
    suspend fun logout(): NetworkResult<Unit>
    suspend fun getProfile(): NetworkResult<UserProfileDto> = NetworkResult.Error(501, "Not implemented")
    suspend fun updateProfile(updates: Map<String, Any?>): NetworkResult<UserProfileDto> = NetworkResult.Error(501, "Not implemented")
    suspend fun changePassword(currentPassword: String, newPassword: String): NetworkResult<String> = NetworkResult.Error(501, "Not implemented")
    fun hasActiveSession(): Boolean
    fun getStoredUserRole(): String?
    fun getStoredUserId(): String?
    fun getLanguage(): String = "en"
    fun setLanguage(language: String) {}
}

class AuthRepositoryImpl(
    private val apiService: FinApiService,
    private val sessionStorage: SessionStorage,
    private val moshi: Moshi
) : AuthRepository {

    override suspend fun login(email: String, password: String, mode: String): NetworkResult<AuthResponseDto> {
        val normalizedEmail = email.trim().lowercase()
        val request = LoginRequestDto(email = normalizedEmail, password = password, mode = mode)
        val result = safeApiCall(moshi) { apiService.login(request) }

        if (result is NetworkResult.Success) {
            val authData = result.data
            val token = authData.accessToken
            if (!token.isNullOrBlank()) {
                sessionStorage.saveSession(
                    accessToken = token,
                    refreshToken = authData.refreshToken,
                    expiresInSeconds = authData.expiresIn,
                    userId = authData.user?.id,
                    userRole = authData.user?.role ?: "user"
                )
            }
        }
        return result
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String?
    ): NetworkResult<AuthResponseDto> {
        val normalizedEmail = email.trim().lowercase()
        val normalizedPhone = phone?.trim()?.ifBlank { null }
        val request = RegisterRequestDto(
            email = normalizedEmail,
            password = password,
            fullName = fullName.trim(),
            phone = normalizedPhone
        )
        val result = safeApiCall(moshi) { apiService.register(request) }

        if (result is NetworkResult.Success) {
            val authData = result.data
            val token = authData.accessToken
            if (!token.isNullOrBlank()) {
                sessionStorage.saveSession(
                    accessToken = token,
                    refreshToken = authData.refreshToken,
                    expiresInSeconds = authData.expiresIn,
                    userId = authData.user?.id,
                    userRole = authData.user?.role ?: "user"
                )
            }
        }
        return result
    }

    override suspend fun verifyOrRestoreSession(): NetworkResult<UserDto> {
        if (!sessionStorage.hasActiveSession()) {
            return NetworkResult.Error(401, "No active session")
        }

        val meResult = safeApiCall(moshi) { apiService.getMe() }
        return when (meResult) {
            is NetworkResult.Success -> {
                val user = meResult.data.user
                if (user != null) {
                    sessionStorage.saveSession(
                        accessToken = sessionStorage.getAccessToken() ?: "",
                        refreshToken = sessionStorage.getRefreshToken(),
                        userId = user.id,
                        userRole = user.role ?: "user"
                    )
                    NetworkResult.Success(user)
                } else {
                    NetworkResult.Error(500, "User profile missing from response")
                }
            }
            is NetworkResult.Error -> {
                // If 401 Unauthorized, attempt automatic refresh using stored refresh token
                if (meResult.code == 401) {
                    val refreshToken = sessionStorage.getRefreshToken()
                    if (!refreshToken.isNullOrBlank()) {
                        val refreshResult = safeApiCall(moshi) {
                            apiService.refreshToken(RefreshTokenRequestDto(refreshToken))
                        }
                        if (refreshResult is NetworkResult.Success) {
                            val authData = refreshResult.data
                            val newToken = authData.accessToken
                            val newUser = authData.user
                            if (!newToken.isNullOrBlank() && newUser != null) {
                                sessionStorage.saveSession(
                                    accessToken = newToken,
                                    refreshToken = authData.refreshToken ?: refreshToken,
                                    expiresInSeconds = authData.expiresIn,
                                    userId = newUser.id,
                                    userRole = newUser.role ?: "user"
                                )
                                return NetworkResult.Success(newUser)
                            }
                        }
                    }
                    // Refresh failed or no refresh token available
                    sessionStorage.clearSession()
                }
                meResult
            }
            is NetworkResult.Exception -> {
                meResult
            }
        }
    }

    override suspend fun logout(): NetworkResult<Unit> {
        return try {
            safeApiCall(moshi) { apiService.logout() }
            NetworkResult.Success(Unit)
        } finally {
            // Local state is cleared unconditionally regardless of server reachability
            sessionStorage.clearSession()
        }
    }

    override fun hasActiveSession(): Boolean = sessionStorage.hasActiveSession()

    override fun getStoredUserRole(): String? = sessionStorage.getUserRole()

    override fun getStoredUserId(): String? = sessionStorage.getUserId()

    override suspend fun getProfile(): NetworkResult<UserProfileDto> {
        val result = safeApiCall(moshi) { apiService.getProfile() }
        return when (result) {
            is NetworkResult.Success -> {
                val data = result.data.data
                if (data != null) {
                    NetworkResult.Success(data)
                } else {
                    NetworkResult.Error(500, "Profile data missing from response")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun updateProfile(updates: Map<String, Any?>): NetworkResult<UserProfileDto> {
        val result = safeApiCall(moshi) { apiService.updateProfile(updates) }
        return when (result) {
            is NetworkResult.Success -> {
                val data = result.data.data
                if (data != null) {
                    NetworkResult.Success(data)
                } else {
                    NetworkResult.Error(500, "Updated profile data missing from response")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): NetworkResult<String> {
        val request = ChangePasswordRequestDto(currentPassword = currentPassword, newPassword = newPassword)
        val result = safeApiCall(moshi) { apiService.changePassword(request) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.message ?: "Password updated successfully")
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override fun getLanguage(): String = sessionStorage.getLanguage()

    override fun setLanguage(language: String) {
        sessionStorage.setLanguage(language)
    }
}

