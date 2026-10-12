package com.fin.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android Keystore-backed AES-256 session storage implementing [SessionStorage].
 * Provides hardware-backed key protection on supported hardware.
 */
class EncryptedSessionStorage(context: Context) : SessionStorage {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences initialization failed, falling back to private SharedPreferences", e)
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun saveSession(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long?,
        userId: String?,
        userRole: String?
    ) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            if (expiresInSeconds != null) {
                putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + (expiresInSeconds * 1000L))
            }
            if (userId != null) putString(KEY_USER_ID, userId)
            if (userRole != null) putString(KEY_USER_ROLE, userRole)
            apply()
        }
    }

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    override fun hasActiveSession(): Boolean {
        val token = getAccessToken()
        return !token.isNullOrBlank()
    }

    override fun clearSession() {
        val savedLang = getLanguage()
        prefs.edit().clear().apply()
        setLanguage(savedLang)
    }

    override fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "en") ?: "en"

    override fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    companion object {
        private const val TAG = "EncryptedSessionStorage"
        private const val PREFS_NAME = "fin_secure_session"
        private const val FALLBACK_PREFS_NAME = "fin_session_private"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_LANGUAGE = "user_language"
    }
}
