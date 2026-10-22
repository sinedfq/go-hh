package com.example.trudomix

import com.example.trudomix.data.api.TrudomixApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class PhotoUploadContractTest {
    @Test fun profileAndResumeUseExistingMultipartEndpoints() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
                .build().create(TrudomixApi::class.java)
            val payload = byteArrayOf(1, 2, 3, 4)
            fun photo() = MultipartBody.Part.createFormData("photo", "photo.jpg", payload.toRequestBody("image/jpeg".toMediaType()))
            server.enqueue(MockResponse().setBody("""{"id":1,"photo_url":"/uploads/user.jpg"}"""))
            assertEquals("/uploads/user.jpg", api.uploadUserPhoto(photo()).body()?.photo_url)
            server.enqueue(MockResponse().setBody("""{"id":2,"photo_url":"/uploads/resume.jpg"}"""))
            assertEquals("/uploads/resume.jpg", api.uploadResumePhoto(photo()).body()?.photo_url)
            for (path in listOf("/api/users/me/photo", "/api/resumes/me/photo")) {
                val request = server.takeRequest(2, TimeUnit.SECONDS)!!
                assertEquals("POST", request.method)
                assertEquals(path, request.path)
                assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data; boundary="))
                val body = request.body.readUtf8()
                assertTrue(body.contains("name=\"photo\"; filename=\"photo.jpg\""))
                assertTrue(body.contains("Content-Type: image/jpeg"))
            }
        } finally { server.shutdown() }
    }
}
