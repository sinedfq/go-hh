package com.example.trudomix.data.api

import com.example.trudomix.data.local.ResponseSnapshot
import com.example.trudomix.data.local.ResponseSnapshots
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody

/** Only read models can fall back. Auth, OTP, live typing and writes always require the server. */
class OfflineReadInterceptor(
    private val snapshots: ResponseSnapshots,
    private val warming: () -> Boolean = { false },
    private val offlineStartup: () -> Boolean = { false },
    private val onFallback: () -> Unit = {},
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val eligible = request.method == "GET" && path.startsWith("/api/") &&
            !path.startsWith("/api/auth/") && !path.startsWith("/api/phone-verification/") &&
            !path.endsWith("/typing") && !path.endsWith("/unread-count") && !path.endsWith("/pdf") && !path.startsWith("/api/devices")
        if (!eligible) {
            val response = chain.proceed(request)
            if (request.method != "GET" && response.isSuccessful && !path.startsWith("/api/devices") && !path.startsWith("/api/phone-verification/") && !path.endsWith("/typing")) snapshots.clearWarm()
            return response
        }
        val generation = snapshots.generation
        val key = responseSnapshotKey(snapshots, request)
        val cached = snapshots.read(key)
        fun restored(stale: Boolean): Response {
            if (stale) onFallback()
            return Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(cached!!.code)
                .message("Saved response").header("X-Trudomix-Cache", if (stale) "offline" else "startup")
                .header("Content-Type", cached.mediaType).body(cached.body.toResponseBody(cached.mediaType.toMediaTypeOrNull())).build()
        }
        val forceRefresh=path=="/api/recommendations" && request.url.queryParameter("refresh")=="true"
        if (!forceRefresh && cached != null && (snapshots.takeWarm(key, consume = !warming()) || warming() && offlineStartup())) return restored(offlineStartup())
        val response = try {
            chain.withConnectTimeout(5, TimeUnit.SECONDS).withReadTimeout(8, TimeUnit.SECONDS).proceed(request)
        } catch (failure: IOException) {
            if (cached == null || chain.call().isCanceled()) throw failure
            return restored(true)
        }
        if (response.code in 500..599 && cached != null) { response.close(); return restored(true) }
        if ((response.code == 200 || response.code == 404 && path == "/api/my-resume") && response.header("Content-Type").orEmpty().contains("json")) {
            val body = response.peekBody(2L * 1024 * 1024 + 1)
            if (body.contentLength() <= 2L * 1024 * 1024) snapshots.write(key, ResponseSnapshot(body.string(), response.header("Content-Type")!!, response.code), generation, warming())
        }
        return response
    }
}

internal fun responseSnapshotKey(snapshots:ResponseSnapshots,request:Request):String {
    val url=if(request.url.encodedPath=="/api/recommendations")request.url.newBuilder().removeAllQueryParameters("refresh").build()else request.url
    return snapshots.key(url.scheme+"://"+url.host+":"+url.port,request.header("Authorization"),url.toString())
}
