package com.example.trudomix

import android.app.Application
import com.example.trudomix.data.api.ApiClient

class TrudomixApp : Application(), coil.ImageLoaderFactory {
    override fun newImageLoader():coil.ImageLoader=coil.ImageLoader.Builder(this)
        .memoryCache{coil.memory.MemoryCache.Builder(this).maxSizePercent(.12).build()}
        .diskCache{coil.disk.DiskCache.Builder().directory(cacheDir.resolve("media-images")).maxSizeBytes(96L*1024*1024).build()}
        .respectCacheHeaders(false).build()
    override fun onConfigurationChanged(newConfig:android.content.res.Configuration){
        super.onConfigurationChanged(newConfig)
        com.example.trudomix.data.local.AppIcons.initialize(this)
    }
    override fun onCreate() {
        super.onCreate()
        com.example.trudomix.push.NotificationPreferences.initialize(this)
        com.example.trudomix.data.local.LoadingStyles.initialize(this)
        com.example.trudomix.data.local.AccentColors.initialize(this)
        com.example.trudomix.data.local.VisualPerformance.initialize(this)
        com.example.trudomix.data.local.ThemeManager.syncPlatformTheme(this,com.example.trudomix.data.local.ThemeManager.cachedMode(this))
        ApiClient.init(this)
        com.example.trudomix.data.local.AppIcons.initialize(this)
        com.example.trudomix.startup.StartupReadiness.initialize(this)
        com.example.trudomix.push.AppNotifications.createChannel(this)
        com.example.trudomix.push.DeviceRegistration.start(this)
    }
}
