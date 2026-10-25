package com.example.trudomix.ui.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trudomix.ui.components.AppIconImage
import com.example.trudomix.ui.components.selectedBrand

// Temporary minimum startup duration for reviewing the animation; set to 0 to disable.
const val STARTUP_LOADER_PREVIEW_MS = 2200L

/** Adapted from loader/android; visibility follows the real request state. */
@Composable
fun TrudomixLoadingScreen(modifier:Modifier=Modifier,compact:Boolean=false,label:String="Подбираем возможности для вас"){
    val icon=selectedBrand()
    val colors=MaterialTheme.colorScheme
    val accent=when(icon.color){"blue"->Color(0xFF248BFF);"violet"->Color(0xFF8651F7);"navy"->Color(0xFF566BC7);"mint"->Color(0xFF14B8A6);"coral"->Color(0xFFFF6B69);"cream"->Color(0xFFB58A31);else->Color(0xFF8651F7)}
    val transition=rememberInfiniteTransition(label="loader-logo")
    val pulse by transition.animateFloat(.94f,1f,infiniteRepeatable(tween(1400,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="loader-pulse")
    Column(modifier.then(if(compact)Modifier.fillMaxWidth().heightIn(min=190.dp)else Modifier.fillMaxSize()).background(colors.background).padding(24.dp).semantics{liveRegion=LiveRegionMode.Polite},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        AppIconImage(icon.resource,"Логотип Трудомикс",Modifier.size(if(compact)64.dp else 116.dp).scale(pulse).semantics{stateDescription=icon.title})
        Spacer(Modifier.height(if(compact)12.dp else 18.dp))
        Text("Трудомикс",fontSize=if(compact)20.sp else 26.sp,color=colors.onBackground)
        Spacer(Modifier.height(10.dp))
        Text(label,color=colors.onBackground.copy(alpha=.65f),fontSize=13.sp)
        Spacer(Modifier.height(if(compact)16.dp else 22.dp))
        LinearProgressIndicator(modifier=Modifier.width(if(compact)150.dp else 190.dp).height(3.dp),color=accent,trackColor=colors.onBackground.copy(alpha=.12f))
    }
}
