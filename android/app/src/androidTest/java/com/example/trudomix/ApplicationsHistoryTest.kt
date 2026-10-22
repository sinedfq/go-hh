package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.ui.screens.applications.ApplicationsScreen
import com.example.trudomix.ui.theme.TrudomixTheme
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ApplicationsHistoryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun detailsAndLazyHistorySupportRetryAndCaching() {
        val historyCalls = AtomicInteger()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when (request.requestUrl!!.encodedPath) {
                    "/api/my-applications" -> """[{"id":8,"vacancy_id":1,"vacancy_title":"Android-инженер","company_name":"Команда","status":"accepted","feedback":"Приходи знакомиться","interview_at":"2026-11-01T03:00:00Z","created_at":"2026-10-22T10:00:00Z"}]"""
                    "/api/applications/8/history" -> {
                        if (historyCalls.incrementAndGet() == 1) return MockResponse().setResponseCode(503).setBody("{}")
                        """[{"id":1,"status":"new","feedback":"Первый этап","created_at":null},{"id":2,"status":"accepted","feedback":"Разговор с командой","interview_at":"2026-11-01T03:00:00Z","created_at":"2026-10-22T10:00:00Z"}]"""
                    }
                    else -> "[]"
                }
                return MockResponse().setHeader("Content-Type", "application/json").setBody(body)
            }
        }
        server.start()
        val oldServer = ApiClient.baseUrl
        val oldToken = runBlocking { ApiClient.getTokenManager().getToken() }
        var active by mutableStateOf(true)
        fun waitText(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
        try {
            runBlocking { ApiClient.setServer(server.url("/").toString()); ApiClient.getTokenManager().saveToken("preview-token") }
            compose.setContent { TrudomixTheme(darkTheme = true) { if (active) ApplicationsScreen() } }
            waitText("Приходи знакомиться")
            compose.onNodeWithText("Приходи знакомиться").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Собеседование").performScrollTo().assertIsDisplayed()
            assertEquals(0, historyCalls.get())
            compose.onNodeWithText("История отклика").performScrollTo().performClick()
            waitText("Повторить загрузку истории")
            compose.onNodeWithText("Повторить загрузку истории").performScrollTo().performClick()
            waitText("Разговор с командой")
            compose.onNodeWithText("Разговор с командой").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Дата не указана").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Скрыть историю").performScrollTo().performClick()
            compose.onNodeWithText("История отклика").performClick()
            compose.waitForIdle()
            assertEquals(2, historyCalls.get())
        } finally {
            compose.runOnIdle { active = false }
            runBlocking { ApiClient.setServer(oldServer); if (oldToken != null) ApiClient.getTokenManager().saveToken(oldToken) else ApiClient.getTokenManager().clearSession() }
            server.shutdown()
        }
    }
}
