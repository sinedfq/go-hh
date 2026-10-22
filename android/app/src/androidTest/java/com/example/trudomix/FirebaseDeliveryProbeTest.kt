package com.example.trudomix

import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.push.AppNotifications
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.DeviceTokenRequest
import com.example.trudomix.data.repository.bodyOrThrow
import com.example.trudomix.data.repository.checkSuccess
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in check against the developer's configured Firebase project, never run in CI. */
class FirebaseDeliveryProbeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun enabled() = assumeTrue(InstrumentationRegistry.getArguments().getString("livePush") == "true")

    @Test fun obtainDeviceToken() {
        enabled()
        assertTrue("Install an APK with google-services.json first", FirebaseApp.getApps(context).isNotEmpty())
        val expectedProject = InstrumentationRegistry.getArguments().getString("firebaseProject")
        if (expectedProject != null) assertEquals(expectedProject, FirebaseApp.getInstance().options.projectId)
        AppNotifications.createChannel(context)
        assertTrue("Allow notification permission first", AppNotifications.allowed(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.notification.extras.getString(Notification.EXTRA_TITLE) == "Проверка Firebase Trudomix" }.forEach { manager.cancel(it.id) }
        val token = runBlocking { withTimeout(60000) { FirebaseMessaging.getInstance().token.await() } }
        assertTrue("Firebase returned an empty token", token.isNotBlank())
        // The token is read locally by the diagnostic sender; do not print it to test logs.
        File(context.getExternalFilesDir(null), "fcm-probe-token.txt").writeText(token)
    }

    @Test fun realMessageAppearsOnDevice() {
        enabled()
        val manager = context.getSystemService(NotificationManager::class.java)
        val deadline = System.currentTimeMillis() + 45000
        fun received() = manager.activeNotifications.firstOrNull { it.notification.extras.getString(Notification.EXTRA_TITLE) == "Проверка Firebase Trudomix" }
        while (received() == null && System.currentTimeMillis() < deadline) Thread.sleep(250)
        val notification = received()
        assertNotNull("A real Firebase data message was not displayed", notification)
        assertEquals(AppNotifications.CHANNEL, notification!!.notification.channelId)
        assertTrue(notification.notification.extras.getString(Notification.EXTRA_TEXT).orEmpty().contains("через Firebase"))
        assertNotNull(notification.notification.contentIntent)
        manager.cancel(notification.id)
        File(context.getExternalFilesDir(null), "fcm-probe-token.txt").delete()
    }

    @Test fun serverDiagnosticReachesTheSignedInDevice() {
        enabled()
        val manager = context.getSystemService(NotificationManager::class.java)
        val title = "Уведомления Trudomix подключены"
        manager.activeNotifications.filter { it.notification.extras.getString(Notification.EXTRA_TITLE) == title }.forEach { manager.cancel(it.id) }
        runBlocking {
            assertFalse("Sign in to Trudomix on the test device first", ApiClient.getTokenManager().getToken().isNullOrBlank())
            withTimeout(45000) {
                val request = DeviceTokenRequest(FirebaseMessaging.getInstance().token.await())
                ApiClient.api.registerDevice(request).checkSuccess()
                val status = ApiClient.api.devicePushStatus(request).bodyOrThrow()
                assertTrue("FCM is disabled on the running API", status.server_enabled)
                assertTrue("Device was not registered with the current user", status.registered)
                ApiClient.api.testDevicePush(request).checkSuccess()
            }
        }
        val deadline = System.currentTimeMillis() + 45000
        fun received() = manager.activeNotifications.firstOrNull { it.notification.extras.getString(Notification.EXTRA_TITLE) == title }
        while (received() == null && System.currentTimeMillis() < deadline) Thread.sleep(250)
        assertNotNull("The server diagnostic did not reach the signed-in device", received())
        received()?.let { manager.cancel(it.id) }
    }
}
