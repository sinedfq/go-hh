package com.example.trudomix.push

import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationPreferences{
    private val value=MutableStateFlow(true)
    val enabled=value.asStateFlow()
    private val eventValues=MutableStateFlow<Map<NotificationEvent,Boolean>>(emptyMap())
    val events=eventValues.asStateFlow()
    fun eventEnabled(context:Context,event:NotificationEvent)=context.getSharedPreferences("notification_preferences",Context.MODE_PRIVATE).getBoolean("event_${event.name}",true)
    fun isEnabled(context:Context)=context.getSharedPreferences("notification_preferences",Context.MODE_PRIVATE).getBoolean("enabled",true)
    fun initialize(context:Context){value.value=isEnabled(context);eventValues.value=NotificationEvent.entries.associateWith{eventEnabled(context,it)}}
    fun setEnabled(context:Context,enabled:Boolean){
        synchronized(AppNotifications){
        context.applicationContext.getSharedPreferences("notification_preferences",Context.MODE_PRIVATE).edit().putBoolean("enabled",enabled).apply()
        value.value=enabled
        if(!enabled){val manager=context.getSystemService(NotificationManager::class.java);manager.activeNotifications.filter{it.notification.channelId!=OtpNotifications.CHANNEL}.forEach{manager.cancel(it.id)}}
        }
    }
    fun setEvent(context:Context,event:NotificationEvent,enabled:Boolean){
        synchronized(AppNotifications){
            context.getSharedPreferences("notification_preferences",Context.MODE_PRIVATE).edit().putBoolean("event_${event.name}",enabled).apply()
            eventValues.value=NotificationEvent.entries.associateWith{eventEnabled(context,it)}
            if(!enabled){val manager=context.getSystemService(NotificationManager::class.java);manager.activeNotifications.filter{it.notification.extras.getString("trudomix_event")==event.name}.forEach{manager.cancel(it.id)};AppNotifications.refreshSummary(context)}
        }
    }
}
