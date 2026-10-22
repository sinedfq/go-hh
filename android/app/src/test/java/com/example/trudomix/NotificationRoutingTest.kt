package com.example.trudomix

import com.example.trudomix.push.*
import org.junit.Assert.*
import org.junit.Test

class NotificationRoutingTest {
    @Test fun eventsOpenTheRelevantScreen() {
        assertEquals("chat_detail/42", notificationTarget(mapOf("conversation_id" to "42", "application_id" to "9")))
        assertEquals("resume", notificationTarget(mapOf("type" to "resume_viewed", "resume_id" to "3")))
        assertEquals("applications", notificationTarget(mapOf("type" to "application_accepted", "application_id" to "9")))
        assertEquals("employer", notificationTarget(mapOf("type" to "new_application", "application_id" to "9")))
        assertEquals("vacancy_detail/7", notificationTarget(mapOf("vacancy_id" to "7")))
        assertNull(notificationTarget(mapOf("conversation_id" to "-1", "vacancy_id" to "invalid")))
    }
    @Test fun channelsAndActionsSeparateMessagesResumeViewsAndApplications() {
        assertEquals(AppNotifications.MESSAGES_CHANNEL, notificationPresentation(mapOf("conversation_id" to "42")).channel)
        assertEquals(AppNotifications.RESUMES_CHANNEL, notificationPresentation(mapOf("type" to "resume_viewed")).channel)
        assertEquals("Моё резюме", notificationPresentation(mapOf("type" to "resume_viewed")).action)
        assertEquals(AppNotifications.APPLICATIONS_CHANNEL, notificationPresentation(mapOf("type" to "application_viewed")).channel)
        assertEquals(AppNotifications.CHANNEL, notificationPresentation(emptyMap()).channel)
    }
    @Test fun eventsReplaceTheSameThreadButKeepDifferentEmployers() {
        assertEquals(notificationKey(mapOf("conversation_id" to "42"), "first"), notificationKey(mapOf("conversation_id" to "42"), "second"))
        assertNotEquals(notificationKey(mapOf("conversation_id" to "42"), null), notificationKey(mapOf("conversation_id" to "43"), null))
        val view = mapOf("type" to "resume_viewed", "resume_id" to "3", "viewer_user_id" to "7")
        assertEquals(notificationKey(view, "first"), notificationKey(view, "second"))
        assertNotEquals(notificationKey(view, null), notificationKey(view + ("viewer_user_id" to "8"), null))
    }
}
