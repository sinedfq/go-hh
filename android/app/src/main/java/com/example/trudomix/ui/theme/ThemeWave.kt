package com.example.trudomix.ui.theme

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import com.example.trudomix.ui.loading.durationScale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import kotlin.math.*

typealias ThemeWaveAction=(Offset,()->Unit)->Unit
val LocalThemeWave=staticCompositionLocalOf<ThemeWaveAction>{{_,apply->apply()}}

/** One temporary frame of the old palette, revealed through a circular opening. */
@Composable fun ThemeWaveHost(window:Window,dark:Boolean,content:@Composable ()->Unit){
    val scope=rememberCoroutineScope()
    val palette by rememberUpdatedState(dark)
    val motion by rememberUpdatedState(durationScale())
    val pending=remember{arrayOfNulls<Job>(1)}
    var frame by remember{mutableStateOf<Bitmap?>(null)}
    var center by remember{mutableStateOf(Offset.Zero)}
    var rootOrigin by remember{mutableStateOf(Offset.Zero)}
    var rootSize by remember{mutableStateOf(IntSize.Zero)}
    val radius=remember{Animatable(0f)}
    val reveal:ThemeWaveAction={origin,apply->
        pending[0]?.cancel();frame=null
        if(motion==0f)apply()
        else pending[0]=scope.launch{
            var applied=false
            fun commit(){if(!applied){applied=true;apply()}}
            try{
                val old=withTimeoutOrNull(350){captureThemeFrame(window)}
                if(old==null){commit();return@launch}
                val before=palette
                radius.snapTo(0f);center=origin-rootOrigin;frame=old
                commit()
                if(withTimeoutOrNull(1200){snapshotFlow{palette}.first{it!=before}}==null)return@launch
                withFrameNanos{};withFrameNanos{}
                val width=rootSize.width.toFloat();val height=rootSize.height.toFloat()
                val end=hypot(max(center.x,width-center.x),max(center.y,height-center.y))
                radius.animateTo(end,tween(620,easing=FastOutSlowInEasing))
            }catch(e:CancellationException){throw e}
            catch(_:Exception){commit()}
            finally{if(pending[0]===coroutineContext[Job]){frame=null;pending[0]=null}}
        }
    }
    DisposableEffect(Unit){onDispose{pending[0]?.cancel()}}
    CompositionLocalProvider(LocalThemeWave provides reveal){
        Box(Modifier.fillMaxSize().onGloballyPositioned{rootOrigin=it.positionInWindow();rootSize=it.size}){
            content()
            frame?.let{bitmap->
                val image=remember(bitmap){bitmap.asImageBitmap()}
                Canvas(Modifier.matchParentSize().testTag("theme-wave-overlay")){
                    val opening=Path().apply{addOval(Rect(center,radius.value))}
                    val ratio=image.width.toFloat()/window.decorView.width.coerceAtLeast(1)
                    val x=(rootOrigin.x*ratio).roundToInt().coerceIn(0,image.width-1)
                    val y=(rootOrigin.y*ratio).roundToInt().coerceIn(0,image.height-1)
                    val source=IntSize((size.width*ratio).roundToInt().coerceIn(1,image.width-x),(size.height*ratio).roundToInt().coerceIn(1,image.height-y))
                    clipPath(opening,ClipOp.Difference){drawImage(image,srcOffset=IntOffset(x,y),srcSize=source,dstSize=IntSize(size.width.roundToInt(),size.height.roundToInt()))}
                }
            }
        }
    }
}

private suspend fun captureThemeFrame(window:Window):Bitmap?=suspendCancellableCoroutine{continuation->
    val view=window.decorView
    if(view.width<=0||view.height<=0){continuation.resume(null);return@suspendCancellableCoroutine}
    val factor=min(1.0,sqrt(4_000_000.0/(view.width.toDouble()*view.height)))
    val bitmap=Bitmap.createBitmap((view.width*factor).roundToInt(),(view.height*factor).roundToInt(),Bitmap.Config.ARGB_8888)
    try{
        PixelCopy.request(window,bitmap,{result->
            if(continuation.isActive&&result==PixelCopy.SUCCESS)continuation.resume(bitmap)
            else{bitmap.recycle();if(continuation.isActive)continuation.resume(null)}
        },Handler(Looper.getMainLooper()))
    }catch(_:Exception){bitmap.recycle();if(continuation.isActive)continuation.resume(null)}
}
