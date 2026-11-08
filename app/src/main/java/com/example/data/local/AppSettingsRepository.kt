package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quizpoll_settings")

enum class ThemeMode {
  SYSTEM, LIGHT, DARK
}

data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val preferredLanguage: String = "English",
  val notificationsEnabled: Boolean = true,
  val offlineCacheEnabled: Boolean = true
)

class AppSettingsRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val OFFLINE_CACHE_ENABLED = booleanPreferencesKey("offline_cache_enabled")
  }

  val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
    val themeStr = preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
    val theme = try {
      ThemeMode.valueOf(themeStr)
    } catch (e: Exception) {
      ThemeMode.SYSTEM
    }
    AppSettings(
      themeMode = theme,
      preferredLanguage = preferences[Keys.PREFERRED_LANGUAGE] ?: "English",
      notificationsEnabled = preferences[Keys.NOTIFICATIONS_ENABLED] ?: true,
      offlineCacheEnabled = preferences[Keys.OFFLINE_CACHE_ENABLED] ?: true
    )
  }

  suspend fun updateThemeMode(mode: ThemeMode) {
    context.dataStore.edit { preferences ->
      preferences[Keys.THEME_MODE] = mode.name
    }
  }

  suspend fun updatePreferredLanguage(language: String) {
    context.dataStore.edit { preferences ->
      preferences[Keys.PREFERRED_LANGUAGE] = language
    }
  }

  suspend fun updateNotificationsEnabled(enabled: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[Keys.NOTIFICATIONS_ENABLED] = enabled
    }
  }

  suspend fun updateOfflineCacheEnabled(enabled: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[Keys.OFFLINE_CACHE_ENABLED] = enabled
    }
  }

  suspend fun clearAllPreferences() {
    context.dataStore.edit { it.clear() }
  }
}
