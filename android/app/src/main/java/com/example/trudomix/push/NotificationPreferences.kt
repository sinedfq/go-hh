package com.example.trudomix.push

import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationPreferences{
    private val value=MutableStateFlow(true)
    val enabled=value.asStateFlow()
    fun isEnabled(context:Context)=context.getSharedPreferences("notification_preferences",Context.MODE_PRIVATE).getBoolean("enabled",true)
    fun initialize(context:Context){value.value=isEnabled(context)}
    fun setEnabled(context:Context,enabled:Boolean){
        context.applicationContext.getSharedPreferences("notification_preferences",Context.MODE_PRIVATE).edit().putBoolean("enabled",enabled).apply()
        value.value=enabled
        if(!enabled)context.getSystemService(NotificationManager::class.java).cancelAll()
    }
}
