package com.example.trudomix

import com.example.trudomix.data.api.OfflineReadInterceptor
import com.example.trudomix.data.local.ResponseSnapshot
import com.example.trudomix.data.local.ResponseSnapshots
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class OfflineReadsTest {
    private fun directory() = Files.createTempDirectory("trudomix-offline").toFile()
    private fun response(body: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test fun `offline models survive a cache instance restart and a failed network`() {
        val root = directory()
        val server = MockWebServer(); server.start()
        val request = Request.Builder().url(server.url("/api/me")).header("Authorization", "Bearer first-session").build()
        val first = OkHttpClient.Builder().addInterceptor(OfflineReadInterceptor(ResponseSnapshots(root))).build()
        server.enqueue(response("{\"id\":7}"))
        first.newCall(request).execute().use { assertEquals(200, it.code); assertEquals("{\"id\":7}", it.body!!.string()) }
        assertTrue(root.listFiles()!!.single().readText().contains("savedAt"))
        server.shutdown()
        var fallback = false
        val restored = OkHttpClient.Builder().retryOnConnectionFailure(false).addInterceptor(OfflineReadInterceptor(ResponseSnapshots(root), onFallback = { fallback = true })).build()
        restored.newCall(request).execute().use {
            assertEquals(200, it.code); assertEquals("offline", it.header("X-Trudomix-Cache")); assertEquals("{\"id\":7}", it.body!!.string())
        }
        assertTrue(fallback)
        root.deleteRecursively()
    }

    @Test fun `server outage preserves a model but authorization failures do not use it`() {
        val root = directory(); val server = MockWebServer(); server.start()
        try {
            val client = OkHttpClient.Builder().addInterceptor(OfflineReadInterceptor(ResponseSnapshots(root))).build()
            val request = Request.Builder().url(server.url("/api/me")).header("Authorization", "Bearer session").build()
            server.enqueue(response("{\"id\":9}")); client.newCall(request).execute().close()
            server.enqueue(response("down", 503))
            client.newCall(request).execute().use { assertEquals(200, it.code); assertEquals("offline", it.header("X-Trudomix-Cache")) }
            server.enqueue(response("expired", 401))
            client.newCall(request).execute().use { assertEquals(401, it.code); assertNull(it.header("X-Trudomix-Cache")) }
            server.enqueue(response("down", 503))
            client.newCall(request.newBuilder().header("Authorization", "Bearer other-session").build()).execute().use { assertEquals(503, it.code) }
        } finally { server.shutdown(); root.deleteRecursively() }
    }

    @Test fun `otp and writes cannot be replayed from a saved response`() {
        val root = directory(); val server = MockWebServer(); server.start()
        try {
            val cache = ResponseSnapshots(root)
            val client = OkHttpClient.Builder().addInterceptor(OfflineReadInterceptor(cache)).build()
            for (path in listOf("/api/phone-verification/config", "/api/conversations/1/typing", "/api/devices/status")) {
                val request = Request.Builder().url(server.url(path)).build()
                server.enqueue(response("{\"enabled\":true}")); client.newCall(request).execute().close()
                server.enqueue(response("down", 503)); client.newCall(request).execute().use { assertEquals(503, it.code) }
            }
            val write = Request.Builder().url(server.url("/api/posts")).post("{}".toRequestBody()).build()
            server.enqueue(response("{\"id\":1}")); client.newCall(write).execute().close()
            server.enqueue(response("down", 503)); client.newCall(write).execute().use { assertEquals(503, it.code) }
        } finally { server.shutdown(); root.deleteRecursively() }
    }

    @Test fun `warm reads are reused once and explicit refresh bypasses them`() {
        val root = directory(); val cache = ResponseSnapshots(root); val server = MockWebServer(); server.start()
        var warming = true
        try {
            val client = OkHttpClient.Builder().addInterceptor(OfflineReadInterceptor(cache, warming = { warming })).build()
            val request = Request.Builder().url(server.url("/api/favorites")).build()
            server.enqueue(response("[1]")); client.newCall(request).execute().close()
            warming = false
            client.newCall(request).execute().use { assertEquals("startup", it.header("X-Trudomix-Cache")) }
            server.enqueue(response("[2]")); client.newCall(request).execute().use { assertEquals("[2]", it.body!!.string()) }
            warming = true
            server.enqueue(response("[3]")); client.newCall(request).execute().close()
            cache.clearWarm()
            server.enqueue(response("[4]")); client.newCall(request).execute().use { assertEquals("[4]", it.body!!.string()) }
        } finally { server.shutdown(); root.deleteRecursively() }
    }

    @Test fun `logout invalidates writes in flight and disk cache is bounded and expires`() {
        val root = directory(); val cache = ResponseSnapshots(root, maxBytes = 300)
        val generation = cache.generation
        cache.write("first", ResponseSnapshot("a".repeat(150), "application/json"), generation, false)
        cache.write("second", ResponseSnapshot("b".repeat(150), "application/json"), generation, false)
        assertTrue(root.listFiles()!!.sumOf { it.length() } <= 300)
        assertNull(cache.read("second", System.currentTimeMillis() + 8L * 24 * 60 * 60 * 1000))
        cache.clear()
        cache.write("late", ResponseSnapshot("old account", "application/json"), generation, true)
        assertNull(cache.read("late"))
        assertNotEquals(cache.key("server", "one", "/api/me"), cache.key("server", "two", "/api/me"))
        assertNotEquals(cache.key("one", "token", "/api/me"), cache.key("two", "token", "/api/me"))
        root.deleteRecursively()
    }

    @Test fun `recommendation refresh bypasses warm reads and replaces the same offline snapshot`() {
        val root=directory();val server=MockWebServer();server.start()
        try {
            val cache=ResponseSnapshots(root)
            val client=OkHttpClient.Builder().addInterceptor(OfflineReadInterceptor(cache,warming={true})).build()
            val normal=Request.Builder().url(server.url("/api/recommendations?refresh=false")).build()
            server.enqueue(response("{\"hidden_count\":0}"));client.newCall(normal).execute().close()
            val refresh=normal.newBuilder().url(server.url("/api/recommendations?refresh=true")).build()
            server.enqueue(response("{\"hidden_count\":2}"))
            client.newCall(refresh).execute().use{assertNull(it.header("X-Trudomix-Cache"));assertEquals("{\"hidden_count\":2}",it.body!!.string())}
            client.newCall(normal).execute().use{assertEquals("startup",it.header("X-Trudomix-Cache"));assertEquals("{\"hidden_count\":2}",it.body!!.string())}
            assertEquals(1,root.listFiles()!!.count{!it.name.endsWith(".tmp")})
        }finally{server.shutdown();root.deleteRecursively()}
    }
}
