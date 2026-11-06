package com.example.trudomix

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trudomix.data.api.ApiClient
import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class OfflineSessionTest {
    @Test fun connectionFailureRestoresSavedProfileAndKeepsSession() = runBlocking {
        val oldServer = ApiClient.baseUrl
        val manager = ApiClient.getTokenManager()
        val oldToken = manager.getToken()
        val server = MockWebServer(); server.start()
        val port = server.port
        var replacement: MockWebServer? = null
        try {
            ApiClient.setServer(server.url("/").toString())
            manager.saveToken("offline-test-session")
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = if (request.requestUrl?.encodedPath == "/api/me")
                    MockResponse().setHeader("Content-Type", "application/json").setBody("{\"id\":9001,\"email\":\"offline@example.test\",\"role\":\"candidate\"}")
                else MockResponse().setResponseCode(404)
            }
            assertEquals(9001, ApiClient.api.getMe().body()!!.id)
            ApiClient.snapshots.clearWarm()
            server.shutdown()
            val saved = ApiClient.api.getMe()
            assertTrue(saved.isSuccessful)
            assertEquals(9001, saved.body()!!.id)
            assertEquals("offline-test-session", manager.getToken())
            // A successful reconnect ends the startup-only offline shortcut.
            com.example.trudomix.startup.StartupReadiness.usingCachedData.value = false
            replacement = MockWebServer().also { it.start(port) }
            replacement.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(if(request.requestUrl?.encodedPath == "/api/me")401 else 404)
            }
            assertEquals(401, ApiClient.api.getMe().code())
            assertNull(manager.getToken())
        } finally {
            replacement?.shutdown()
            runCatching { server.shutdown() }
            ApiClient.setServer(oldServer)
            if (oldToken != null) manager.saveToken(oldToken) else manager.clearSession()
            com.example.trudomix.startup.StartupReadiness.usingCachedData.value = false
        }
    }

    @Test fun oldUnauthorizedRequestCannotClearANewerSession() = runBlocking {
        val oldServer = ApiClient.baseUrl
        val manager = ApiClient.getTokenManager()
        val oldToken = manager.getToken()
        val server = MockWebServer(); server.start()
        val releaseResponse = CountDownLatch(1)
        val meRequested = CountDownLatch(1)
        try {
            ApiClient.setServer(server.url("/").toString())
            manager.saveToken("previous-session")
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if(request.requestUrl?.encodedPath != "/api/me")return MockResponse().setResponseCode(404)
                    meRequested.countDown()
                    releaseResponse.await(10, TimeUnit.SECONDS)
                    return MockResponse().setResponseCode(401)
                }
            }
            val request = async(Dispatchers.IO) { ApiClient.api.getMe() }
            assertTrue(meRequested.await(5, TimeUnit.SECONDS))
            manager.saveToken("new-session")
            releaseResponse.countDown()
            assertEquals(401, request.await().code())
            assertEquals("new-session", manager.getToken())
        } finally {
            releaseResponse.countDown()
            server.shutdown()
            ApiClient.setServer(oldServer)
            if (oldToken != null) manager.saveToken(oldToken) else manager.clearSession()
            com.example.trudomix.startup.StartupReadiness.usingCachedData.value = false
        }
    }
}
