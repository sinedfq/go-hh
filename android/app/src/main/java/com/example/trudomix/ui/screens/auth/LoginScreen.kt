package com.example.trudomix.ui.screens.auth

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.trudomix.ui.loading.AppLoader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.trudomix.R
import com.example.trudomix.ui.screens.settings.ServerConnectionButton

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var phoneMode by rememberSaveable{mutableStateOf(false)}
    val phone=androidx.lifecycle.viewmodel.compose.viewModel<PhoneOtpViewModel>(key="login-phone")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state is AuthUiState.Success) { viewModel.resetState(); onLoginSuccess() }
    }
    AnimatedContent(targetState=phoneMode,modifier=Modifier.fillMaxSize(),transitionSpec={(fadeIn(tween(220))+slideInHorizontally{if(targetState)it/8 else -it/8}) togetherWith (fadeOut(tween(150))+slideOutHorizontally{if(targetState)-it/8 else it/8})},label="login-method"){byPhone->
        if(byPhone)PhoneLoginContent(viewModel,phone,{phoneMode=false;viewModel.resetState()},onNavigateToRegister)
        else LoginContent(state, viewModel::login, onNavigateToRegister, viewModel::resetState,onPhoneLogin={phoneMode=true;viewModel.resetState()})
    }
}

@Composable
fun LoginContent(state: AuthUiState, onLogin: (String, String) -> Unit, onRegister: () -> Unit, onServerChanged: () -> Unit = {},onPhoneLogin:(()->Unit)?=null) {
    AuthScaffold(false,onPhoneLogin?.let{callback->{byPhone->if(byPhone)callback()}},state !is AuthUiState.Loading,onRegister){
        LoginFields(state,onLogin,onServerChanged)
    }
}

@Composable
internal fun AuthBackdrop(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val animation = rememberInfiniteTransition(label = "ambient")
    val drift by animation.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ambient-drift")
    Canvas(modifier) {
        drawRect(colors.background)
        val first=Offset(size.width*(-.2f+drift*1.15f),size.height*(.05f+drift*.4f))
        val second=Offset(size.width*(1.15f-drift*.9f),size.height*(.95f-drift*.35f))
        drawRect(Brush.linearGradient(listOf(colors.primary.copy(alpha=.04f+drift*.06f),colors.background,colors.primary.copy(alpha=.08f)),start=first,end=second))
        drawCircle(Brush.radialGradient(listOf(colors.primary.copy(alpha=.32f),colors.primary.copy(alpha=0f)),center=first,radius=size.width*1.05f),radius=size.width*1.05f,center=first)
        drawCircle(Brush.radialGradient(listOf(colors.primary.copy(alpha=.22f),colors.primary.copy(alpha=0f)),center=second,radius=size.width*.9f),radius=size.width*.9f,center=second)
    }
}

@Composable
internal fun LoginHero(compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        com.example.trudomix.ui.components.SelectedBrandImage( "Trudomix", Modifier.size(if (compact) 44.dp else 72.dp))
        if(!compact)Text("Твоя следующая работа", style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if(!compact)Text("Находи возможности. Знакомься с командами.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun LoginFields(state:AuthUiState,onLogin:(String,String)->Unit,onServerChanged:()->Unit){
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val busy = state is AuthUiState.Loading
    val colors = MaterialTheme.colorScheme
    fun submit() { if (!busy) { focus.clearFocus(); onLogin(email.trim(), password) } }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedTextField(email, { email = it }, label = { Text("Почта") }, placeholder={Text("you@example.com")}, leadingIcon = { Icon(Icons.Default.Email, null) }, singleLine = true,
                enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Пароль") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, singleLine = true, enabled = !busy,
                trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visible) "Скрыть пароль" else "Показать пароль") } },
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            AnimatedVisibility(state is AuthUiState.Error, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Surface(color = colors.errorContainer, contentColor = colors.onErrorContainer, shape = RoundedCornerShape(12.dp)) {
                    Text((state as? AuthUiState.Error)?.message.orEmpty(), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = { submit() }, enabled = !busy, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                if (busy) AppLoader(Modifier.size(22.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                else { Text("Войти", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ServerConnectionButton(onServerChanged) }
    }
}
