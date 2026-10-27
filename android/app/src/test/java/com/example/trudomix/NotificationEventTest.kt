package com.example.trudomix

import com.example.trudomix.push.*
import org.junit.Assert.*
import org.junit.Test

class NotificationEventTest{
    @Test fun invitationsAndUpdatesHaveIndependentPreferences(){
        assertEquals(NotificationEvent.INVITATIONS,notificationEvent(mapOf("type" to "application_accepted")))
        for(type in listOf("application_rejected","application_viewed","application_updated"))assertEquals(NotificationEvent.APPLICATION_UPDATES,notificationEvent(mapOf("type" to type)))
        assertEquals(NotificationEvent.NEW_APPLICATIONS,notificationEvent(mapOf("type" to "new_application")))
    }
    @Test fun messagesResumeViewsAndOtherEventsAreSeparated(){
        assertEquals(NotificationEvent.MESSAGES,notificationEvent(mapOf("conversation_id" to "3")))
        assertEquals(NotificationEvent.RESUME_VIEWS,notificationEvent(mapOf("type" to "resume_viewed")))
        assertEquals(NotificationEvent.OTHER,notificationEvent(mapOf("type" to "unknown")))
    }
}
