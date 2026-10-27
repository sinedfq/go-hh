package com.example.trudomix.ui.screens.auth

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
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state) {
        if (state is AuthUiState.Success) { viewModel.resetState(); onLoginSuccess() }
    }
    if(phoneMode)PhoneLoginContent(viewModel,phone,{phoneMode=false;viewModel.resetState()},onNavigateToRegister)
    else LoginContent(state, viewModel::login, onNavigateToRegister, viewModel::resetState,onPhoneLogin={phoneMode=true;viewModel.resetState()})
}

@Composable
fun LoginContent(state: AuthUiState, onLogin: (String, String) -> Unit, onRegister: () -> Unit, onServerChanged: () -> Unit = {},onPhoneLogin:(()->Unit)?=null) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val appearance by animateFloatAsState(if (appeared) 1f else 0f, tween(600, easing = FastOutSlowInEasing), label = "login-entry")
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp && LocalDensity.current.fontScale < 1.4f
        val compact = maxHeight < 620.dp || maxWidth < 360.dp
        AuthBackdrop(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 48.dp else 20.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            val entrance = Modifier.graphicsLayer { alpha = appearance; translationY = (1f - appearance) * 32.dp.toPx() }
            if (wide) Row(entrance.widthIn(max = 1040.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                Column(Modifier.weight(1f)) { LoginHero(false) }
                LoginForm(state, onLogin, onRegister, onServerChanged, Modifier.weight(1f),onPhoneLogin)
            } else Column(entrance.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 32.dp)) {
                LoginHero(compact)
                LoginForm(state, onLogin, onRegister, onServerChanged,onPhoneLogin=onPhoneLogin)
                Text("Вакансии · Отклики · Общение", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
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
private fun LoginHero(compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        com.example.trudomix.ui.components.SelectedBrandImage( "Trudomix", Modifier.size(if (compact) 64.dp else 88.dp))
        Text("Твоя следующая работа", style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("Находи возможности. Знакомься с командами.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun LoginForm(state: AuthUiState, onLogin: (String, String) -> Unit, onRegister: () -> Unit, onServerChanged: () -> Unit, modifier: Modifier = Modifier,onPhoneLogin:(()->Unit)?=null) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val busy = state is AuthUiState.Loading
    val colors = MaterialTheme.colorScheme
    fun submit() { if (!busy) { focus.clearFocus(); onLogin(email.trim(), password) } }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = colors.surface, contentColor = colors.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant), shadowElevation = 4.dp) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("С возвращением", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Войди в свой аккаунт", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
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
            TextButton(onClick = onRegister, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Создать аккаунт", textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            if(onPhoneLogin!=null)TextButton(onClick=onPhoneLogin,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Войти по телефону")}
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ServerConnectionButton(onServerChanged) }
        }
    }
}
