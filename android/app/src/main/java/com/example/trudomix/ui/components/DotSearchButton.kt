package com.example.trudomix.ui.components

import android.app.ActivityManager
import android.content.*
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.trudomix.data.local.VisualPerformance
import com.example.trudomix.ui.loading.durationScale
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlin.math.*

internal fun searchMotionAllowed(lowRam:Boolean,memoryClass:Int,powerSave:Boolean,scale:Float)=!lowRam&&memoryClass>=192&&!powerSave&&scale>0f

private val SearchBackground=Color(0xFF0B2448)
private val SearchBlue=Color(0xFF70C9FF)
// Only normalized immutable geometry; created once, not on each frame/composition.
private val SearchDots=buildList{
    for(y in -3..3)for(x in -3..3)if(x*x+y*y<=10)add(Offset(x/9f,y/9f))
}.toTypedArray()
private val DotPhases=FloatArray(SearchDots.size){i->SearchDots[i].x*7f+SearchDots[i].y*5f}

/** One draw node. Animation state is read exclusively in the draw phase. No blur or layers. */
@Composable fun DotSearchButton(onClick:()->Unit,modifier:Modifier=Modifier,label:String="Поиск"){
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val preferred by VisualPerformance.searchMotion.collectAsStateWithLifecycle()
    val scale=durationScale()
    val activityManager=remember(context){context.getSystemService(ActivityManager::class.java)}
    val power=remember(context){context.getSystemService(PowerManager::class.java)}
    var powerSave by remember{mutableStateOf(power.isPowerSaveMode)}
    DisposableEffect(context,power){
        val receiver=object:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){powerSave=power.isPowerSaveMode}}
        androidx.core.content.ContextCompat.registerReceiver(context,receiver,IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose{context.unregisterReceiver(receiver)}
    }
    val capable=searchMotionAllowed(activityManager.isLowRamDevice,activityManager.memoryClass,powerSave,scale)
    val interactions=remember{MutableInteractionSource()}
    val pressed by interactions.collectIsPressedAsState()
    val density=LocalDensity.current
    val borderStroke=remember(density){Stroke(with(density){1.dp.toPx()})}
    val lensStroke=remember(density){Stroke(with(density){2.3.dp.toPx()})}
    val phase=remember{mutableFloatStateOf(0f)}
    val pulse=remember{mutableFloatStateOf(0f)}
    LaunchedEffect(lifecycle,preferred,capable){
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
            // The press collector owns its short pulse, and stops with the visible screen.
            launch{
                interactions.interactions.collect{event->
                    if(event is PressInteraction.Press&&capable){
                        val start=withFrameNanos{it}
                        do{val elapsed=(withFrameNanos{it}-start)/1_000_000f;pulse.floatValue=(1-elapsed/180f).coerceIn(0f,1f)}while(pulse.floatValue>0f)
                    }
                }
            }
            try{
                if(preferred&&capable){
                    val start=withFrameNanos{it};var previous=start
                    while(true){
                        val now=withFrameNanos{it}
                        if(now-previous>=33_333_333L){phase.floatValue=((now-start)%6_000_000_000L)/6_000_000_000f*(2*PI).toFloat();previous=now}
                    }
                }else kotlinx.coroutines.awaitCancellation()
            }finally{phase.floatValue=0f;pulse.floatValue=0f}
        }
    }
    Canvas(modifier.size(56.dp).semantics{contentDescription=label}.clickable(interactionSource=interactions,indication=null,role=Role.Button,onClick=onClick)){
        val w=size.minDimension;val center=Offset(size.width/2,size.height/2)
        drawCircle(SearchBackground,w/2,center)
        val wave=phase.floatValue;val flash=pulse.floatValue
        for(index in SearchDots.indices){
            val point=SearchDots[index]
            val amount=if(preferred&&capable)(sin(wave+DotPhases[index])+1f)*.5f else .35f
            val movement=if(preferred&&capable)sin(wave+point.x*6f)*w*.008f else 0f
            drawCircle(SearchBlue.copy(alpha=.09f+amount*.17f),w*(.007f+amount*.009f),Offset(center.x+point.x*w,center.y+point.y*w+movement))
        }
        drawCircle(SearchBlue.copy(alpha=.65f),w/2-.7.dp.toPx(),center,style=borderStroke)
        if(!capable&&pressed)drawCircle(SearchBlue.copy(alpha=.12f),w/2,center)
        if(flash>0f){drawCircle(SearchBlue.copy(alpha=flash*.15f),w/2,center);drawCircle(SearchBlue.copy(alpha=flash*.45f),w*(.28f+(1-flash)*.20f),center,style=borderStroke)}
        val lens=Offset(center.x-w*.045f,center.y-w*.045f)
        val stroke=2.3.dp.toPx()
        drawCircle(SearchBlue,w*.145f,lens,style=lensStroke)
        drawLine(SearchBlue,Offset(lens.x+w*.105f,lens.y+w*.105f),Offset(center.x+w*.205f,center.y+w*.205f),stroke,StrokeCap.Round)
    }
}
