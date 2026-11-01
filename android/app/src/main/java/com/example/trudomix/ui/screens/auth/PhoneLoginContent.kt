package com.example.trudomix.ui.screens.auth

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun PhoneLoginContent(auth:AuthViewModel,phone:PhoneOtpViewModel,onEmail:()->Unit,onRegister:()->Unit){
    val state by auth.uiState.collectAsStateWithLifecycle()
    val proof by phone.uiState.collectAsStateWithLifecycle()
    var submitted by remember{mutableStateOf("")}
    fun login(){if(proof.verified){submitted=proof.receipt;auth.loginPhone(proof.phone,proof.receipt,phone.deviceKey())}}
    LaunchedEffect(proof.receipt){if(proof.verified&&submitted!=proof.receipt)login()}
    AuthScaffold(true,{if(!it)onEmail()},state !is AuthUiState.Loading,onRegister){
        PhoneOtpForm(phone,enabled=state !is AuthUiState.Loading,compact=true)
        if(state is AuthUiState.Error)Text((state as AuthUiState.Error).message,color=MaterialTheme.colorScheme.error)
        if(proof.verified)Button(onClick={login()},enabled=state !is AuthUiState.Loading,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(if(state is AuthUiState.Loading)"Входим…"else "Войти")}
        Text("На новом устройстве используй вход по почте.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
