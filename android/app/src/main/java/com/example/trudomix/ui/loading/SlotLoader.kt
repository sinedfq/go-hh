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
        val pixels=if(style==LoadingStyle.PIXEL){
            val bitmap=android.graphics.Bitmap.createBitmap(34,29,android.graphics.Bitmap.Config.ARGB_8888)
            val canvas=android.graphics.Canvas(bitmap)
            canvas.scale(34f/1000f,29f/850f)
            val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.shader=android.graphics.LinearGradient(0f,0f,967f,809f,if(dark)intArrayOf(-1,0xFFDED4FF.toInt())else intArrayOf(0xFF11102E.toInt(),0xFF5540CF.toInt()),null,android.graphics.Shader.TileMode.CLAMP)
            canvas.drawPath(mark.asAndroidPath(),paint)
            paint.shader=android.graphics.LinearGradient(620.67f,384f,872.67f,636f,intArrayOf(0xFFE49CFF.toInt(),0xFFA545FB.toInt(),0xFF4A16D7.toInt()),floatArrayOf(0f,.5f,1f),android.graphics.Shader.TileMode.CLAMP)
            canvas.drawCircle(746.67f,510f,126f,paint)
            val random=kotlin.random.Random(41)
            val result=buildList{for(y in 0..28)for(x in 0..33){val color=bitmap.getPixel(x,y);if(android.graphics.Color.alpha(color)>=65)add(PixelTile(x,y,Color(color),random.nextFloat()*1.25f+(x+y)/63f*.58f))}}
            bitmap.recycle();result
        }else emptyList()
        onDrawBehind{
            val frame=LoadingStyleMotion.frame(time.value,style)
            withTransform({scale(size.width/1000f,size.height/850f,Offset.Zero)}){
                val t=time.value
                if(style==LoadingStyle.PIXEL&&t<3.05f){
                    val burst=t in .48f.. .76f||t in 1.26f..1.53f||t in 2.08f..2.26f
                    for(tile in pixels){
                        val progress=((t-tile.delay)/.36f).coerceIn(0f,1f)
                        val alpha=progress*(1-((t-2.46f)/.42f).coerceIn(0f,1f))
                        if(alpha<=0f)continue
                        val stripe=tile.y/3
                        val shift=if(burst&&(stripe+(t/.065f).toInt())%4==0)if(stripe%2==0)47f else -38f else 0f
                        val position=Offset(tile.x*1000f/34+shift,tile.y*850f/29)
                        val block=androidx.compose.ui.geometry.Size(1000f/34+1,850f/29+1)
                        if(shift!=0f){drawRect(Color(0xFF40E6FF),position-Offset(15f,0f),block,alpha=alpha*.55f);drawRect(Color(0xFFFF4BD8),position+Offset(12f,0f),block,alpha=alpha*.55f)}
                        drawRect(tile.color,position,block,alpha=alpha)
                    }
                }
                drawPath(mark,letter,alpha=frame.fill)
                drawCircle(orbBrush,126f,Offset(746.67f,510f),alpha=frame.fill)
                if(style!=LoadingStyle.SHUTTER&&style!=LoadingStyle.PIXEL){
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

private data class PixelTile(val x:Int,val y:Int,val color:Color,val delay:Float)

@Composable internal fun SlotImpact(time:State<Float>,modifier:Modifier=Modifier,style:LoadingStyle=LoadingStyle.IMPACT,dark:Boolean=true){
    Box(modifier.drawWithCache{
        val radius=size.maxDimension*.8f
        val wave=if(dark)Color(0xFFB76AFF)else Color(0xFF7745CC)
        val glow=Brush.radialGradient(0f to wave.copy(alpha=if(dark).32f else .16f),.3f to wave.copy(alpha=.08f),1f to Color.Transparent,center=Offset(size.width/2,size.height/2),radius=radius)
        val stroke=Stroke(2.dp.toPx())
        val logoWidth=(size.minDimension*.40f).coerceIn(80.dp.toPx(),220.dp.toPx())
        onDrawBehind{
            val frame=LoadingStyleMotion.frame(time.value,style)
            if(frame.glow>0f)drawRect(glow,alpha=frame.glow)
            if(frame.ringAlpha>0f)drawCircle(wave,logoWidth*.242f*(.5f+4.5f*frame.ring),alpha=frame.ringAlpha,style=stroke)
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
