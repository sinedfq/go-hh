package com.example.trudomix.ui.loading

import com.example.trudomix.startup.StartupReadiness
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

const val STARTUP_LOADER_PREVIEW_MS = 0L

/** Existing screen contract delegates to the shared, compact brand loader. */
@Composable fun TrudomixLoadingScreen(modifier:Modifier=Modifier,compact:Boolean=false,label:String="Загружаем…"){
    val startupVisible by StartupReadiness.visible.collectAsState()
    if(startupVisible&&!compact){
        val ticket=remember{Any()}
        DisposableEffect(ticket){StartupReadiness.begin(ticket);onDispose{StartupReadiness.finish(ticket)}}
        Box(modifier.fillMaxSize());return
    }
    PageLoadingState(modifier.then(if(compact)Modifier else Modifier.fillMaxSize()),label,compact)
}
