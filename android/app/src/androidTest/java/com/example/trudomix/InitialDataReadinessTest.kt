package com.example.trudomix

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.local.SessionIdentity
import com.example.trudomix.startup.InitialDataViewModel
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class InitialDataReadinessTest {
    @Test fun loaderWaitsForCatalogPagesAndAnImageOutsideTheViewport() {
        val server = MockWebServer()
        val imageRequested = CountDownLatch(1)
        val releaseImage = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val url = request.requestUrl!!
                fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
                return when (url.encodedPath) {
                    "/api/me" -> json("{\"id\":9002,\"email\":\"readiness@example.test\",\"role\":\"candidate\"}")
                    "/api/vacancies/search" -> {
                        val offset = url.queryParameter("offset")?.toInt() ?: 0
                        val limit = url.queryParameter("limit")?.toInt() ?: 20
                        val jobs = (offset + 1..minOf(offset + limit, 40)).joinToString(",") { "{\"id\":$it,\"title\":\"Job $it\",\"company_logo\":\"/uploads/readiness.png\"}" }
                        json("{\"vacancies\":[$jobs],\"total_count\":40,\"limit\":$limit,\"offset\":$offset}")
                    }
                    "/api/feed" -> json("{\"posts\":[]}")
                    "/api/my-resume" -> json("{}").setResponseCode(404)
                    "/api/my-resume/stats" -> json("{\"total_views\":0}")
                    "/uploads/readiness.png" -> {
                        imageRequested.countDown()
                        releaseImage.await(15, TimeUnit.SECONDS)
                        val png = android.util.Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aMh8AAAAASUVORK5CYII=", 0)
                        MockResponse().setHeader("Content-Type", "image/png").setBody(Buffer().write(png))
                    }
                    else -> json("[]")
                }
            }
        }
        server.start()
        val manager = ApiClient.getTokenManager()
        val oldServer = ApiClient.baseUrl
        val oldToken = runBlocking { manager.getToken() }
        val oldIdentity = SessionIdentity.user.value
        val store = ViewModelStore()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var model: InitialDataViewModel
        try {
            runBlocking { ApiClient.setServer(server.url("/").toString()); manager.saveToken("readiness-test") }
            SessionIdentity.user.value = null
            com.example.trudomix.startup.StartupReadiness.usingCachedData.value = false
            instrumentation.runOnMainSync { model = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[InitialDataViewModel::class.java] }
            assertTrue("Offscreen image must be requested during preload", imageRequested.await(15, TimeUnit.SECONDS))
            assertTrue("Loader must remain while the image is unavailable", model.loading.value)
            assertEquals(40, model.catalog!!.vacancies.size)
            releaseImage.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (model.loading.value && System.nanoTime() < deadline) Thread.sleep(20)
            assertFalse("Loading completes after data and image settle", model.loading.value)
        } finally {
            releaseImage.countDown()
            instrumentation.runOnMainSync { store.clear() }
            runBlocking { ApiClient.setServer(oldServer); if (oldToken != null) manager.saveToken(oldToken) else manager.clearSession() }
            SessionIdentity.user.value = oldIdentity
            server.shutdown()
        }
    }
}
