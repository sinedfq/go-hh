package com.example.trudomix.ui.screens.profile

import com.example.trudomix.ui.components.AppScreenHeader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel:ProfileViewModel,onLogout:()->Unit,onNavigateToSettings:()->Unit={},onNavigateToLogs:()->Unit={},onNavigateToResumeEditor:()->Unit={},onEmployer:()->Unit={},onApplications:()->Unit={},onFavorites:()->Unit={},onBack:(()->Unit)?=null) {
    val uiState by viewModel.uiState.collectAsState()
    val overview by viewModel.overview.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    com.example.trudomix.ui.components.RefreshContent(overview.loading,viewModel::loadUserProfile) {
    Box(Modifier.fillMaxSize().statusBarsPadding(),contentAlignment=Alignment.TopCenter) {
        Column(Modifier.widthIn(max=760.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(start=20.dp,end=20.dp,top=16.dp,bottom=148.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        AppScreenHeader(title={Text("Профиль")},subtitle="Твой профиль и карьерная активность",applyInsets=false,contentPadding=PaddingValues(0.dp),navigationIcon={onBack?.let{IconButton(onClick=it){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}}},actions={
        IconButton(onClick=onNavigateToSettings){Icon(Icons.Default.Settings,"Настройки")}
        })
        Spacer(Modifier.height(24.dp))
            when(val state=uiState) {
                ProfileUiState.Loading -> com.example.trudomix.ui.loading.TrudomixLoadingScreen(label="Загружаем профиль")
                is ProfileUiState.Error -> Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(state.message,color=MaterialTheme.colorScheme.error)
                    Button(onClick={viewModel.loadUserProfile()}){Text("Повторить")}
                }
                is ProfileUiState.Success -> {
                    val user=state.user
                    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
                        com.example.trudomix.ui.components.PhotoPicker(user.photo_url,"Фото профиля",viewModel::uploadPhoto)
                        Spacer(Modifier.height(12.dp))
                        Text(overview.resume?.full_name?.takeIf{it.isNotBlank()} ?: user.email,style=MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        if(!overview.resume?.full_name.isNullOrBlank()) Text(user.email,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        AssistChip(onClick={},label={Text(if(user.role=="candidate")"Соискатель" else "Работодатель")})
                        Spacer(Modifier.height(24.dp))
                        ProfileDashboard(overview,user.role=="candidate",onNavigateToResumeEditor,if(user.role=="candidate")onApplications else onEmployer,onFavorites,viewModel::loadOverview)
                        Spacer(Modifier.height(20.dp))
                        if(user.role=="candidate")OutlinedButton(onClick=onNavigateToResumeEditor,modifier=Modifier.fillMaxWidth()){Text("Моё резюме")}
                        if(user.role=="employer"||user.role=="admin")OutlinedButton(onClick=onEmployer,modifier=Modifier.fillMaxWidth()){Text("Моя компания")}
                        OutlinedButton(onClick={showLogoutDialog=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.error)) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp,null);Spacer(Modifier.width(8.dp));Text("Выйти из аккаунта")
                        }
                    }
                }
            }
        }
    }
    }
    if(showLogoutDialog)AlertDialog(onDismissRequest={showLogoutDialog=false},title={Text("Выход")},text={Text("Вы уверены, что хотите выйти из аккаунта?")},confirmButton={TextButton(onClick={showLogoutDialog=false;viewModel.logout(onLogout)}){Text("Выйти",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={showLogoutDialog=false}){Text("Отмена")}})
}
