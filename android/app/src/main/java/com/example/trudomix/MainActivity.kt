package com.example.trudomix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import com.example.trudomix.data.local.AppThemeMode
import com.example.trudomix.data.local.ThemeManager
import com.example.trudomix.ui.navigation.AppNavGraph
import com.example.trudomix.ui.theme.TrudomixTheme

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.example.trudomix.push.PushNavigation.accept(intent)
    }

    override fun onResume() {
        super.onResume()
        com.example.trudomix.push.DeviceRegistration.sync(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.trudomix.push.PushNavigation.accept(intent)
        enableEdgeToEdge()
        setContent {
            val themeManager = remember { ThemeManager(applicationContext) }
            val themeMode by themeManager.themeModeFlow.collectAsState(initial = ThemeManager.cachedMode(applicationContext))

            val darkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme){com.example.trudomix.data.local.AppIcons.applyTheme(applicationContext,darkTheme)}
            TrudomixTheme(darkTheme = darkTheme) {
                AppNavGraph()
            }
        }
    }
}
