package com.example.trudomix.ui.screens.settings

import com.example.trudomix.ui.components.AppScreenHeader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.local.AppThemeMode
import com.example.trudomix.ui.screens.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val themeMode by viewModel.themeModeFlow.collectAsState()

    Scaffold(
        topBar = {
            AppScreenHeader(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ServerConnectionButton()
            AppIconPicker()
            NotificationSettings()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Тема оформления", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = themeMode == AppThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                            label = { Text("Системная") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChip(
                            selected = themeMode == AppThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                            label = { Text("Светлая") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterChip(
                            selected = themeMode == AppThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                            label = { Text("Тёмная") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Системные логи", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNavigateToLogs,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Просмотр системного лога")
                    }
                }
            }
        }
    }
}
