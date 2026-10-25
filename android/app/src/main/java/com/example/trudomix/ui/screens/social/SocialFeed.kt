package com.example.trudomix.ui.screens.social

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.api.userErrorMessage
import com.example.trudomix.data.local.SessionIdentity
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.*
import com.example.trudomix.ui.components.CompanyAvatar
import com.example.trudomix.ui.components.VideoPreview
import com.example.trudomix.ui.screens.companies.CompanyStory
import com.example.trudomix.ui.screens.companies.CompanyStoryViewer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SocialState(val posts:List<SocialPost> = emptyList(),val stories:List<TimedCompanyStory> = emptyList(),val scope:String="all",val next:Long=0,val loading:Boolean=false,val error:String?=null,val busy:Set<Long> = emptySet())
class SocialViewModel:ViewModel(){
    val state=MutableStateFlow(SocialState())
    private var loadJob:Job?=null
    init{load()}
    fun load(more:Boolean=false,scope:String=state.value.scope){
        if(more&&(state.value.loading||state.value.next==0L))return
        loadJob?.cancel();val before=if(more)state.value.next else null
        state.value=state.value.copy(scope=scope,loading=true,error=null,posts=if(scope==state.value.scope)state.value.posts else emptyList())
        loadJob=viewModelScope.launch{try{
            val page=ApiClient.api.socialFeed(scope,before).bodyOrThrow()
            state.value=state.value.copy(posts=if(more)state.value.posts+page.posts else page.posts,next=page.next_cursor)
            val stories=ApiClient.api.timedStories().bodyOrThrow();state.value=state.value.copy(stories=stories)
        }catch(e:CancellationException){throw e}catch(e:Exception){state.value=state.value.copy(error=userErrorMessage(e))}finally{if(isActive)state.value=state.value.copy(loading=false)}}
    }
    private fun action(id:Long,block:suspend()->Unit){if(id in state.value.busy)return;state.value=state.value.copy(busy=state.value.busy+id,error=null);viewModelScope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){state.value=state.value.copy(error=userErrorMessage(e))}finally{state.value=state.value.copy(busy=state.value.busy-id)}}}
    fun like(post:SocialPost)=action(post.id){val result=(if(post.liked)ApiClient.api.unlikePost(post.id)else ApiClient.api.likePost(post.id)).bodyOrThrow();state.value=state.value.copy(posts=state.value.posts.map{if(it.id==post.id)it.copy(liked=result.liked,likes=result.likes)else it})}
    fun delete(post:SocialPost)=action(post.id){ApiClient.api.deletePost(post.id).checkSuccess();state.value=state.value.copy(posts=state.value.posts.filter{it.id!=post.id})}
    fun commentCount(id:Long,delta:Int){state.value=state.value.copy(posts=state.value.posts.map{if(it.id==id)it.copy(comments=(it.comments+delta).coerceAtLeast(0))else it})}
    fun refreshStories(){viewModelScope.launch{try{state.value=state.value.copy(stories=ApiClient.api.timedStories().bodyOrThrow())}catch(e:CancellationException){throw e}catch(_:Exception){/* Pull-to-refresh reports errors without interrupting reading. */}}}
}

private fun socialDate(value:String)=runCatching{DateTimeFormatter.ofPattern("dd.MM · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))}.getOrDefault("")
fun activeStories(items:List<TimedCompanyStory>,now:Long=System.currentTimeMillis())=items.filter{runCatching{Instant.parse(it.expires_at).toEpochMilli()>now}.getOrDefault(false)}

@Composable
fun SocialHomeHeader(state:SocialState,model:SocialViewModel){
    var composePost by rememberSaveable{mutableStateOf(false)};var composeStory by rememberSaveable{mutableStateOf(false)}
    val identity by SessionIdentity.user.collectAsState()
    val employer=identity?.role in listOf("employer","admin")
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)){
        TimedStoriesBar(state.stories,onRefresh={model.load()})
        Text("Люди и команды",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Делись опытом, следи за новостями и общайся с командами.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(onClick={composePost=true},Modifier.weight(1f)){Icon(Icons.Default.Edit,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Написать пост")}
            if(employer)OutlinedButton(onClick={composeStory=true}){Text("История +")}
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf("all" to "Все публикации","following" to "Мои подписки")){(value,label)->FilterChip(state.scope==value,{model.load(scope=value)},label={Text(label)})}}
        state.error?.let{Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={model.load()}){Text("Повторить загрузку")}}
        if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(state.posts.isEmpty()&&!state.loading&&state.error==null)OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Text(if(state.scope=="following")"Подпишись на компании: их новости появятся здесь."else "Стань автором первого поста о работе и команде.",Modifier.padding(20.dp))}
    }
    if(composePost)SocialEditor(false,employer,{composePost=false}){model.load()}
    if(composeStory)SocialEditor(true,true,{composeStory=false}){model.load()}
}

