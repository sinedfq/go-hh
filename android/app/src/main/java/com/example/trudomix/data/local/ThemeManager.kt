package com.example.trudomix.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

class ThemeManager(context: Context) {
    private val appContext=context.applicationContext
    private val mirror=context.getSharedPreferences("theme_cache",Context.MODE_PRIVATE)
    private val dataStore = context.themeDataStore

    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme_mode")
        private val NAV_ANIMATION_KEY = stringPreferencesKey("nav_animation_style")
        fun cachedMode(context:Context):AppThemeMode=runCatching{AppThemeMode.valueOf(context.getSharedPreferences("theme_cache",Context.MODE_PRIVATE).getString("mode",AppThemeMode.SYSTEM.name)!!)}.getOrDefault(AppThemeMode.SYSTEM)
        fun syncPlatformTheme(context:Context,mode:AppThemeMode){
            if(android.os.Build.VERSION.SDK_INT<31)return
            val prefs=context.getSharedPreferences("theme_cache",Context.MODE_PRIVATE)
            if(prefs.getString("platform_mode",null)==mode.name)return
            prefs.edit().putString("platform_mode",mode.name).apply()
            val platform=when(mode){AppThemeMode.DARK->android.app.UiModeManager.MODE_NIGHT_YES;AppThemeMode.LIGHT->android.app.UiModeManager.MODE_NIGHT_NO;else->android.app.UiModeManager.MODE_NIGHT_AUTO}
            context.getSystemService(android.app.UiModeManager::class.java).setApplicationNightMode(platform)
        }
    }

    val themeModeFlow: Flow<AppThemeMode> = dataStore.data.map { prefs ->
        val mode=when (prefs[THEME_KEY]) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            AppThemeMode.DARK.name -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
        mirror.edit().putString("mode",mode.name).apply()
        syncPlatformTheme(appContext,mode)
        mode
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
        mirror.edit().putString("mode",mode.name).apply()
        syncPlatformTheme(appContext,mode)
    }

    val navAnimationStyleFlow:Flow<NavAnimationStyle> = dataStore.data.map{NavAnimationStyle.fromId(it[NAV_ANIMATION_KEY])}.distinctUntilChanged()

    suspend fun setNavAnimationStyle(style:NavAnimationStyle){dataStore.edit{it[NAV_ANIMATION_KEY]=style.id}}
}
