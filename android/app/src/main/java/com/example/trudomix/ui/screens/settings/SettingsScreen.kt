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
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.api.phoneOtpBody
import com.example.trudomix.ui.screens.auth.formattedAccountPhone
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsScreen(viewModel:ProfileViewModel,onNavigateBack:()->Unit,onNotifications:()->Unit={},onDocuments:()->Unit={},onPhone:()->Unit={},onAppearance:()->Unit={}){
    var linkedPhone by remember{mutableStateOf("")}
    LaunchedEffect(Unit){try{linkedPhone=ApiClient.api.accountPhone().phoneOtpBody().phone}catch(e:CancellationException){throw e}catch(_:Exception){}}
    Scaffold(topBar={AppScreenHeader(
        title={Text("Настройки",fontWeight=FontWeight.Bold)},
        navigationIcon={IconButton(onClick=onNavigateBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Назад")}}
    )}){padding->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            ServerConnectionButton()
            SettingsLink("Оформление", "Тема, цвет интерфейса, иконка и анимация загрузки", onAppearance)
            SettingsLink("Телефон", if(linkedPhone.isBlank())"Привязать номер для входа"else formattedAccountPhone(linkedPhone), onPhone,highlight=linkedPhone.isNotBlank())
            SettingsLink("Уведомления", "Firebase, сообщения, отклики и просмотры резюме", onNotifications)
            SettingsLink("Документы", "Политика конфиденциальности и документы сервиса", onDocuments)
            Text("Trudomix ${com.example.trudomix.BuildConfig.VERSION_NAME} · сборка #${com.example.trudomix.BuildConfig.VERSION_CODE}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable private fun SettingsLink(title:String,subtitle:String,onClick:()->Unit,highlight:Boolean=false){
    OutlinedCard(onClick=onClick,modifier=Modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(title,style=MaterialTheme.typography.titleMedium)
                Text(subtitle,style=if(highlight)MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,fontWeight=if(highlight)FontWeight.SemiBold else FontWeight.Normal,color=if(highlight)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›",style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary)
        }
    }
}
