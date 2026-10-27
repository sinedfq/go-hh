package com.example.trudomix.ui.screens.auth

import com.example.trudomix.ui.loading.AppLoader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.local.AppIcons
import com.example.trudomix.ui.components.AppIconImage
import com.example.trudomix.ui.components.AppScreenHeader

@Composable
fun RegisterScreen(viewModel:AuthViewModel,onRegisterSuccess:()->Unit,onNavigateToLogin:()->Unit) {
    val phone=androidx.lifecycle.viewmodel.compose.viewModel<PhoneOtpViewModel>(key="registration-phone")
    val proof by phone.uiState.collectAsState()
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state) { if(state is AuthUiState.Success){viewModel.resetState();onRegisterSuccess()} }
    RegisterContent(state,{email,password,role->viewModel.registerWithPhone(email,password,role,proof.phone,proof.receipt,phone.deviceKey())},onNavigateToLogin,phone)
}

@Composable
fun RegisterContent(state:AuthUiState,onRegister:(String,String,String)->Unit,onLogin:()->Unit,phoneViewModel:PhoneOtpViewModel?=null) {
    var appeared by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){appeared=true}
    val entrance by animateFloatAsState(if(appeared)1f else 0f,tween(450),label="registration-entry")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide=maxWidth>=840.dp&&LocalDensity.current.fontScale<1.4f
        AuthBackdrop(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=if(wide)48.dp else 20.dp,vertical=24.dp),contentAlignment=Alignment.Center) {
            Column(Modifier.widthIn(max=if(wide)1040.dp else 520.dp).fillMaxWidth().graphicsLayer{alpha=entrance;translationY=(1-entrance)*24.dp.toPx()},verticalArrangement=Arrangement.spacedBy(24.dp)) {
                AppScreenHeader(title={Text("Регистрация")},subtitle="Создай профиль для новых возможностей",applyInsets=false,contentPadding=PaddingValues(0.dp),containerColor=Color.Transparent,
                    navigationIcon={IconButton(onClick=onLogin,enabled=state !is AuthUiState.Loading){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Ко входу")}})
                if(wide)Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(48.dp)) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                        com.example.trudomix.ui.components.SelectedBrandImage("Trudomix",Modifier.size(88.dp))
                        Text("Знакомься с будущей командой",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                        Text("Ищи работу или находи людей, с которыми хочется работать.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RegistrationForm(state,onRegister,onLogin,Modifier.weight(1f),phoneViewModel)
                } else RegistrationForm(state,onRegister,onLogin,phoneViewModel=phoneViewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegistrationForm(state:AuthUiState,onRegister:(String,String,String)->Unit,onLogin:()->Unit,modifier:Modifier=Modifier,phoneViewModel:PhoneOtpViewModel?=null) {
    val phoneState=phoneViewModel?.uiState?.collectAsState()
    val phoneReady=phoneViewModel==null||phoneState?.value?.verified==true
    var email by rememberSaveable{mutableStateOf("")}
    var password by remember{mutableStateOf("")}
    var visible by remember{mutableStateOf(false)}
    var role by rememberSaveable{mutableStateOf("candidate")}
    val busy=state is AuthUiState.Loading
    val colors=MaterialTheme.colorScheme
    val focus=LocalFocusManager.current
    fun submit(){if(!busy&&phoneReady){focus.clearFocus();onRegister(email.trim(),password,role)}}
    Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),color=colors.surface,contentColor=colors.onSurface,border=BorderStroke(1.dp,colors.outlineVariant),shadowElevation=4.dp) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Создать аккаунт",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text("Выбери свою роль и укажи данные для входа",color=colors.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(role=="candidate",{role="candidate"},enabled=!busy,label={Text("Ищу работу",maxLines=1,style=MaterialTheme.typography.labelMedium)})
                FilterChip(role=="employer",{role="employer"},enabled=!busy,label={Text("Ищу сотрудников",maxLines=1,style=MaterialTheme.typography.labelMedium)})
            }
            OutlinedTextField(email,{email=it},label={Text("Почта")},singleLine=true,enabled=!busy,leadingIcon={Icon(Icons.Default.Email,null)},shape=RoundedCornerShape(16.dp),
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,imeAction=ImeAction.Next),modifier=Modifier.fillMaxWidth())
            if(phoneViewModel!=null)PhoneOtpForm(phoneViewModel,enabled=!busy)
            OutlinedTextField(password,{password=it},label={Text("Пароль")},singleLine=true,enabled=!busy,leadingIcon={Icon(Icons.Default.Lock,null)},shape=RoundedCornerShape(16.dp),
                trailingIcon={IconButton(onClick={visible=!visible},enabled=!busy){Icon(if(visible)Icons.Default.VisibilityOff else Icons.Default.Visibility,if(visible)"Скрыть пароль" else "Показать пароль")}},
                visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),
                keyboardActions=KeyboardActions(onDone={submit()}),modifier=Modifier.fillMaxWidth())
            AnimatedVisibility(state is AuthUiState.Error,enter=fadeIn()+expandVertically(),exit=fadeOut()+shrinkVertically()) {
                Surface(color=colors.errorContainer,contentColor=colors.onErrorContainer,shape=RoundedCornerShape(12.dp)) {
                    Text((state as? AuthUiState.Error)?.message.orEmpty(),Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick={submit()},enabled=!busy&&phoneReady,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {
                if(busy)AppLoader(Modifier.size(22.dp),color=colors.onPrimary,strokeWidth=2.dp)else Text("Зарегистрироваться")
            }
            TextButton(onClick=onLogin,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Уже есть аккаунт? Войти")}
        }
    }
}
