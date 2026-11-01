package com.example.trudomix.ui.loading

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.trudomix.startup.StartupReadiness

/** Only composed images on the initial screen participate; offscreen pages stay lazy. */
@Composable fun StartupAsyncImage(model:Any?,contentDescription:String?,modifier:Modifier=Modifier,contentScale:ContentScale=ContentScale.Fit){
    val ticket=remember(model){Any()}
    DisposableEffect(ticket){if(model!=null)StartupReadiness.begin(ticket);onDispose{StartupReadiness.finish(ticket)}}
    AsyncImage(model,contentDescription,modifier,contentScale=contentScale,
        onSuccess={StartupReadiness.finish(ticket)},onError={StartupReadiness.finish(ticket)})
}
