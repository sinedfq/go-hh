package com.example.trudomix

import com.example.trudomix.data.api.*
import com.example.trudomix.data.models.Resume
import com.example.trudomix.ui.screens.profile.resumeCompleteness
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class UserErrorsTest {
    @Test fun badPasswordNeverShowsRawStatus() {
        for(status in listOf(401,403)) {
            val text=httpErrorMessage(status,authentication=true)
            assertTrue(text.contains("Неверная почта или пароль"));assertFalse(text.contains(status.toString()))
        }
    }
    @Test fun registrationAndNetworkErrorsHaveActionableMessages() {
        assertTrue(httpErrorMessage(409,registration=true).contains("уже существует"))
        assertTrue(userErrorMessage(SocketTimeoutException()).contains("вовремя"))
        assertTrue(userErrorMessage(UnknownHostException("private.internal.host")).contains("соединения"))
        assertFalse(userErrorMessage(UnknownHostException("private.internal.host")).contains("private.internal"))
        assertTrue(httpErrorMessage(429).contains("Подожди"))
    }
    @Test fun profileCompletenessUsesRealFields() {
        assertEquals(0,resumeCompleteness(Resume()))
        assertEquals(8,resumeCompleteness(Resume(full_name="Имя",desired_position="Android",city="Город",experience="junior",skills=listOf("Kotlin"),about_me="О себе",phone="123",photo_url="/uploads/me.jpg")))
    }
}
