package com.example.trudomix.ui.loading

import com.example.trudomix.startup.StartupReadiness
import com.example.trudomix.data.local.LoadingStyle
import com.example.trudomix.data.local.LoadingStyles
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val MARK="M12 24 Q12 12 25 12 H99 Q108 12 108 22 Q108 43 87 43 H63 Q55 43 55 54 V79 Q55 89 38 96 Q32 99 32 91 V65 Q32 52 45 43 H25 Q12 43 12 32Z"
class SplashViewModel:ViewModel(){val elapsed=mutableFloatStateOf(0f);var animationDone by mutableStateOf(false);var closed by mutableStateOf(false)}

@Composable internal fun durationScale():Float {
    val resolver=LocalContext.current.contentResolver
    fun read()=Settings.Global.getFloat(resolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)
    var value by remember(resolver){mutableFloatStateOf(read())}
    DisposableEffect(resolver){
        val observer=object:ContentObserver(Handler(Looper.getMainLooper())){override fun onChange(selfChange:Boolean){value=read()}}
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
        onDispose{resolver.unregisterContentObserver(observer)}
    }
    return value
}

/** One process-start overlay. Navigation and requests initialize underneath it. */
@Composable fun StartupSplashHost(dark:Boolean,window:android.view.Window,model:SplashViewModel=viewModel()){
    val visible by StartupReadiness.visible.collectAsStateWithLifecycle()
    if(model.closed||!visible)return
    val ready by StartupReadiness.ready.collectAsStateWithLifecycle()
    val problem by StartupReadiness.failure.collectAsStateWithLifecycle()
    val busy by StartupReadiness.busy.collectAsStateWithLifecycle()
    val scale=durationScale()
    val opacity=remember{Animatable(1f)}
    LaunchedEffect(model.animationDone){StartupReadiness.animationDone.value=model.animationDone}
    BackHandler{}
    DisposableEffect(window,dark){
        val bars=WindowCompat.getInsetsController(window,window.decorView)
        val oldStatus=bars.isAppearanceLightStatusBars;val oldNavigation=bars.isAppearanceLightNavigationBars
        val oldStatusColor=window.statusBarColor;val oldNavigationColor=window.navigationBarColor
        bars.isAppearanceLightStatusBars=!dark;bars.isAppearanceLightNavigationBars=!dark
        val background=if(dark)0xFF080816.toInt()else 0xFFF8F7FC.toInt()
        window.statusBarColor=background;window.navigationBarColor=background
        onDispose{
            bars.isAppearanceLightStatusBars=oldStatus;bars.isAppearanceLightNavigationBars=oldNavigation
            window.statusBarColor=oldStatusColor;window.navigationBarColor=oldNavigationColor
        }
    }
    LaunchedEffect(model,scale){
        if(scale==0f){model.elapsed.floatValue=SplashTiming.duration;model.animationDone=true}
        else if(!model.animationDone){
            var previous=SystemClock.elapsedRealtime()
            while(model.elapsed.floatValue<SplashTiming.duration){
                withFrameNanos{}
                val now=SystemClock.elapsedRealtime()
                model.elapsed.floatValue=(model.elapsed.floatValue+(now-previous)/1000f/scale).coerceAtMost(SplashTiming.duration)
                previous=now
            }
            model.animationDone=true
        }
    }
    LaunchedEffect(ready,problem,model.animationDone,scale){
        if(ready&&problem==null&&model.animationDone){
            StartupReadiness.revealing.value=true
            withFrameNanos{}
            opacity.animateTo(0f,tween(if(scale==0f)0 else 280));model.closed=true;StartupReadiness.dismiss()
        }else{StartupReadiness.revealing.value=false;opacity.snapTo(1f)}
    }
    if(model.animationDone&&problem!=null)SplashConnectionState(problem!!,busy)
    else SplashScreen(dark,model.elapsed,Modifier.graphicsLayer{alpha=opacity.value})
}

@Composable fun SplashScreen(dark:Boolean,time:State<Float>,modifier:Modifier=Modifier,style:LoadingStyle?=null,blockInput:Boolean=true){
    val selected by LoadingStyles.style.collectAsState()
    val effective=style?:selected
    val background=if(dark)Color(0xFF080816)else Color(0xFFF8F7FC)
    val foreground=if(dark)Color(0xFFFAF8FF)else Color(0xFF191431)
    val stage by remember(time){derivedStateOf{SplashTiming.stage(time.value)}}
    BoxWithConstraints(modifier.fillMaxSize().background(background).then(if(blockInput)Modifier.pointerInput(Unit){awaitPointerEventScope{while(true){awaitPointerEvent().changes.forEach{it.consume()}}}}else Modifier).semantics{liveRegion=LiveRegionMode.Polite;contentDescription="Загрузка Трудомикс";stateDescription=stage}){
        val width=SplashTiming.logoSize(maxWidth.value,maxHeight.value).dp
        val height=width*850f/1000f
        SlotImpact(time,Modifier.fillMaxSize(),effective)

        SlotLogo(dark,time,Modifier.align(Alignment.Center).size(width,height),effective)
        AnimatedWordmark(time,foreground,Modifier.align(Alignment.Center).offset(y=height/2+25.dp))
        if(effective==LoadingStyle.SHUTTER)SlotShutters(dark,time,Modifier.fillMaxSize())
    }
}

