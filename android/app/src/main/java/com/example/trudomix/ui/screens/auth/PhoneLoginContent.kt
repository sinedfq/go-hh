package com.example.trudomix.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun PhoneLoginContent(auth:AuthViewModel,phone:PhoneOtpViewModel,onEmail:()->Unit,onRegister:()->Unit){
    val state by auth.uiState.collectAsState()
    val proof by phone.uiState.collectAsState()
    var submitted by remember{mutableStateOf("")}
    fun login(){if(proof.verified){submitted=proof.receipt;auth.loginPhone(proof.phone,proof.receipt,phone.deviceKey())}}
    LaunchedEffect(proof.receipt){if(proof.verified&&submitted!=proof.receipt)login()}
    Box(Modifier.fillMaxSize()){
        AuthBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp,Alignment.CenterVertically),horizontalAlignment=Alignment.CenterHorizontally){
            Surface(Modifier.widthIn(max=480.dp).fillMaxWidth(),shape=MaterialTheme.shapes.extraLarge){
                Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                    Text("Вход по телефону",style=MaterialTheme.typography.headlineSmall)
                    Text("В тестовом режиме вход доступен на устройстве регистрации. После смены устройства используйте почту и пароль.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    PhoneOtpForm(phone,enabled=state !is AuthUiState.Loading)
                    if(state is AuthUiState.Error)Text((state as AuthUiState.Error).message,color=MaterialTheme.colorScheme.error)
                    if(proof.verified)Button(onClick={login()},enabled=state !is AuthUiState.Loading,modifier=Modifier.fillMaxWidth()){Text(if(state is AuthUiState.Loading)"Входим…"else "Войти")}
                    TextButton(onClick=onEmail,enabled=state !is AuthUiState.Loading){Text("Войти по почте")}
                    TextButton(onClick=onRegister,enabled=state !is AuthUiState.Loading){Text("Создать аккаунт")}
                }
            }
        }
    }
}
