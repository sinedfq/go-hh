package com.example.trudomix.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

class ThemeManager(context: Context) {
    private val dataStore = context.themeDataStore

    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme_mode")
    }

    val themeModeFlow: Flow<AppThemeMode> = dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            AppThemeMode.DARK.name -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }
}
