package com.example.trudomix

import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.push.AppNotifications
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class NotificationDisplayTest {
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
            assertEquals(AppNotifications.CHANNEL, notification.channelId)
            assertEquals("Тест отображения сообщения", notification.extras.getString(Notification.EXTRA_TEXT))
            assertEquals(R.drawable.ic_notification, notification.smallIcon.resId)
            assertNotNull(notification.getLargeIcon())
            assertTrue(notification.actions.any{it.title.toString()=="Открыть чат"})
            assertNotNull(notification.contentIntent)
        } finally { manager.cancel(id) }
    }
}
