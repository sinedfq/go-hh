package com.example.trudomix.data.api

import android.content.Context
import com.example.trudomix.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.example.trudomix.data.local.TokenManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiClient {
    // Для эмулятора: http://10.0.2.2:8080/
    // Для реального телефона (IP вашего компьютера в Wi-Fi сети): http://192.168.0.4:8080/
    private val BASE_URL = BuildConfig.API_BASE_URL
    private lateinit var preferences: android.content.SharedPreferences
    @Volatile private var serverUrl = BASE_URL.toHttpUrl()
    val baseUrl: String get() = serverUrl.toString()

    private lateinit var tokenManager: TokenManager

    fun init(context: Context) {
        tokenManager = TokenManager(context.applicationContext)
        preferences = context.getSharedPreferences("server_connection", Context.MODE_PRIVATE)
        serverUrl = if (BuildConfig.DEBUG) runCatching {
            normalizeServerUrl(preferences.getString("base_url", BASE_URL) ?: BASE_URL).toHttpUrl()
        }.getOrDefault(BASE_URL.toHttpUrl()) else BASE_URL.toHttpUrl()
    }

    fun getTokenManager(): TokenManager = tokenManager

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .eventListenerFactory{com.example.trudomix.startup.StartupReadiness.listener()}
            .addInterceptor { chain ->
                val destination = serverUrl
                val original = chain.request()
                val url = original.url.newBuilder().scheme(destination.scheme).host(destination.host).port(destination.port).build()
                chain.proceed(original.newBuilder().url(url).build())
            }
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val api: TrudomixApi by lazy {
        retrofit.create(TrudomixApi::class.java)
    }

    fun mediaUrl(path: String): String? = path.takeIf { it.isNotBlank() }?.let { serverUrl.resolve(it)?.toString() }

    suspend fun checkServer(address: String) = withContext(Dispatchers.IO) {
        val url = normalizeServerUrl(address).toHttpUrl().resolve("health")!!
        // No session token is sent when checking a different server.
        OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(false).build().newCall(Request.Builder().url(url).build()).execute().use {
                if(!it.isSuccessful)throw ServerCheckException(it.code)
                val body = it.body?.string().orEmpty()
                check(runCatching { json.parseToJsonElement(body).jsonObject["status"]?.jsonPrimitive?.content == "ok" }.getOrDefault(false)) { "По этому адресу не найден API Trudomix" }
            }
    }

    suspend fun setServer(address: String) {
        check(BuildConfig.DEBUG) { "Адрес release-сервера задаётся при сборке" }
        val url = normalizeServerUrl(address).toHttpUrl()
        if (url != serverUrl) {
            tokenManager.clearSession()
            serverUrl = url
            preferences.edit().putString("base_url", url.toString()).apply()
        }
    }
}

class ServerCheckException(val status:Int):java.io.IOException("Сервер временно недоступен")

fun normalizeServerUrl(address: String): String {
    val url = address.trim().toHttpUrl()
    require(url.encodedPath == "/" && url.query == null && url.fragment == null && url.username.isEmpty() && url.password.isEmpty()) {
        "Укажите только адрес сервера, например http://192.168.0.4:8080/"
    }
    return url.toString()
}
