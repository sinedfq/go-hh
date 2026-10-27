package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trudomix.ui.components.AppScreenHeader
import com.example.trudomix.ui.screens.auth.PhoneOtpViewModel
import com.example.trudomix.ui.screens.auth.PhoneOtpForm
import com.example.trudomix.data.api.*
import com.example.trudomix.data.models.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsDetailScreen(title:String,onBack:()->Unit,content:@Composable ColumnScope.()->Unit){
    Scaffold(topBar={AppScreenHeader(title={Text(title)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}})}){padding->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
    }
}

@Composable fun PhoneAccountScreen(onBack:()->Unit){
    val otp:PhoneOtpViewModel=androidx.lifecycle.viewmodel.compose.viewModel()
    val state by otp.uiState.collectAsState()
    var account by remember{mutableStateOf<AccountPhone?>(null)}
    var loading by remember{mutableStateOf(true)}
    var saving by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var saved by remember{mutableStateOf(false)}
    var refresh by remember{mutableIntStateOf(0)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(refresh){loading=true;error=null;try{account=ApiClient.api.accountPhone().phoneOtpBody()}catch(e:CancellationException){throw e}catch(e:Exception){error=phoneOtpError(e)}finally{loading=false}}
    SettingsDetailScreen("Номер телефона",onBack){
        if(loading)Text("Загружаем номер…")
        account?.let{current->
            Text(if(current.linked)"Привязан номер ${current.phone}"else "Номер пока не привязан",style=MaterialTheme.typography.titleMedium)
            Text("Подтверди номер и сохрани его. Почта и пароль продолжат работать.",style=MaterialTheme.typography.bodyMedium)
            if(saved)Text("Номер успешно привязан",color=MaterialTheme.colorScheme.primary)
            PhoneOtpForm(otp,enabled=!saving)
            Button(enabled=state.verified&&!saving,onClick={scope.launch{
                saving=true;error=null;saved=false
                try{account=ApiClient.api.linkPhone(PhoneLoginRequest(state.phone,state.receipt,otp.deviceKey())).phoneOtpBody();saved=true;otp.editPhone("+7")}
                catch(e:CancellationException){throw e}catch(e:Exception){error=phoneOtpError(e)}finally{saving=false}
            }},modifier=Modifier.fillMaxWidth()){Text(if(saving)"Сохраняем…"else "Привязать номер")}
        }
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        if(!loading&&account==null)OutlinedButton(onClick={refresh++}){Text("Повторить")}
    }
}
