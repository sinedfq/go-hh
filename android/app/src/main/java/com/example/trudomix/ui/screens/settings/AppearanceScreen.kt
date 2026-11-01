package com.example.trudomix.ui.screens.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.local.*
import com.example.trudomix.ui.screens.profile.ProfileViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable fun AppearanceScreen(viewModel:ProfileViewModel,onBack:()->Unit){
    val mode by viewModel.themeModeFlow.collectAsStateWithLifecycle()
    val searchMotion by VisualPerformance.searchMotion.collectAsStateWithLifecycle()
    val context=LocalContext.current
    SettingsDetailScreen("Оформление",onBack){
        ThemePicker(mode,viewModel::setThemeMode)
        AccentPalette()
        AppIconPicker()
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Column(Modifier.weight(1f)){Text("Движение точек в поиске",style=MaterialTheme.typography.titleMedium);Text("По умолчанию выключено для экономии ресурсов. На слабых устройствах и в энергосбережении фон остаётся статичным.",style=MaterialTheme.typography.bodySmall)}
            Switch(searchMotion,{VisualPerformance.setSearchMotion(context,it)})
        }
        LoadingStylePicker()
    }
}
