package com.example.trudomix.push

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.IconCompat
import com.example.trudomix.MainActivity
import com.example.trudomix.R
import com.example.trudomix.data.local.AppIcons

object AppNotifications {
    const val CHANNEL = "trudomix_events"
    const val MESSAGES_CHANNEL = "trudomix_messages"
    const val RESUMES_CHANNEL = "trudomix_resume_views"
    const val APPLICATIONS_CHANNEL = "trudomix_applications"
    private const val GROUP = "trudomix_activity"
    private const val SUMMARY_ID = 0x54524D58
    @Volatile private var activeConversation: Int? = null

    private fun conversationNotificationId(id: Int) = "conversation-$id".hashCode()

    @Synchronized fun openConversation(context: Context, id: Int) {
        activeConversation = id
        context.getSystemService(NotificationManager::class.java).cancel(conversationNotificationId(id))
        updateSummary(context)
    }

    @Synchronized fun closeConversation(id: Int) {
        if (activeConversation == id) activeConversation = null
    }

    @Synchronized fun refreshSummary(context: Context) = updateSummary(context)

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // Preserve an explicit opt-out of the original shared channel on upgrade.
        val disabled = manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE
        fun channel(id: String, name: String, importance: Int, description: String) =
            NotificationChannel(id, name, if (disabled) NotificationManager.IMPORTANCE_NONE else importance).apply { this.description = description }
        manager.createNotificationChannels(listOf(
            channel(CHANNEL, "Другие события", NotificationManager.IMPORTANCE_LOW, "Обновления вакансий и сводка событий"),
            channel(MESSAGES_CHANNEL, "Сообщения", NotificationManager.IMPORTANCE_HIGH, "Новые сообщения от собеседников"),
            channel(RESUMES_CHANNEL, "Просмотры резюме", NotificationManager.IMPORTANCE_LOW, "Работодатели, заинтересовавшиеся вашим резюме"),
            channel(APPLICATIONS_CHANNEL, "Отклики и приглашения", NotificationManager.IMPORTANCE_DEFAULT, "Ответы работодателей и собеседования")
        ))
    }

    fun allowed(context: Context, channel: String? = null): Boolean {
        if (!NotificationPreferences.isEnabled(context)) return false
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        return if (channel != null) manager.getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE
        else listOf(CHANNEL, MESSAGES_CHANNEL, RESUMES_CHANNEL, APPLICATIONS_CHANNEL).any { manager.getNotificationChannel(it)?.importance?.let { value -> value != NotificationManager.IMPORTANCE_NONE } == true }
    }

    @Synchronized fun show(context: Context, title: String, body: String, data: Map<String, String>, messageId: String? = null): Boolean {
        createChannel(context)
        val presentation = notificationPresentation(data)
        if (!allowed(context, presentation.channel)) return false
        val conversation = data["conversation_id"]?.toIntOrNull()?.takeIf { it > 0 }
        if (conversation != null && conversation == activeConversation) return false
        val id = notificationKey(data, messageId).hashCode()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val dismissed = PendingIntent.getBroadcast(context, id, Intent(context, NotificationDismissReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = context.getSystemService(NotificationManager::class.java)
        val appIcon = ContextCompat.getDrawable(context, AppIcons.current(context).resource)!!.toBitmap(192,192)
        val builder = NotificationCompat.Builder(context, presentation.channel)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body)
            .setLargeIcon(appIcon)
            .setSubText(presentation.label).setColor(0xFF0066CC.toInt())
            .addAction(R.drawable.ic_notification, presentation.action, pending)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true)
            .setCategory(if (conversation != null) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(if (conversation != null) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(GROUP).setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(pending).setDeleteIntent(dismissed)
            .setPublicVersion(NotificationCompat.Builder(context, presentation.channel)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("Trudomix")
                .setContentText(presentation.label).setContentIntent(pending).build())
        if (conversation != null && !data["sender_name"].isNullOrBlank()) {
            val sender = Person.Builder().setName(data["sender_name"]).setIcon(IconCompat.createWithBitmap(appIcon)).build()
            val previous = manager.activeNotifications.firstOrNull { it.id == id }?.notification
                ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
            val style = NotificationCompat.MessagingStyle(Person.Builder().setName("Вы").build()).setGroupConversation(false)
            previous?.messages?.takeLast(4)?.forEach { style.addMessage(it) }
            style.addMessage(body, System.currentTimeMillis(), sender)
            builder.setStyle(style)
        }
        manager.notify(id, builder.build())
        updateSummary(context)
        return true
    }

    private fun updateSummary(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val children = manager.activeNotifications.filter { it.id != SUMMARY_ID && it.notification.group == GROUP }
        if (children.size < 2 || !allowed(context, CHANNEL)) { manager.cancel(SUMMARY_ID); return }
        val inbox = NotificationCompat.InboxStyle().setBigContentTitle("События Trudomix")
        children.sortedByDescending { it.postTime }.take(5).forEach { inbox.addLine(it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)) }
        val open = PendingIntent.getActivity(context, SUMMARY_ID, Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(SUMMARY_ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Trudomix")
            .setContentText("Новые сообщения и события: ${children.size}")
            .setStyle(inbox).setGroup(GROUP).setGroupSummary(true).setSilent(true).setContentIntent(open)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN).setAutoCancel(true).build())
    }
}
