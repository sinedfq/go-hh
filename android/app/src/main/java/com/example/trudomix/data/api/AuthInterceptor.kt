package com.example.trudomix.data.api

import com.example.trudomix.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = runBlocking { tokenManager.getToken() }

        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        val response = chain.proceed(request)
        if (response.code == 401 && token != null && !original.url.encodedPath.startsWith("/api/auth/") && !original.url.encodedPath.startsWith("/api/phone-verification/")) runBlocking { tokenManager.clearSession(token) }
        return response
    }
}
