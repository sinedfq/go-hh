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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.example.trudomix.data.local.AppThemeMode
import com.example.trudomix.data.local.ThemeManager
import com.example.trudomix.ui.navigation.AppNavGraph
import com.example.trudomix.ui.theme.TrudomixTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.trudomix.ui.loading.StartupSplashHost
import com.example.trudomix.startup.StartupReadiness
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.graphicsLayer

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
                val startupVisible by StartupReadiness.visible.collectAsStateWithLifecycle()
                val revealing by StartupReadiness.revealing.collectAsStateWithLifecycle()
                val startupAttempt by StartupReadiness.attempt.collectAsStateWithLifecycle()
                var appMounted by remember{mutableStateOf(!startupVisible)}
                LaunchedEffect(Unit){withFrameNanos{};appMounted=true}
                com.example.trudomix.ui.theme.ThemeWaveHost(window,darkTheme){
                Box(Modifier.fillMaxSize()){
                    if(appMounted)Box(Modifier.graphicsLayer{alpha=if(startupVisible&&!revealing)0f else 1f}.then(if(startupVisible)Modifier.clearAndSetSemantics{}else Modifier)){androidx.compose.runtime.key(startupAttempt){AppNavGraph()}}
                    if(!startupVisible)com.example.trudomix.ui.loading.ConnectionBanner()
                    StartupSplashHost(darkTheme,window)
                }
                }
            }
        }
    }
}
