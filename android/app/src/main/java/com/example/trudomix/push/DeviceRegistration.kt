package com.example.trudomix.push

import android.content.Context
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.DeviceTokenRequest
import com.example.trudomix.data.repository.checkSuccess
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

enum class RegistrationStatus { UNCONFIGURED, SIGNED_OUT, DISABLED, REGISTERING, REGISTERED, FAILED }

object DeviceRegistration {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val started = AtomicBoolean(false)
    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val mutableStatus = MutableStateFlow(RegistrationStatus.UNCONFIGURED)
    val status = mutableStatus.asStateFlow()

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        if (FirebaseApp.getApps(context).isEmpty()) return
        scope.launch {
            combine(ApiClient.getTokenManager().tokenFlow, NotificationPreferences.enabled, refresh.onStart { emit(Unit) }) { token, enabled, _ -> token to enabled }
                .collectLatest { (session, enabled) ->
                    if (session.isNullOrBlank()) { mutableStatus.value = RegistrationStatus.SIGNED_OUT; return@collectLatest }
                    // Keep the signed-in installation reachable for explicitly requested login codes.
                    // Business events are suppressed locally by NotificationPreferences.
                    mutableStatus.value = RegistrationStatus.REGISTERING
                    repeat(4) { attempt ->
                        try {
                            val registered = mutex.withLock {
                                withTimeout(20000) {
                                    val token = FirebaseMessaging.getInstance().token.await()
                                    if (ApiClient.getTokenManager().getToken() != session) false
                                    else {
                                        context.getSharedPreferences("push_registration",Context.MODE_PRIVATE).edit().putString("token",token).apply()
                                        ApiClient.api.registerDevice(DeviceTokenRequest(token)).checkSuccess(); true
                                    }
                                }
                            }
                            if (!registered) return@collectLatest
                            mutableStatus.value = if(enabled)RegistrationStatus.REGISTERED else RegistrationStatus.DISABLED
                            return@collectLatest
                        } catch (e: TimeoutCancellationException) {
                            // Retry a bounded registration attempt after a temporary network timeout.
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) { }
                        if (attempt < 3) delay(1500L shl attempt)
                    }
                    mutableStatus.value = RegistrationStatus.FAILED
                    android.util.Log.w("Push", "Device registration failed; retry is available in settings")
                }
        }
    }
    fun sync(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) { mutableStatus.value = RegistrationStatus.UNCONFIGURED; return }
        if (!started.get()) start(context.applicationContext)
        refresh.tryEmit(Unit)
    }
    suspend fun unregister(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        mutex.withLock {
            try {
                withTimeout(10000) {
                    val token = FirebaseMessaging.getInstance().token.await()
                    ApiClient.api.unregisterDevice(DeviceTokenRequest(token)).checkSuccess()
                }
            } catch (e: TimeoutCancellationException) { }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }
            try { withTimeout(10000) { FirebaseMessaging.getInstance().deleteToken().await() } }
            catch (e: TimeoutCancellationException) { }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }
            mutableStatus.value = RegistrationStatus.SIGNED_OUT
        }
    }
}
