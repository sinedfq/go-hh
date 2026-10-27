package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.trudomix.ui.components.AppScreenHeader
import com.example.trudomix.ui.screens.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsScreen(viewModel:ProfileViewModel,onNavigateBack:()->Unit){
    val themeMode by viewModel.themeModeFlow.collectAsState()
    Scaffold(topBar={AppScreenHeader(
        title={Text("Настройки",fontWeight=FontWeight.Bold)},
        navigationIcon={IconButton(onClick=onNavigateBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Назад")}}
    )}){padding->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            ServerConnectionButton()
            ThemePicker(themeMode,viewModel::setThemeMode)
            AppIconPicker()
            LoadingStylePicker()
            NotificationSettings()
            LegalDocuments()
            Text("Trudomix ${com.example.trudomix.BuildConfig.VERSION_NAME} · сборка #${com.example.trudomix.BuildConfig.VERSION_CODE}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.align(Alignment.CenterHorizontally))
        }
    }
}