package com.example.trudomix

import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.push.AppNotifications
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class NotificationDisplayTest {
    @Test fun richMessagesAndResumeViewsHaveSeparateChannelsAndPrivatePreviews() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppNotifications.createChannel(context)
        assumeTrue(AppNotifications.allowed(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        val chat = mapOf("conversation_id" to "987654318", "sender_name" to "Команда Trudomix", "type" to "message")
        val view = mapOf("resume_id" to "987654317", "viewer_user_id" to "987654316", "type" to "resume_viewed")
        val chatId = com.example.trudomix.push.notificationKey(chat, null).hashCode()
        val viewId = com.example.trudomix.push.notificationKey(view, null).hashCode()
        fun notification(id: Int): Notification {
            val deadline = System.currentTimeMillis() + 3000
            while (manager.activeNotifications.none { it.id == id } && System.currentTimeMillis() < deadline) Thread.sleep(50)
            return manager.activeNotifications.single { it.id == id }.notification
        }
        try {
            assertTrue(AppNotifications.show(context, "Команда Trudomix", "Давайте познакомимся", chat))
            val message = notification(chatId)
            assertEquals(AppNotifications.MESSAGES_CHANNEL, message.channelId)
            val style = androidx.core.app.NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(message)!!
            assertEquals("Давайте познакомимся", style.messages.last().text.toString())
            assertEquals("Команда Trudomix", style.messages.last().person!!.name.toString())
            assertNotNull("The message sender must display the application logo", style.messages.last().person!!.icon)
            assertEquals(R.drawable.ic_notification, message.smallIcon.resId)
            assertEquals(Notification.VISIBILITY_PRIVATE, message.visibility)
            assertEquals("Новое сообщение", message.publicVersion.extras.getString(Notification.EXTRA_TEXT))
            assertTrue(AppNotifications.show(context, "Ваше резюме просмотрели", "Компания посмотрела резюме", view))
            val resume = notification(viewId)
            assertEquals(AppNotifications.RESUMES_CHANNEL, resume.channelId)
            assertEquals("Моё резюме", resume.actions.first().title.toString())
            assertEquals(NotificationManager.IMPORTANCE_LOW, manager.getNotificationChannel(AppNotifications.RESUMES_CHANNEL).importance)
            assertNotNull(resume.contentIntent)
        } finally {
            manager.cancel(chatId)
            manager.cancel(viewId)
            AppNotifications.refreshSummary(context)
        }
    }
    @Test fun visibleChatClearsItsNotificationAndOtherChatsStillNotify() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppNotifications.createChannel(context)
        assumeTrue("Enable notification permission before this display test", AppNotifications.allowed(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        val active = 987654320
        val other = 987654319
        fun send(id: Int) = AppNotifications.show(context, "Новое сообщение", "Откройте чат", mapOf("conversation_id" to id.toString()))
        fun shown(id: Int) = manager.activeNotifications.any { it.id == "conversation-$id".hashCode() }
        fun awaitShown(id: Int, expected: Boolean) {
            val deadline = System.currentTimeMillis() + 3000
            while (shown(id) != expected && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertEquals(expected, shown(id))
        }
        try {
            assertTrue(send(active))
            awaitShown(active, true)
            AppNotifications.openConversation(context, active)
            assertFalse(send(active))
            awaitShown(active, false)
            assertTrue(send(other))
            awaitShown(other, true)
            // A screen finishing its transition must not clear the current chat.
            AppNotifications.closeConversation(other)
            assertFalse(send(active))
            // Backgrounding or leaving the chat enables its notifications again.
            AppNotifications.closeConversation(active)
            assertTrue(send(active))
            awaitShown(active, true)
        } finally {
            AppNotifications.closeConversation(active)
            manager.cancel("conversation-$active".hashCode())
            manager.cancel("conversation-$other".hashCode())
        }
    }
    @Test fun messageNotificationUsesChannelBodyAndNavigationIntent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Enable notification permission before this display test", AppNotifications.allowed(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        val conversationId = "987654321"
        val id = "conversation-$conversationId".hashCode()
        try {
            assertTrue(AppNotifications.show(context,"Проверка сообщения","Тест отображения сообщения",mapOf("conversation_id" to conversationId)))
            val deadline = System.currentTimeMillis() + 3000
            while (manager.activeNotifications.none { it.id == id } && System.currentTimeMillis() < deadline) Thread.sleep(50)
            val notification = manager.activeNotifications.single { it.id == id }.notification
            assertEquals(AppNotifications.MESSAGES_CHANNEL, notification.channelId)
            assertEquals("Тест отображения сообщения", notification.extras.getString(Notification.EXTRA_TEXT))
            assertEquals(R.drawable.ic_notification, notification.smallIcon.resId)
            assertNotNull(notification.getLargeIcon())
            assertTrue(notification.actions.any{it.title.toString()=="Открыть чат"})
            assertNotNull(notification.contentIntent)
        } finally { manager.cancel(id) }
    }
}
