package com.example.trudomix.ui.loading

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.trudomix.startup.StartupReadiness
import com.example.trudomix.startup.StartupAssetTicket

/** Only composed images on the initial screen participate; offscreen pages stay lazy. */
@Composable fun StartupAsyncImage(model:Any?,contentDescription:String?,modifier:Modifier=Modifier,contentScale:ContentScale=ContentScale.Fit){
    val ticket=rememberStartupImageTicket(model)
    AsyncImage(model,contentDescription,modifier,contentScale=contentScale,
        onSuccess={ticket.complete()},onError={ticket.complete()})
}

@Composable internal fun rememberStartupImageTicket(model:Any?):StartupAssetTicket{
    val ticket=remember(model){StartupAssetTicket(StartupReadiness::begin,StartupReadiness::finish)}
    DisposableEffect(ticket){if(model!=null&&model!="")ticket.start();onDispose{ticket.complete()}}
    return ticket
}
