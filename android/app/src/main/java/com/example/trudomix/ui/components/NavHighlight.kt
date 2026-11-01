package com.example.trudomix.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.trudomix.data.local.NavAnimationStyle
import com.example.trudomix.ui.loading.durationScale
import kotlinx.coroutines.*
import kotlin.math.*

/** Matches the unchanged Row weights, including the larger central search section. */
internal fun navSectionEdges(width:Float)=FloatArray(6){i->width*(i+if(i>2).15f else 0f)/5.15f}
internal fun navRevealRadius(width:Float,height:Float)=hypot(width/2,height/2)

internal data class NavRevealLayer(val slot:Int,val fraction:Float,val opacity:Float)

@Stable internal class NavHighlightState(initial:Int){
    val position=Animatable(initial.coerceAtLeast(0).toFloat())
    val reveal=Animatable(1f)
    val crest=Animatable(1f)
    var slot by mutableIntStateOf(initial);private set
    var style by mutableStateOf(NavAnimationStyle.SMOOTH);private set
    var previous by mutableStateOf<List<NavRevealLayer>>(emptyList());private set

    suspend fun settle(target:Int,choice:NavAnimationStyle){
        slot=target;style=choice;previous=emptyList()
        position.snapTo(target.coerceAtLeast(0).toFloat());reveal.snapTo(1f);crest.snapTo(1f)
    }
    suspend fun select(target:Int,choice:NavAnimationStyle,animate:Boolean){
        if(!animate||target<0||slot<0||choice!=style){settle(target,choice);return}
        if(target==slot)return
        val p=reveal.value
        // Snapshot visible reveals before cancellation/retargeting; allocation occurs only on a tap.
        previous=if(choice==NavAnimationStyle.CENTER_REVEAL)buildList{
            previous.forEach{val alpha=it.opacity*(1-p);if(alpha>.02f)add(it.copy(opacity=alpha))}
            if(p>0f)add(NavRevealLayer(slot,p,1f))
        }.takeLast(8)else emptyList()
        slot=target
        reveal.snapTo(0f)
        if(choice==NavAnimationStyle.ELASTIC)crest.snapTo(.65f)
        coroutineScope{
            launch{position.animateTo(target.toFloat(),if(choice==NavAnimationStyle.ELASTIC)spring(dampingRatio=.82f,stiffness=420f)else tween(360,easing=FastOutSlowInEasing))}
            launch{reveal.animateTo(1f,tween(if(choice==NavAnimationStyle.CENTER_REVEAL)340 else 360,easing=FastOutSlowInEasing))}
            if(choice==NavAnimationStyle.ELASTIC)launch{crest.animateTo(1f,spring(dampingRatio=.72f,stiffness=350f))}
        }
    }
}

@Composable internal fun rememberNavHighlight(target:Int,style:NavAnimationStyle):NavHighlightState{
    val state=remember{NavHighlightState(target)}
    val scale=durationScale()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(target,style,scale,lifecycle){
        if(scale==0f||style==NavAnimationStyle.NONE){state.settle(target,style);return@LaunchedEffect}
        // Leaving RESUMED cancels finite transitions and holds their final state.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
            try{state.select(target,style,true);awaitCancellation()}
            finally{if(!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))withContext(NonCancellable){state.settle(target,style)}}
        }
    }
    return state
}

/** Draw-only reads: progress does not recompose icons, labels, or destination screens. */
internal fun Modifier.navHighlight(state:NavHighlightState,background:Color,fill:Color,borderColor:Color)=drawWithCache{
    val edges=navSectionEdges(size.width)
    val outline=Path()
    val radius=size.height/2
    val border=Stroke(1.dp.toPx())
    val borderBrush=Brush.verticalGradient(listOf(Color.White.copy(alpha=.36f),borderColor.copy(alpha=.45f)))
    val sheen=Brush.verticalGradient(listOf(Color.White.copy(alpha=.06f),Color.Transparent))
    val rightArc=androidx.compose.ui.geometry.Rect(size.width-size.height,0f,size.width,size.height)
    val leftArc=androidx.compose.ui.geometry.Rect(0f,0f,size.height,size.height)
    onDrawBehind{
        val choice=state.style
        val position=state.position.value.coerceIn(0f,4f)
        val i=floor(position).toInt();val next=(i+1).coerceAtMost(4);val part=position-i
        val left=edges[i]+(edges[next]-edges[i])*part
        val right=edges[i+1]+(edges[next+1]-edges[i+1])*part
        val shoulder=minOf((right-left)*.35f,20.dp.toPx(),((size.width-size.height)/2).coerceAtLeast(0f))
        val center=((left+right)/2).coerceIn(radius+shoulder,size.width-radius-shoulder)
        val wave=state.slot>=0&&(choice==NavAnimationStyle.SMOOTH||choice==NavAnimationStyle.ELASTIC)
        val half=minOf((right-left)*.65f,center-radius,size.width-radius-center).coerceAtLeast(0f)
        val lift=if(wave)minOf(6.dp.toPx(),size.height*.10f)*(if(choice==NavAnimationStyle.ELASTIC)state.crest.value.coerceIn(.5f,1.12f)else 1f) else 0f
        outline.reset();outline.moveTo(radius,0f)
        if(wave&&half>0f){
            outline.lineTo(center-half,0f)
            outline.cubicTo(center-half*.55f,0f,center-half*.45f,-lift,center,-lift)
            outline.cubicTo(center+half*.45f,-lift,center+half*.55f,0f,center+half,0f)
        }
        outline.lineTo(size.width-radius,0f)
        outline.arcTo(rightArc,-90f,180f,false)
        outline.lineTo(radius,size.height)
        outline.arcTo(leftArc,90f,180f,false);outline.close()
        drawPath(outline,background)
        drawPath(outline,sheen)
        clipPath(outline){
            if(state.slot>=0){
                if(choice==NavAnimationStyle.CENTER_REVEAL){
                    fun drawReveal(slot:Int,fraction:Float,alpha:Float){
                        if(slot !in 0..4||fraction<=0f||alpha<=0f)return
                        val x=edges[slot];val width=edges[slot+1]-x
                        clipRect(x,-lift,edges[slot+1],size.height){
                            if(fraction>=1f)drawRect(fill.copy(alpha=fill.alpha*alpha),Offset(x,-lift),Size(width,size.height+lift))
                            else drawCircle(fill.copy(alpha=fill.alpha*alpha),navRevealRadius(width,size.height)*fraction,Offset(x+width/2,size.height/2))
                        }
                    }
                    val progress=state.reveal.value
                    state.previous.forEach{drawReveal(it.slot,it.fraction,it.opacity*(1-progress))}
                    drawReveal(state.slot,progress,1f)
                }else{
                    val stretch=if(choice==NavAnimationStyle.ELASTIC)minOf(abs(state.position.velocity)*.012f,.18f)*(right-left)else 0f
                    val extendLeft=if(state.position.velocity<0f)stretch else 0f
                    val extendRight=if(state.position.velocity>0f)stretch else 0f
                    drawRect(fill,Offset(left-extendLeft,-lift),Size(right-left+extendLeft+extendRight,size.height+lift))
                }
            }
        }
        drawPath(outline,borderBrush,style=border)
    }
}