@Composable
fun TimedStoriesBar(stories:List<TimedCompanyStory>,onRefresh:()->Unit={}){
    var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(Unit){while(true){delay(1000);now=System.currentTimeMillis()}}
    val groups=activeStories(stories,now).groupBy{it.company_id}
    var opened by remember{mutableStateOf<Int?>(null)};var deleting by remember{mutableStateOf<TimedCompanyStory?>(null)}
    var error by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Истории · 24 часа",style=MaterialTheme.typography.titleLarge)
        if(groups.isEmpty())Text("Новые истории компаний из подписок появятся здесь.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){
            items(groups.entries.toList(),key={it.key}){group->val first=group.value.first();OutlinedCard(onClick={opened=group.key},modifier=Modifier.width(112.dp),shape=RoundedCornerShape(22.dp),border=BorderStroke(2.dp,MaterialTheme.colorScheme.primary)){
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Box(Modifier.fillMaxWidth().height(120.dp).testTag("story-preview-${group.key}"),contentAlignment=Alignment.Center){
                        val preview=group.value.maxBy{it.id}
                        if(preview.media_type=="video")VideoPreview(preview.media_url,null,Modifier.fillMaxSize(),showPlay=false)
                        else AsyncImage(ApiClient.mediaUrl(preview.media_url),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.22f)))
                        CompanyAvatar(first.company_logo,first.company_name,48.dp)
                    }
                    Text(first.company_name,Modifier.padding(horizontal=8.dp),maxLines=2,style=MaterialTheme.typography.labelMedium)
                    Text("Историй: ${group.value.size}",Modifier.padding(bottom=12.dp),style=MaterialTheme.typography.labelSmall)
                }
            }}
        }
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }
    opened?.let{id->groups[id]?.sortedBy{it.id}?.let{selected->key(selected.map{it.id}){CompanyStoryViewer(selected.first().company_name,selected.map{CompanyStory(it.media_url,it.media_type=="video",it.caption)},0,onDelete=if(selected.first().can_delete){{index->deleting=selected[index];opened=null}}else null){opened=null}}}}
    deleting?.let{story->AlertDialog(onDismissRequest={if(!busy)deleting=null},title={Text("Удалить историю?")},text={Text("Она сразу исчезнет у подписчиков.")},confirmButton={TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{ApiClient.api.deleteStory(story.id).checkSuccess();deleting=null;onRefresh()}catch(e:CancellationException){throw e}catch(e:Exception){error=userErrorMessage(e);deleting=null}finally{busy=false}}}){Text("Удалить")}},dismissButton={TextButton(enabled=!busy,onClick={deleting=null}){Text("Отмена")}})}
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SocialPostCard(post:SocialPost,busy:Boolean,onLike:()->Unit,onDelete:()->Unit,onCommentCount:(Int)->Unit){
    var comments by remember{mutableStateOf(false)};var confirm by remember{mutableStateOf(false)};var media by remember{mutableStateOf(false)}
    OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                CompanyAvatar(post.author_photo,post.author_name,44.dp)
                Column(Modifier.weight(1f)){Text(post.author_name,fontWeight=FontWeight.SemiBold);Text("${if(post.company_id!=null)"Компания"else "Личный профиль"} · ${socialDate(post.created_at)}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                if(post.can_delete)IconButton(onClick={confirm=true},enabled=!busy){Icon(Icons.Default.DeleteOutline,"Удалить пост")}
            }
            if(post.body.isNotBlank())Text(post.body,style=MaterialTheme.typography.bodyLarge)
            if(post.media_url.isNotBlank())OutlinedCard(onClick={media=true},shape=RoundedCornerShape(16.dp)){
                if(post.media_type=="video")VideoPreview(post.media_url,"Смотреть видео",Modifier.fillMaxWidth().height(200.dp))
                else AsyncImage(ApiClient.mediaUrl(post.media_url),"Фото публикации",Modifier.fillMaxWidth().heightIn(min=120.dp,max=320.dp))
            }
            HorizontalDivider()
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick=onLike,enabled=!busy,contentPadding=PaddingValues(horizontal=12.dp),modifier=Modifier.height(44.dp)){Icon(if(post.liked)Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Нравится",Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text(post.likes.toString())}
                OutlinedButton(onClick={comments=true},contentPadding=PaddingValues(horizontal=12.dp),modifier=Modifier.height(44.dp)){Icon(Icons.Default.ChatBubbleOutline,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text("Комментарии · ${post.comments}")}
            }
        }
    }
    if(media)CompanyStoryViewer(post.author_name,listOf(CompanyStory(post.media_url,post.media_type=="video")),0){media=false}
    if(comments)PostCommentsSheet(post.id,{comments=false},onCommentCount)
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Удалить пост?")},text={Text("Комментарии и лайки также будут удалены.")},confirmButton={TextButton(onClick={confirm=false;onDelete()}){Text("Удалить")}},dismissButton={TextButton(onClick={confirm=false}){Text("Отмена")}})
}

