package com.example.trudomix.ui.screens.companies

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.Company
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CompanyStory(val url:String,val video:Boolean=false,val caption:String="")
fun companyStories(company:Company)=company.photos.map{CompanyStory(it)}+company.videos.map{CompanyStory(it,true)}

@Composable
fun CompanyStories(company:Company) {
    val stories=remember(company.photos,company.videos){companyStories(company)}
    var opened by remember(company.id){mutableStateOf<Int?>(null)}
    if(stories.isEmpty())return
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Жизнь команды",style=MaterialTheme.typography.titleLarge)
        Text("Фото и видео · нажми, чтобы посмотреть",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            itemsIndexed(stories,key={_,story->story.url}){index,story->
                Box(Modifier.width(124.dp).height(180.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer).clickable{opened=index},contentAlignment=Alignment.Center) {
                    if(story.video)Icon(Icons.Default.PlayCircle,"Видео компании ${index+1}",Modifier.size(44.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)
                    else AsyncImage(ApiClient.mediaUrl(story.url),"Фото компании ${index+1}",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    Text("${index+1}",Modifier.align(Alignment.BottomStart).padding(10.dp).background(Color.Black.copy(alpha=.6f),RoundedCornerShape(8.dp)).padding(horizontal=8.dp,vertical=4.dp),color=Color.White,style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    opened?.let{initial->CompanyStoryViewer(company.name,stories,initial){opened=null}}
}

@Composable
internal fun CompanyStoryViewer(name:String,stories:List<CompanyStory>,initial:Int,onDelete:((Int)->Unit)?=null,onClose:()->Unit) {
    val pager=rememberPagerState(initialPage=initial.coerceIn(stories.indices),pageCount={stories.size})
    val scope=rememberCoroutineScope()
    var paused by remember{mutableStateOf(false)}
    var progress by remember(pager.currentPage){mutableFloatStateOf(0f)}
    var foreground by remember{mutableStateOf(true)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,_->foreground=lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}
    fun next(){if(pager.currentPage<stories.lastIndex)scope.launch{pager.animateScrollToPage(pager.currentPage+1)}else onClose()}
    LaunchedEffect(pager.currentPage,paused,foreground,pager.isScrollInProgress){
        if(!stories[pager.currentPage].video&&!paused&&foreground&&!pager.isScrollInProgress){while(progress<1f){delay(50);progress+=.01f};next()}
    }
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.fillMaxSize(),color=Color(0xFF080C12)){
            Box(Modifier.fillMaxSize().systemBarsPadding()){
                HorizontalPager(pager,Modifier.fillMaxSize()) { page->
                    val story=stories[page]
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                        if(story.video&&page==pager.currentPage) StoryVideo(story.url,!paused&&foreground,onProgress={progress=it},onFinished={next()})
                        else if(!story.video){
                            var failed by remember(story.url){mutableStateOf(false)}
                            if(failed)Text("Не удалось загрузить фото. Перейди к следующей истории.",Modifier.padding(30.dp),color=Color.White)
                            else AsyncImage(ApiClient.mediaUrl(story.url),"История ${page+1}",Modifier.fillMaxSize(),contentScale=ContentScale.Fit,onError={failed=true})
                        }
                Row(Modifier.fillMaxWidth().fillMaxHeight(.82f).align(Alignment.Center)){
                    Box(Modifier.weight(1f).fillMaxHeight().pointerInput(pager.currentPage){detectTapGestures(onTap={if(pager.currentPage>0)scope.launch{pager.animateScrollToPage(pager.currentPage-1)}},onPress={paused=true;tryAwaitRelease();paused=false})})
                    Box(Modifier.weight(1f).fillMaxHeight().pointerInput(pager.currentPage){detectTapGestures(onTap={next()},onPress={paused=true;tryAwaitRelease();paused=false})})
                }
                    }
                }
                Column(Modifier.align(Alignment.TopCenter).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    if(onDelete!=null)TextButton(onClick={onDelete(pager.currentPage)}){Text("Удалить историю",color=Color.White)}
                    if(stories[pager.currentPage].caption.isNotBlank())Text(stories[pager.currentPage].caption,Modifier.fillMaxWidth().background(Color.Black.copy(alpha=.65f),RoundedCornerShape(12.dp)).padding(12.dp),color=Color.White)
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){stories.indices.forEach{index->LinearProgressIndicator(progress={if(index<pager.currentPage)1f else if(index==pager.currentPage)progress.coerceIn(0f,1f)else 0f},modifier=Modifier.weight(1f),color=Color.White,trackColor=Color.White.copy(alpha=.25f))}}
                    Row(verticalAlignment=Alignment.CenterVertically){Text(name,Modifier.weight(1f),color=Color.White,style=MaterialTheme.typography.titleMedium);Text("${pager.currentPage+1} / ${stories.size}",color=Color.White);IconButton(onClick=onClose){Icon(Icons.Default.Close,"Закрыть истории",tint=Color.White)}}
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={if(pager.currentPage>0)scope.launch{pager.animateScrollToPage(pager.currentPage-1)}}){Text("Назад",color=Color.White)};IconButton(onClick={paused=!paused}){Icon(if(paused)Icons.Default.PlayArrow else Icons.Default.Pause,if(paused)"Продолжить историю" else "Пауза",tint=Color.White)};TextButton(onClick={next()}){Text(if(pager.currentPage==stories.lastIndex)"Готово"else "Далее",color=Color.White)}}
            }
        }
    }
}

@Composable
private fun StoryVideo(url:String,playing:Boolean,onProgress:(Float)->Unit,onFinished:()->Unit){
    var player by remember(url){mutableStateOf<VideoView?>(null)}
    var ready by remember(url){mutableStateOf(false)}
    var rendered by remember(url){mutableStateOf(false)}
    var error by remember(url){mutableStateOf(false)}
    val currentFinish by rememberUpdatedState(onFinished)
    LaunchedEffect(playing,ready){player?.let{if(playing&&ready)it.start()else it.pause()}}
    LaunchedEffect(player,ready){while(ready&&!error){player?.let{if(it.duration>0)onProgress(it.currentPosition.toFloat()/it.duration)};delay(100)}}
    DisposableEffect(url){onDispose{player?.stopPlayback()}}
    Box(Modifier.fillMaxSize().semantics{contentDescription=if(error)"Ошибка видео"else if(!rendered)"Загрузка видео"else if(playing)"Видео воспроизводится"else "Видео на паузе"},contentAlignment=Alignment.Center){
        AndroidView(factory={context->VideoView(context).apply{player=this;setOnPreparedListener{ready=true};setOnInfoListener{_,what,_->if(what==android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)rendered=true;false};setOnCompletionListener{currentFinish()};setOnErrorListener{_,_,_->error=true;true};setVideoURI(Uri.parse(ApiClient.mediaUrl(url))) }},modifier=Modifier.fillMaxSize())
        if(error)Text("Не удалось воспроизвести видео. Попробуй следующую историю.",Modifier.padding(30.dp).background(Color.Black.copy(alpha=.8f)),color=Color.White)
        else if(!ready)CircularProgressIndicator(color=Color.White)
    }
}
