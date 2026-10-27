package com.example.trudomix.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import com.example.trudomix.data.api.ApiClient

class ProfileAccess(val open:()->Unit,val photo:String,val headerVisible:(Any,Boolean)->Unit)
val LocalProfileAccess=staticCompositionLocalOf<ProfileAccess?>{null}

@Composable
fun ProfileAction(modifier:Modifier=Modifier,trackHeader:Boolean=true,toolbarStyle:Boolean=false){
    val access=LocalProfileAccess.current?:return
    var photoFailed by remember(access.photo){mutableStateOf(false)}
    val top=with(LocalDensity.current){36.dp.toPx()}
    val identity=remember{Any()}
    if(trackHeader)DisposableEffect(access,identity){onDispose{access.headerVisible(identity,false)}}
    val tracked=modifier.then(if(trackHeader)Modifier.onGloballyPositioned{access.headerVisible(identity,it.boundsInRoot().bottom>top)}else Modifier)
    val icon: @Composable () -> Unit = {
        if(access.photo.isNotBlank()&&!photoFailed)AsyncImage(ApiClient.mediaUrl(access.photo),"Профиль",Modifier.size(if(toolbarStyle)28.dp else 36.dp).clip(CircleShape),contentScale=androidx.compose.ui.layout.ContentScale.Crop,onError={photoFailed=true})
        else Icon(Icons.Default.PersonOutline,"Профиль",tint=if(toolbarStyle)LocalContentColor.current else MaterialTheme.colorScheme.primary)
    }
    if(toolbarStyle)FilledTonalIconButton(onClick=access.open,shape=RoundedCornerShape(18.dp),modifier=tracked.size(56.dp),content=icon)
    else IconButton(onClick=access.open,modifier=tracked,content=icon)
}

@Composable
fun ProfileAccessHost(key:Any?,enabled:Boolean,onProfile:()->Unit,photo:String="",content: @Composable () -> Unit){
    var up by remember(key){mutableStateOf(false)}
    val headers=remember(key){mutableStateMapOf<Any,Boolean>()}
    val header=headers.values.any{it}
    val latest=rememberUpdatedState(onProfile)
    val access=remember(photo,key){ProfileAccess({latest.value()},photo,{id,visible->if(visible)headers[id]=true else headers.remove(id)})}
    val connection=remember(key){object:NestedScrollConnection{
        override fun onPreScroll(available:Offset,source:NestedScrollSource):Offset{if(source==NestedScrollSource.UserInput&&kotlin.math.abs(available.y)>2)up=available.y>0;return Offset.Zero}
    }}
    CompositionLocalProvider(LocalProfileAccess provides if(enabled)access else null){
        Box(Modifier.fillMaxSize().nestedScroll(connection)) {
            content()
            AnimatedVisibility(enabled&&up&&!header,enter=fadeIn()+slideInVertically{-it},exit=fadeOut(),modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top=8.dp,end=16.dp).testTag("floating-profile-action")) {
                Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,shadowElevation=8.dp){ProfileAction(trackHeader=false)}
            }
        }
    }
}