private suspend fun<T>withSocialAttachment(context:Context,uri:Uri?,block:suspend(MultipartBody.Part?)->T):T{
    if(uri==null)return block(null)
    return withContext(Dispatchers.IO){val mime=context.contentResolver.getType(uri).orEmpty();val video=mime in listOf("video/mp4","video/webm");require(video||mime in listOf("image/jpeg","image/png","image/webp")){"Выбери JPG, PNG, WebP, MP4 или WebM."};val limit=(if(video)50L else 4L)*1024*1024
        val file=File.createTempFile("social_",".upload",context.cacheDir)
        try{context.contentResolver.openInputStream(uri)?.use{input->file.outputStream().use{output->val buffer=ByteArray(8192);var total=0L;while(true){ensureActive();val n=input.read(buffer);if(n<0)break;total+=n;require(total<=limit){if(video)"Видео — до 50 МБ."else "Фото — до 4 МБ."};output.write(buffer,0,n)}}}?:error("Не удалось открыть файл")
            block(MultipartBody.Part.createFormData(if(video)"video"else "photo","media.${mime.substringAfter('/')}",file.asRequestBody(mime.toMediaType())))
        }finally{file.delete()}
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialEditor(story:Boolean,companyAllowed:Boolean,onClose:()->Unit,onPublished:()->Unit){
    var body by rememberSaveable{mutableStateOf("")};var company by rememberSaveable{mutableStateOf(story)};var uri by remember{mutableStateOf<Uri?>(null)}
    var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope();val context=LocalContext.current
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){if(it!=null)uri=it}
    ModalBottomSheet(onDismissRequest={if(!busy)onClose()}){
        LazyColumn(Modifier.fillMaxWidth().imePadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            item{Text(if(story)"История компании"else "Новая публикация",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
            item{Text(if(story)"Фото или видео будет доступно 24 часа."else "Расскажи о работе, опыте или новостях команды.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            if(companyAllowed&&!story)item{Row(verticalAlignment=Alignment.CenterVertically){Switch(company,{company=it},enabled=!busy);Spacer(Modifier.width(12.dp));Text("От имени компании")}}
            item{OutlinedTextField(body,{body=it.take(if(story)500 else 5000)},Modifier.fillMaxWidth(),label={Text(if(story)"Подпись"else "Текст публикации")},placeholder={Text(if(story)"Один день из жизни команды"else "Расскажи о работе, опыте или новостях")},minLines=3,maxLines=8,enabled=!busy,shape=RoundedCornerShape(16.dp),supportingText={Text("${body.length}/${if(story)500 else 5000}")})}
            item{OutlinedButton(onClick={picker.launch(arrayOf("image/jpeg","image/png","image/webp","video/mp4","video/webm"))},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(uri==null)"Добавить фото или видео"else "Заменить вложение")}}
            if(uri!=null)item{if(context.contentResolver.getType(uri!!)?.startsWith("image/")==true)AsyncImage(uri,"Вложение",Modifier.fillMaxWidth().height(160.dp));TextButton(onClick={uri=null},enabled=!busy){Text("Убрать вложение")}}
            error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
            item{Button(enabled=!busy&&(if(story)uri!=null else body.isNotBlank()||uri!=null),modifier=Modifier.fillMaxWidth(),onClick={busy=true;error=null;scope.launch{try{
                withSocialAttachment(context,uri){media->val text=body.trim().toRequestBody("text/plain; charset=utf-8".toMediaType());if(story)ApiClient.api.publishStory(text,requireNotNull(media)).bodyOrThrow()else ApiClient.api.publishPost(text,company.toString().toRequestBody("text/plain".toMediaType()),media).bodyOrThrow()}
                onPublished();onClose()
            }catch(e:CancellationException){throw e}catch(e:IllegalArgumentException){error=e.message}catch(e:Exception){error=userErrorMessage(e)}finally{busy=false}}}){Text(if(busy)"Публикуем…"else "Опубликовать")}}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCommentsSheet(postId:Long,onClose:()->Unit,onCount:(Int)->Unit){
    var items by remember{mutableStateOf<List<SocialComment>>(emptyList())};var next by remember{mutableLongStateOf(0)};var body by rememberSaveable{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)};var loading by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    suspend fun load(before:Long?=null){loading=true;try{val page=ApiClient.api.postComments(postId,before).bodyOrThrow();items=if(before==null)page.comments else items+page.comments;next=page.next_cursor;error=null}catch(e:CancellationException){throw e}catch(e:Exception){error=userErrorMessage(e)}finally{loading=false}}
    LaunchedEffect(postId){load()}
    ModalBottomSheet(onDismissRequest={if(!busy)onClose()}){
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=650.dp).imePadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            item{Text("Комментарии",style=MaterialTheme.typography.headlineSmall)}
            if(next>0)item{TextButton(onClick={scope.launch{load(next)}},enabled=!loading&&!busy){Text("Предыдущие комментарии")}}
            items(items.reversed(),key={it.id}){comment->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){CompanyAvatar(comment.author_photo,comment.author_name,36.dp);Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(comment.author_name,fontWeight=FontWeight.SemiBold);Text(socialDate(comment.created_at),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(comment.body);if(comment.can_delete)TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{ApiClient.api.deletePostComment(comment.id).checkSuccess();items=items.filter{it.id!=comment.id};onCount(-1)}catch(e:CancellationException){throw e}catch(e:Exception){error=userErrorMessage(e)}finally{busy=false}}}){Text("Удалить комментарий")}}}}
            if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            error?.let{item{Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={scope.launch{load()}},enabled=!loading&&!busy){Text("Повторить")}}}
            item{OutlinedTextField(body,{body=it.take(2000)},Modifier.fillMaxWidth(),label={Text("Ваш комментарий")},minLines=2,maxLines=5,enabled=!busy,shape=RoundedCornerShape(16.dp))}
            item{Button(modifier=Modifier.fillMaxWidth(),enabled=body.isNotBlank()&&!busy&&!loading,onClick={busy=true;error=null;scope.launch{try{ApiClient.api.addPostComment(postId,SocialBody(body.trim())).bodyOrThrow();body="";onCount(1);load()}catch(e:CancellationException){throw e}catch(e:Exception){error=userErrorMessage(e)}finally{busy=false}}}){Text(if(busy)"Отправляем…"else "Отправить")}}
        }
    }
}

@Composable
fun CompanyFollowButton(companyId:Int){
    var subscription by remember(companyId){mutableStateOf<CompanySubscription?>(null)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)};var retry by remember{mutableIntStateOf(0)};val scope=rememberCoroutineScope()
    LaunchedEffect(companyId,retry){try{subscription=ApiClient.api.companySubscription(companyId).bodyOrThrow();error=null}catch(e:CancellationException){throw e}catch(e:Exception){error=userErrorMessage(e)}}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Button(enabled=subscription!=null&&!busy,onClick={busy=true;scope.launch{try{subscription=(if(subscription!!.following)ApiClient.api.unfollowCompany(companyId)else ApiClient.api.followCompany(companyId)).bodyOrThrow();error=null}catch(e:CancellationException){throw e}catch(e:Exception){error=userErrorMessage(e)}finally{busy=false}}}){Text(if(busy)"Сохраняем…"else if(subscription?.following==true)"Вы подписаны"else "Подписаться")}
        subscription?.let{Text("${it.followers} подписчиков · новости на главной",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error);if(subscription==null)TextButton(onClick={retry++}){Text("Повторить")}}
    }
}
