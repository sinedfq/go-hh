package com.example.trudomix.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.trudomix.data.api.ApiClient
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private object VideoFrames {
    private val cache=object:LruCache<String,Bitmap>(12*1024){override fun sizeOf(key:String,value:Bitmap)=(value.byteCount/1024).coerceAtLeast(1)}
    private val decoding=Semaphore(2)
    suspend fun get(url:String):Bitmap?=withContext(Dispatchers.IO){
        cache.get(url)?:decoding.withPermit{
            cache.get(url)?:run {
                val retriever=MediaMetadataRetriever()
                try{
                    retriever.setDataSource(url,emptyMap())
                    val bitmap=if(Build.VERSION.SDK_INT>=27)retriever.getScaledFrameAtTime(200000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,360,480)else retriever.getFrameAtTime(200000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    bitmap?.also{cache.put(url,it)}
                }catch(e:CancellationException){throw e}catch(_:Exception){null}finally{retriever.release()}
            }
        }
    }
}

/** Native HTTP range reads plus a bounded frame cache; previews never autoplay. */
@Composable
fun VideoPreview(url:String,description:String?,modifier:Modifier=Modifier,showPlay:Boolean=true){
    val fullUrl=ApiClient.mediaUrl(url)
    val frame by produceState<Bitmap?>(null,fullUrl){value=null;if(fullUrl!=null)value=VideoFrames.get(fullUrl)}
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh).semantics(mergeDescendants=true){stateDescription=if(frame!=null)"Превью видео готово"else "Превью загружается"},contentAlignment=Alignment.Center){
        frame?.let{Image(it.asImageBitmap(),description,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
        if(showPlay||frame==null)Icon(Icons.Default.PlayCircle,if(frame==null)description else null,Modifier.size(40.dp),tint=if(frame==null)MaterialTheme.colorScheme.onSurfaceVariant else Color.White)
    }
}
