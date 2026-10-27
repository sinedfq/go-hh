package com.example.trudomix.ui.loading

import androidx.compose.ui.graphics.luminance
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.trudomix.startup.NetworkIssue
import com.example.trudomix.startup.StartupReadiness

@Composable fun NetworkErrorState(issue:NetworkIssue,busy:Boolean,onRetry:()->Unit,modifier:Modifier=Modifier){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Text(issue.title,style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
        Text(issue.message,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
        Button(onClick=onRetry,enabled=!busy,shape=MaterialTheme.shapes.medium,contentPadding=PaddingValues(16.dp,12.dp)){
            if(busy){AppLoader(Modifier.size(20.dp),color=MaterialTheme.colorScheme.onPrimary);Spacer(Modifier.width(10.dp))}
            Text(if(busy)"Подключение…"else "Повторить подключение")
        }
    }
}

@Composable fun SplashConnectionState(issue:NetworkIssue,busy:Boolean){
    val context=LocalContext.current
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp,Alignment.CenterVertically),horizontalAlignment=Alignment.CenterHorizontally){
        SlotLogo(MaterialTheme.colorScheme.background.luminance()<.5f,remember{mutableFloatStateOf(SplashTiming.duration)},Modifier.size(156.dp,132.6.dp))
        Text("ТРУДОМИКС",style=MaterialTheme.typography.titleLarge,fontSize=22.sp,letterSpacing=2.5.sp,fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        NetworkErrorState(issue,busy,StartupReadiness::retry,Modifier.widthIn(max=420.dp))
        val intent=remember{Intent(Settings.ACTION_WIRELESS_SETTINGS)}
        if(intent.resolveActivity(context.packageManager)!=null)TextButton(onClick={context.startActivity(intent)}){Text("Настройки сети")}
        if(com.example.trudomix.BuildConfig.DEBUG)com.example.trudomix.ui.screens.settings.ServerConnectionButton{StartupReadiness.retry()}
    }
}

@Composable fun ConnectionBanner(){
    val issue by StartupReadiness.connection.collectAsStateWithLifecycle()
    val busy by StartupReadiness.busy.collectAsStateWithLifecycle()
    issue?.let{Surface(color=MaterialTheme.colorScheme.surface,tonalElevation=4.dp){Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(it.title,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall);TextButton(onClick=StartupReadiness::retry,enabled=!busy){if(busy){AppLoader(Modifier.size(20.dp));Spacer(Modifier.width(8.dp))};Text(if(busy)"Подключение…"else "Повторить")}}}}
}
