package com.fin.mobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fin_app_preferences")

/**
 * Jetpack DataStore-based preference manager for non-sensitive application settings.
 */
class AppPreferences(private val context: Context) {

    val selectedLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE
    }

    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE]
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = languageCode
        }
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit { preferences ->
            if (enabled == null) {
                preferences.remove(KEY_DARK_MODE)
            } else {
                preferences[KEY_DARK_MODE] = enabled
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_DONE] = completed
        }
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "en"
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_DARK_MODE = booleanPreferencesKey("app_dark_mode")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("app_onboarding_completed")
    }
}
