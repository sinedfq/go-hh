package com.example.trudomix.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trudomix_prefs")

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun clearSession(expectedToken: String? = null) {
        var cleared = false
        context.dataStore.edit { prefs ->
            if (expectedToken == null || prefs[TOKEN_KEY] == expectedToken) {
                prefs.remove(TOKEN_KEY)
                cleared = true
            }
        }
        if (cleared) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.trudomix.data.api.ApiClient.snapshots.clear()
        }
    }
}
