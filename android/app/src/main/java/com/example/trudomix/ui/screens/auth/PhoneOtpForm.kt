package com.example.trudomix.ui.screens.auth

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.trudomix.push.OtpNotifications
import com.example.trudomix.ui.loading.AppLoader

@Composable fun PhoneOtpForm(viewModel:PhoneOtpViewModel,enabled:Boolean=true){
    val state by viewModel.uiState.collectAsState()
    val context=LocalContext.current
    var permissionError by remember{mutableStateOf<String?>(null)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted){permissionError=null;viewModel.requestCode()}else permissionError="Разрешите уведомления Android, чтобы получить код"}
    fun request(){
        permissionError=null
        if(state.config?.test_mode==true&&!OtpNotifications.allowed(context)){
            if(Build.VERSION.SDK_INT>=33&&androidx.core.content.ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else permissionError="В Android отключены уведомления с кодами. Откройте настройки уведомлений"
        }else viewModel.requestCode()
    }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        OutlinedTextField(state.phone,viewModel::editPhone,label={Text("Номер телефона")},placeholder={Text("+7 999 123-45-67")},singleLine=true,enabled=enabled&&!state.busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth())
        if(state.config?.test_mode==true)Text(if(state.config?.delivery=="firebase")"Код придёт с сервера через Firebase в уведомлении Trudomix. Это тестовый способ: SMS не отправляется и владение номером не проверяется."else "Временно код приходит в уведомлении Trudomix. SMS не отправляется; владение номером у оператора не проверяется.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(state.configuring)Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){AppLoader(Modifier.size(20.dp));Text("Подключаем доставку кода…",style=MaterialTheme.typography.bodySmall)}
        else if(state.config?.enabled!=true)TextButton(onClick=viewModel::loadConfig){Text("Повторить подключение")}
        else if(!state.verified){
            OutlinedButton(onClick={request()},enabled=enabled&&!state.busy&&state.retryIn==0&&state.phone.length>=8,modifier=Modifier.fillMaxWidth()){
                Text(if(state.retryIn>0)"Повторить через ${state.retryIn} с"else if(state.challenge.isNotBlank())"Запросить новый код"else "Получить код")
            }
            if(state.challenge.isNotBlank()){
                OutlinedTextField(state.code,viewModel::editCode,label={Text(if(state.config?.test_mode==true)"Код из уведомления"else "Код из SMS")},singleLine=true,enabled=enabled&&!state.busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth())
                Button(onClick=viewModel::verify,enabled=enabled&&!state.busy&&state.code.length==6,modifier=Modifier.fillMaxWidth()){Text("Подтвердить код")}
                if(state.config?.test_mode==true&&state.config?.delivery!="firebase")TextButton(onClick=viewModel::showAgain,enabled=enabled&&!state.busy){Text("Показать уведомление")}
            }
        }
        state.note?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=if(state.verified)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}
        (permissionError?:state.error)?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
        if(permissionError!=null||(state.config?.test_mode==true&&state.challenge.isNotBlank()))TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}){Text("Уведомления Android")}
        if(state.busy)AppLoader(Modifier.size(24.dp))
    }
}