@Composable fun TrudomixLogo(color:Color,modifier:Modifier=Modifier){
    val path=remember{PathParser().parsePathString(MARK).toPath()}
    Canvas(modifier){withTransform({scale(size.width/118f,size.height/105f,Offset.Zero)}){
        drawPath(path,color);drawCircle(Color(0xFF9653F5),12f,Offset(73f,59f))
    }}
}

@Composable fun AnimatedLines(time:State<Float>,modifier:Modifier=Modifier){
    val flightSeed=androidx.compose.runtime.saveable.rememberSaveable{kotlin.random.Random.nextInt()}
    Box(modifier.drawWithCache{
        val w=size.width;val h=size.height
        val measures=(0..11).map{index->
            val curves=SplashFlights.curves(w,h,flightSeed,index,12.dp.toPx()+size.minDimension*.018f,4.dp.toPx())
            val path=Path().apply{
                moveTo(curves.first().start.x,curves.first().start.y)
                curves.forEach{curve->cubicTo(curve.first.x,curve.first.y,curve.second.x,curve.second.y,curve.end.x,curve.end.y)}
            }
            PathMeasure().apply{setPath(path,false)}
        }
        val partial=List(12){Path()};val stroke=Stroke(1.5.dp.toPx(),cap=StrokeCap.Round)
        onDrawBehind{
            val t=time.value;val fade=1-SplashTiming.smooth((t-2.65f)/.15f)
            if(fade>0f)measures.forEachIndexed{index,measure->
                val delay=(index%3)*.08f+(index/3)*.035f
                val segment=SplashTiming.lineWindow(t,delay)
                val path=partial[index];path.reset()
                measure.getSegment(measure.length*segment.tail,measure.length*segment.head,path,true)
                drawPath(path,Color(SplashTiming.lineColors[index]),alpha=fade*segment.alpha,style=stroke)
            }
        }
    })
}

@Composable fun CircularWave(time:State<Float>,dark:Boolean,logoSize:androidx.compose.ui.unit.Dp,modifier:Modifier=Modifier){
    val color=if(dark)Color(0xFFD4D4D8)else Color(0xFFA1A1AA)
    Box(modifier.drawWithCache{
        val start=logoSize.toPx()*.8f;val end=maxOf(start+20.dp.toPx(),size.minDimension*.46f)
        val stroke=Stroke(1.4.dp.toPx());val soft=Stroke(4.dp.toPx())
        onDrawBehind{
            val alpha=SplashTiming.ringAlpha(time.value)
            if(alpha>0f){
                val radius=start+(end-start)*SplashTiming.smooth(SplashTiming.ringProgress(time.value))
                // A faint wider stroke provides a soft edge without a per-frame blur pass.
                drawCircle(color.copy(alpha=alpha*.13f),radius,style=soft)
                drawCircle(color.copy(alpha=alpha),radius,style=stroke)
            }
        }
    })
}

@Composable fun AnimatedWordmark(time:State<Float>,color:Color,modifier:Modifier=Modifier){
    val shown by remember(time){derivedStateOf{SplashTiming.wordmark(time.value)>0f}}
    Text("ТРУДОМИКС",modifier.then(if(shown)Modifier else Modifier.clearAndSetSemantics{}).graphicsLayer{
        val value=SplashTiming.wordmark(time.value);alpha=value;translationY=(1-value)*10.dp.toPx()
    },color=color,fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=22.sp,letterSpacing=2.5.sp)
}

@Composable fun LoadingStylePreview(style:LoadingStyle,replay:Int,modifier:Modifier=Modifier){
    val time=remember(style,replay){mutableFloatStateOf(0f)}
    val scale=durationScale()
    LaunchedEffect(time,scale){
        if(scale==0f)time.floatValue=SplashTiming.duration
        else{
            var previous=withFrameNanos{it}
            while(time.floatValue<SplashTiming.duration){
                val now=withFrameNanos{it}
                time.floatValue=(time.floatValue+(now-previous)/1_000_000_000f/scale).coerceAtMost(SplashTiming.duration)
                previous=now
            }
        }
    }
    SplashScreen(MaterialTheme.colorScheme.background.luminance()<.5f,time,modifier.clip(RoundedCornerShape(16.dp)),style,blockInput=false)
}
