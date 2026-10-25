package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.local.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconPicker() {
    val context = LocalContext.current
    val appearance by AppIcons.appearance.collectAsState()
    val selected = appearance.icon
    val effective = com.example.trudomix.ui.components.selectedBrand()
    var open by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            com.example.trudomix.ui.components.AppIconImage(effective.resource, null, Modifier.size(52.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Иконка приложения", style = MaterialTheme.typography.titleMedium)
                Text(effective.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { open = true }) { Text("Выбрать") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Фон по теме приложения");Text("Нейтральные фоны следуют теме. Цветные сохраняются.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(appearance.followTheme,{AppIcons.followTheme(context,it)})}
        message?.let { Text(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall) }
    }
    if (open) ModalBottomSheet(onDismissRequest = { open = false }) {
        Text("Выберите иконку", Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.titleLarge)
        LazyVerticalGrid(columns = GridCells.Adaptive(96.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AppIcons.choices, key = { it.number }) { icon ->
                OutlinedCard(onClick = {
                    runCatching { AppIcons.select(context, icon) }.onSuccess {
                        open = false; message = "Иконка обновлена. Лаунчеру может понадобиться несколько секунд."
                    }.onFailure { message = "Не удалось изменить иконку. Повторите попытку."; open = false }
                }, colors = CardDefaults.outlinedCardColors(containerColor = if (selected == icon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        com.example.trudomix.ui.components.AppIconImage(icon.resource, icon.title, Modifier.size(64.dp))
                        Text(icon.title, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}
