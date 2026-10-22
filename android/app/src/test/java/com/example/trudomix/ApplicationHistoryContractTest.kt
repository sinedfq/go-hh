package com.example.trudomix

import com.example.trudomix.data.models.ApplicationEvent
import com.example.trudomix.data.models.JobApplication
import com.example.trudomix.ui.screens.applications.applicationEventDate
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class ApplicationHistoryContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun currentDetailsAndLegacyEventsDecode() {
        val app = json.decodeFromString<JobApplication>("""{"id":8,"vacancy_id":1,"status":"accepted","feedback":"Встреча с командой","interview_at":"2026-11-01T03:00:00Z"}""")
        assertEquals("Встреча с командой", app.feedback)
        assertEquals("2026-11-01T03:00:00Z", app.interview_at)
        val legacy = json.decodeFromString<ApplicationEvent>("""{"id":1,"status":"new","created_at":null}""")
        assertNull(legacy.created_at)
        assertEquals("", legacy.feedback)
    }

    @Test fun meetingTimeUsesDeviceZoneWithoutInventingLegacyDates() {
        val date = applicationEventDate("2026-11-01T03:00:00Z", ZoneId.of("Asia/Novosibirsk"))
        assertTrue(date.endsWith("10:00"))
        assertEquals("Дата не указана", applicationEventDate(null))
        assertEquals("invalid", applicationEventDate("invalid"))
    }
}
