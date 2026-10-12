package com.fin.mobile.data.local

/**
 * Interface providing authentication tokens to networking interceptors.
 * Does not assume any hardcoded credentials or mock behavior.
 */
interface SessionTokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
}

/**
 * Interface for secure local persistence of authenticated session data.
 */
interface SessionStorage : SessionTokenProvider {
    fun saveSession(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long? = null,
        userId: String? = null,
        userRole: String? = null
    )

    fun getUserId(): String?
    fun getUserRole(): String?
    fun hasActiveSession(): Boolean
    fun clearSession()

    fun getLanguage(): String = "en"
    fun setLanguage(language: String) {}
}

