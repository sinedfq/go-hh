package com.example.trudomix

import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.push.AppNotifications
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class NotificationDisplayTest {
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
