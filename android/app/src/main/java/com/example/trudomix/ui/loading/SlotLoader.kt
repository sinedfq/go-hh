package com.example.trudomix.ui.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.local.LoadingStyle

/** Native Canvas rendition of the supplied HTML; paths and gradient stops are preserved. */
@Composable internal fun SlotLogo(dark:Boolean,time:State<Float>,modifier:Modifier=Modifier,style:LoadingStyle=LoadingStyle.IMPACT){
    Box(modifier.drawWithCache{
        val mark=PathParser().parsePathString(SLOT_MARK).toPath()
        val orb=Path().apply{addOval(androidx.compose.ui.geometry.Rect(620.67f,384f,872.67f,636f))}
        val measure=PathMeasure().apply{setPath(mark,false)}
        val orbMeasure=PathMeasure().apply{setPath(orb,true)}
        val traced=Path();val tracedOrb=Path()
        val letter=if(dark)Brush.linearGradient(listOf(Color.White,Color(0xFFDED4FF)),start=Offset.Zero,end=Offset(967f,809f))
            else Brush.linearGradient(listOf(Color(0xFF11102E),Color(0xFF5540CF)),start=Offset.Zero,end=Offset(967f,809f))
        val orbBrush=Brush.linearGradient(0f to Color(0xFFE49CFF),.5f to Color(0xFFA545FB),1f to Color(0xFF4A16D7),start=Offset(620.67f,384f),end=Offset(872.67f,636f))
        val outline=Stroke(5f,cap=StrokeCap.Round,join=StrokeJoin.Round)
        val soft=Stroke(18f,cap=StrokeCap.Round,join=StrokeJoin.Round)
        val strokeColor=if(style==LoadingStyle.NEON)Color(0xFFDCACFF)else Color(0xFFBD84FF)
        onDrawBehind{
            val frame=LoadingStyleMotion.frame(time.value,style)
            withTransform({scale(size.width/1000f,size.height/850f,Offset.Zero)}){
                drawPath(mark,letter,alpha=frame.fill)
                drawCircle(orbBrush,126f,Offset(746.67f,510f),alpha=frame.fill)
                if(style!=LoadingStyle.SHUTTER){
                    traced.reset();measure.getSegment(0f,measure.length*frame.trace,traced,true)
                    tracedOrb.reset();orbMeasure.getSegment(0f,orbMeasure.length*frame.orbTrace,tracedOrb,true)
                    if(style==LoadingStyle.NEON){drawPath(traced,strokeColor,alpha=.15f,style=soft);drawPath(tracedOrb,strokeColor,alpha=.15f,style=soft)}
                    drawPath(traced,strokeColor,style=outline);drawPath(tracedOrb,strokeColor,style=outline)
                }
                if(frame.glow>0f){drawPath(mark,strokeColor,alpha=frame.glow*.18f,style=soft);drawCircle(strokeColor,126f,Offset(746.67f,510f),alpha=frame.glow*.18f,style=soft)}
            }
        }
    })
}

@Composable internal fun SlotImpact(time:State<Float>,modifier:Modifier=Modifier,style:LoadingStyle=LoadingStyle.IMPACT){
    Box(modifier.drawWithCache{
        val radius=size.maxDimension*.8f
        val glow=Brush.radialGradient(0f to Color(0xFFAD70FF).copy(alpha=.32f),.3f to Color(0xFF744BF4).copy(alpha=.15f),1f to Color.Transparent,center=Offset(size.width/2,size.height/2),radius=radius)
        val stroke=Stroke(2.dp.toPx())
        val logoWidth=(size.minDimension*.55f).coerceIn(100.dp.toPx(),310.dp.toPx())
        onDrawBehind{
            val frame=LoadingStyleMotion.frame(time.value,style)
            if(frame.glow>0f)drawRect(glow,alpha=frame.glow)
            if(frame.ringAlpha>0f)drawCircle(Color(0xFFB76AFF),logoWidth*.242f*(.5f+4.5f*frame.ring),alpha=frame.ringAlpha,style=stroke)
        }
    })
}

@Composable internal fun SlotShutters(dark:Boolean,time:State<Float>,modifier:Modifier=Modifier){
    Box(modifier.drawWithCache{
        val panel=Brush.linearGradient(if(dark)listOf(Color(0xFF1D1432),Color(0xFF0E0C1C))else listOf(Color(0xFFE2D7F8),Color(0xFFF4EDFF)),end=Offset(size.width,size.height))
        onDrawBehind{
            val amount=LoadingStyleMotion.frame(time.value,LoadingStyle.SHUTTER).shutter
            if(amount<1f){
                val half=size.height/2
                drawRect(panel,topLeft=Offset(0f,-half*1.03f*amount),size=androidx.compose.ui.geometry.Size(size.width,half))
                drawRect(panel,topLeft=Offset(0f,half+half*1.03f*amount),size=androidx.compose.ui.geometry.Size(size.width,half))
            }
        }
    })
}

@Composable fun StaticLoaderLogo(dark:Boolean,modifier:Modifier=Modifier){
    val final=remember{mutableFloatStateOf(LoadingStyleMotion.duration)}
    SlotLogo(dark,final,modifier)
}
