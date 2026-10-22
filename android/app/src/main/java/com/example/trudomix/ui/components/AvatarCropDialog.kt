package com.example.trudomix.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AvatarCropDialog(original:Bitmap,busy:Boolean,error:String?,onDismiss:()->Unit,onConfirm:(Bitmap,AvatarCropPose)->Unit) {
    var rotation by remember(original){mutableIntStateOf(0)}
    var bitmap by remember(original){mutableStateOf(original)}
    var rotating by remember{mutableStateOf(false)}
    var pose by remember(original){mutableStateOf(AvatarCropPose())}
    LaunchedEffect(original,rotation) {
        rotating=true
        try {
            bitmap=withContext(Dispatchers.Default){if(rotation==0)original else Bitmap.createBitmap(original,0,0,original.width,original.height,Matrix().apply{postRotate(rotation*90f)},true)}
            pose=AvatarCropPose()
        }finally{rotating=false}
    }
    val locked=busy||rotating
    val image=remember(bitmap){bitmap.asImageBitmap()}
    val colors=MaterialTheme.colorScheme
    Dialog(onDismissRequest={if(!busy)onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=colors.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick=onDismiss,enabled=!busy){Icon(Icons.Default.Close,"Отмена")}
                    Text("Кадрирование фото",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=2)
                    Button(onClick={onConfirm(bitmap,pose)},enabled=!locked) {
                        if(busy)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=colors.onPrimary)else Text("Сохранить фото",style=MaterialTheme.typography.labelMedium)
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    val side=minOf(maxWidth-32.dp,maxHeight-24.dp,560.dp).coerceAtLeast(1.dp)
                    Box(Modifier.fillMaxSize().background(Color(0xFF0D1117)),contentAlignment=Alignment.Center) {
                        Canvas(Modifier.size(side).clip(RoundedCornerShape(20.dp)).semantics {
                            contentDescription="Круглая область аватарки. Фото можно перемещать и увеличивать."
                            customActions=listOf(
                                CustomAccessibilityAction("Сдвинуть фото влево"){if(!busy)pose=transformAvatarCrop(bitmap.width,bitmap.height,pose,1f,panX=-.1f);!busy},
                                CustomAccessibilityAction("Сдвинуть фото вправо"){if(!busy)pose=transformAvatarCrop(bitmap.width,bitmap.height,pose,1f,panX=.1f);!busy},
                                CustomAccessibilityAction("Сдвинуть фото вверх"){if(!busy)pose=transformAvatarCrop(bitmap.width,bitmap.height,pose,1f,panY=-.1f);!busy},
                                CustomAccessibilityAction("Сдвинуть фото вниз"){if(!busy)pose=transformAvatarCrop(bitmap.width,bitmap.height,pose,1f,panY=.1f);!busy})
                        }.pointerInput(bitmap,locked) {
                            detectTransformGestures { centroid,pan,zoom,_ ->
                                if(!locked&&size.width>0&&size.height>0)pose=transformAvatarCrop(bitmap.width,bitmap.height,pose,zoom,pan.x/size.width,pan.y/size.height,centroid.x/size.width,centroid.y/size.height)
                            }
                        }) {
                            val crop=avatarCropSquare(bitmap.width,bitmap.height,pose)
                            val edge=crop.edge.roundToInt().coerceIn(1,min(bitmap.width,bitmap.height))
                            drawImage(image,srcOffset=IntOffset(crop.left.roundToInt().coerceIn(0,bitmap.width-edge),crop.top.roundToInt().coerceIn(0,bitmap.height-edge)),srcSize=IntSize(edge,edge),dstSize=IntSize(size.width.roundToInt(),size.height.roundToInt()),filterQuality=FilterQuality.High)
                            val mask=Path().apply{fillType=PathFillType.EvenOdd;addRect(androidx.compose.ui.geometry.Rect(Offset.Zero,size));addOval(androidx.compose.ui.geometry.Rect(Offset.Zero,size))}
                            drawPath(mask,Color.Black.copy(alpha=.6f))
                            drawCircle(Color.White.copy(alpha=.9f),radius=size.minDimension/2-1.dp.toPx(),style=Stroke(2.dp.toPx()))
                        }
                        if(rotating)CircularProgressIndicator()
                    }
                }
                Column(Modifier.align(Alignment.CenterHorizontally).widthIn(max=600.dp).fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("Двигай фото внутри круга · Масштаб — двумя пальцами",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                        Row(Modifier.widthIn(max=480.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text("Масштаб",style=MaterialTheme.typography.labelMedium)
                            Slider(pose.zoom,{value->pose=transformAvatarCrop(bitmap.width,bitmap.height,pose,value/pose.zoom)},enabled=!locked,valueRange=1f..6f,modifier=Modifier.weight(1f).semantics{contentDescription="Масштаб фото"})
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick={rotation=(rotation+1)%4},enabled=!locked){Icon(Icons.Default.RotateRight,null);Spacer(Modifier.width(8.dp));Text("Повернуть")}
                            TextButton(onClick={rotation=0;pose=AvatarCropPose()},enabled=!locked){Text("Сбросить")}
                        }
                        error?.let{Text(it,color=colors.error,style=MaterialTheme.typography.bodySmall)}
                }
            }
        }
    }
}
