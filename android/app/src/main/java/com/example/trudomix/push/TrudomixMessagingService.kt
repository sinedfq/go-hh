package com.example.trudomix.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.trudomix.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TrudomixMessagingService:FirebaseMessagingService(){
    override fun onNewToken(token:String){DeviceRegistration.sync(this)}
    override fun onMessageReceived(message:RemoteMessage){
        AppNotifications.show(this, message.notification?.title ?: message.data["title"] ?: "Trudomix",
            message.notification?.body ?: message.data["body"] ?: "Новое уведомление", message.data, message.messageId)
    }
}
