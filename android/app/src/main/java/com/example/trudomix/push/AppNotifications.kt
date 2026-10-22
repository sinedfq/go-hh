package com.example.trudomix.push

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.trudomix.MainActivity
import com.example.trudomix.R
import androidx.core.graphics.drawable.toBitmap
import com.example.trudomix.data.local.AppIcons

object AppNotifications {
    const val CHANNEL = "trudomix_events"
    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Сообщения и отклики", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Новые сообщения и ответы работодателей"; enableVibration(true)
            })
    }
    fun allowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    fun show(context: Context, title: String, body: String, data: Map<String, String>, messageId: String? = null): Boolean {
        createChannel(context)
        if (!allowed(context)) return false
        val id = (data["conversation_id"]?.let { "conversation-$it" } ?: messageId ?: data.toString()).hashCode()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body)
            .setLargeIcon(ContextCompat.getDrawable(context, AppIcons.current(context).resource)!!.toBitmap(192,192))
            .addAction(R.drawable.ic_notification, if(data.containsKey("conversation_id"))"Открыть чат" else "Открыть приложение", pending)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(pending).build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
        return true
    }
}
