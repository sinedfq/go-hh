package com.example.trudomix.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.DeviceTokenRequest
import com.example.trudomix.data.repository.bodyOrThrow
import com.example.trudomix.data.repository.checkSuccess
import com.example.trudomix.push.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationSettings() {
    val context = LocalContext.current
    val registration by DeviceRegistration.status.collectAsState()
    var allowed by remember { mutableStateOf(AppNotifications.allowed(context)) }
    var server by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var serverReady by remember { mutableStateOf(false) }
    var testingPush by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = AppNotifications.allowed(context); DeviceRegistration.sync(context); refresh++
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = AppNotifications.allowed(context); refresh++ }
    LaunchedEffect(registration, refresh) {
        server = null
        serverReady = false
        if (registration == RegistrationStatus.REGISTERED) {
            server = try {
                val result = withTimeout(12000) {
                    ApiClient.api.devicePushStatus(DeviceTokenRequest(FirebaseMessaging.getInstance().token.await())).bodyOrThrow()
                }
                serverReady = result.server_enabled && result.registered
                when {
                    !result.server_enabled -> "На сервере ещё не включена отправка уведомлений"
                    !result.registered -> "Устройство не зарегистрировано. Нажми «Повторить подключение»"
                    else -> "Устройство подключено к уведомлениям"
                }
            } catch (e: TimeoutCancellationException) { "Сервер не ответил на проверку уведомлений" }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { "Сервер не подтвердил готовность уведомлений" }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.NotificationsNone, null, tint = MaterialTheme.colorScheme.primary)
                Text("Уведомления", style = MaterialTheme.typography.titleMedium)
            }
            Text(if (allowed) "Уведомления разрешены на устройстве" else "Разреши уведомления, чтобы видеть новые сообщения", style = MaterialTheme.typography.bodyMedium)
            Text(server ?: when (registration) {
                RegistrationStatus.UNCONFIGURED -> "Сервис уведомлений ещё не подключён в этой сборке. После настройки потребуется обновить приложение."
                RegistrationStatus.SIGNED_OUT -> "Войди в аккаунт для получения уведомлений"
                RegistrationStatus.REGISTERING -> "Подключаем уведомления…"
                RegistrationStatus.REGISTERED -> "Проверяем готовность сервера…"
                RegistrationStatus.FAILED -> "Не удалось подключить уведомления. Проверь соединение и повтори попытку."
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text(if (allowed) "Настройки уведомлений" else "Разрешить уведомления") }
                if (allowed) TextButton(onClick = {
                    AppNotifications.show(context, "Trudomix", "Тестовое уведомление: отображение на устройстве работает", emptyMap(), "notification-test")
                    feedback = "Это локальная проверка отображения. Доставка сообщений с сервера проверяется отдельно."
                }) { Text("Тест уведомления") }
                if (registration != RegistrationStatus.UNCONFIGURED) TextButton(onClick = { DeviceRegistration.sync(context); refresh++ }) { Text("Повторить подключение") }
                if (serverReady && allowed) OutlinedButton(enabled = !testingPush, onClick = {
                    testingPush = true
                    scope.launch {
                        try {
                            withTimeout(20000) {
                                ApiClient.api.testDevicePush(DeviceTokenRequest(FirebaseMessaging.getInstance().token.await())).checkSuccess()
                            }
                            feedback = "Firebase принял сообщение с сервера. Дождись уведомления «Уведомления Trudomix подключены»."
                        } catch (_: TimeoutCancellationException) { feedback = "Не удалось дождаться ответа сервера. Повтори проверку." }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { feedback = com.example.trudomix.data.api.userErrorMessage(e) }
                        finally { testingPush = false }
                    }
                }) { Text(if (testingPush) "Отправляем…" else "Проверить доставку с сервера") }
            }
            feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Настройки по событиям", style = MaterialTheme.typography.labelLarge)
            Text("Сообщения могут появляться поверх экрана, просмотры резюме приходят без звука. Звук и показ каждого типа можно изменить отдельно.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Сообщения" to AppNotifications.MESSAGES_CHANNEL, "Просмотры резюме" to AppNotifications.RESUMES_CHANNEL, "Отклики" to AppNotifications.APPLICATIONS_CHANNEL).forEach { (label, channel) ->
                    TextButton(onClick = {
                        AppNotifications.createChannel(context)
                        context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, channel))
                    }) { Text(label) }
                }
            }
        }
    }
}
