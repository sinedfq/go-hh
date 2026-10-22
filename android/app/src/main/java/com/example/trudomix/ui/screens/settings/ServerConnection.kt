package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.trudomix.BuildConfig
import com.example.trudomix.data.api.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ServerConnectionButton(onChanged: () -> Unit = {}) {
    if (!BuildConfig.DEBUG) return
    var show by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { show = true }) { Text("Подключение к серверу") }
    if (show) ServerConnectionDialog({ show = false }, { show = false; onChanged() })
}

@Composable
private fun ServerConnectionDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    var address by rememberSaveable { mutableStateOf(ApiClient.baseUrl) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Подключение к серверу") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Телефон и компьютер должны быть в одной Wi-Fi сети. Укажите IP компьютера и порт API. Адрес 10.0.2.2 доступен только эмулятору.")
                OutlinedTextField(address, { address = it; result = null }, label = { Text("Адрес сервера") }, singleLine = true,
                    enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
                Text("Например: http://192.168.0.4:8080/", style = MaterialTheme.typography.bodySmall)
                Text("При смене сервера потребуется войти заново.", style = MaterialTheme.typography.bodySmall)
                result?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true; result = null
                scope.launch {
                    try {
                        ApiClient.checkServer(address)
                        ApiClient.setServer(address)
                        onSaved()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { result = "Нет подключения: ${e.localizedMessage}. Проверьте адрес, запуск API и доступ к порту в Windows Firewall." }
                    finally { busy = false }
                }
            }) { Text("Проверить и сохранить") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Отмена") } }
    )
}
