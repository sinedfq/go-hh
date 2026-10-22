package com.example.trudomix.ui.components

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

suspend fun loadAvatarForCrop(context:Context,uri:Uri):Bitmap=withContext(Dispatchers.IO) {
    val limit=20*1024*1024
    val bytes=context.contentResolver.openInputStream(uri)?.use { input ->
        val output=ByteArrayOutputStream()
        val buffer=ByteArray(8192)
        while(output.size()<=limit) {
            ensureActive()
            val count=input.read(buffer,0,min(buffer.size,limit+1-output.size()))
            if(count<0)break
            output.write(buffer,0,count)
        }
        output.toByteArray()
    }?:error("Не удалось прочитать фотографию")
    require(bytes.isNotEmpty()&&bytes.size<=limit){"Выбери фотографию размером до 20 МБ"}
    val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
    BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
    require(bounds.outWidth>0&&bounds.outHeight>0&&bounds.outWidth.toLong()*bounds.outHeight<=150_000_000L){"Не удалось открыть изображение. Выбери другое фото."}
    require(bounds.outMimeType in listOf("image/jpeg","image/png","image/webp")){"Выбери фотографию JPG, PNG или WebP"}
    var sample=1
    while(maxOf(bounds.outWidth,bounds.outHeight)/sample>2048)sample*=2
    val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888})?:error("Не удалось открыть фотографию")
    val orientation=runCatching{ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix=Matrix().apply {
        when(orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL->setScale(-1f,1f)
            ExifInterface.ORIENTATION_ROTATE_180->setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL->setScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE->{setRotate(90f);postScale(-1f,1f)}
            ExifInterface.ORIENTATION_ROTATE_90->setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE->{setRotate(-90f);postScale(-1f,1f)}
            ExifInterface.ORIENTATION_ROTATE_270->setRotate(-90f)
        }
    }
    if(matrix.isIdentity)bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true).also{if(it!==bitmap)bitmap.recycle()}
}

suspend fun croppedAvatarPart(bitmap:Bitmap,pose:AvatarCropPose):MultipartBody.Part=withContext(Dispatchers.Default) {
    val square=avatarCropSquare(bitmap.width,bitmap.height,pose)
    val edge=square.edge.roundToInt().coerceIn(1,min(bitmap.width,bitmap.height))
    val left=square.left.roundToInt().coerceIn(0,bitmap.width-edge)
    val top=square.top.roundToInt().coerceIn(0,bitmap.height-edge)
    val target=min(1024,edge)
    val result=Bitmap.createBitmap(target,target,Bitmap.Config.ARGB_8888)
    val bytes=try {
        Canvas(result).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap,Rect(left,top,left+edge,top+edge),Rect(0,0,target,target),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        ByteArrayOutputStream().use{output->check(result.compress(Bitmap.CompressFormat.JPEG,90,output)){"Не удалось сохранить фотографию"};output.toByteArray()}
    } finally {result.recycle()}
    require(bytes.size<=4*1024*1024){"Не удалось уменьшить фото до 4 МБ"}
    MultipartBody.Part.createFormData("photo","avatar.jpg",bytes.toRequestBody("image/jpeg".toMediaType()))
}
