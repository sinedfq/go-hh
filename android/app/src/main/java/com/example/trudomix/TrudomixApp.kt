package com.example.trudomix

import android.app.Application
import com.example.trudomix.data.api.ApiClient

class TrudomixApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        com.example.trudomix.data.local.AppIcons.initialize(this)
        com.example.trudomix.push.AppNotifications.createChannel(this)
        com.example.trudomix.push.DeviceRegistration.start(this)
    }
}
