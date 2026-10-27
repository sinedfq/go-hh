package com.example.trudomix

import android.app.Application
import com.example.trudomix.data.api.ApiClient

class TrudomixApp : Application() {
    override fun onConfigurationChanged(newConfig:android.content.res.Configuration){
        super.onConfigurationChanged(newConfig)
        com.example.trudomix.data.local.AppIcons.initialize(this)
    }
    override fun onCreate() {
        super.onCreate()
        com.example.trudomix.push.NotificationPreferences.initialize(this)
        com.example.trudomix.data.local.LoadingStyles.initialize(this)
        com.example.trudomix.data.local.ThemeManager.syncPlatformTheme(this,com.example.trudomix.data.local.ThemeManager.cachedMode(this))
        ApiClient.init(this)
        com.example.trudomix.data.local.AppIcons.initialize(this)
        com.example.trudomix.startup.StartupReadiness.initialize(this)
        com.example.trudomix.push.AppNotifications.createChannel(this)
        com.example.trudomix.push.DeviceRegistration.start(this)
    }
}
