package com.example.trudomix.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.data.api.*
import com.example.trudomix.data.local.*
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.*
import com.example.trudomix.ui.components.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.example.trudomix.ui.screens.social.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class DashboardState(val loading:Boolean=false,val user:User?=null,val resume:Resume?=null,val jobs:List<Vacancy> = emptyList(),val applications:Int?=null,val views:Int?=null,val error:Boolean=false)
class DashboardViewModel:ViewModel(){
    val state=MutableStateFlow(DashboardState())
    private var job:Job?=null
    private var loadedAt=0L
    init{load()}
    fun refreshIfStale(){if(job?.isActive!=true&&System.currentTimeMillis()-loadedAt>60000)load()}
    fun load(){job?.cancel();job=viewModelScope.launch{
        state.value=state.value.copy(loading=true)
        try{val user=SessionIdentity.user.value?:ApiClient.api.getMe().bodyOrThrow();val candidate=user.role=="candidate"
            coroutineScope {
                val employer=if(!candidate)async{optional{ApiClient.api.getEmployerStats().bodyOrThrow()}}else null
                val jobs=async{optional{ApiClient.api.searchVacancies(limit=4).bodyOrThrow().vacancies}}
                val resume=async{if(candidate)optional{val response=ApiClient.api.getMyResume();if(response.code()==404)true to null else true to response.bodyOrThrow()}else true to null}
                val apps=async{if(candidate)optional{ApiClient.api.getMyApplications().bodyOrThrow().size}else employer?.await()?.total_applications}
                val views=async{if(candidate)optional{ApiClient.api.getResumeStats().bodyOrThrow().total_views}else employer?.await()?.total_vacancy_views}
                val r=resume.await();val j=jobs.await();val a=apps.await();val v=views.await()
                state.value=DashboardState(false,user,r?.second,j.orEmpty(),a,v,r==null||j==null||a==null||v==null);loadedAt=System.currentTimeMillis()
            }
        }catch(e:CancellationException){throw e}catch(_:Exception){state.value=state.value.copy(loading=false,error=true)}
    }}
    private suspend fun<T>optional(block:suspend()->T):T?=try{block()}catch(e:CancellationException){throw e}catch(_:Exception){null}
}

@Composable
fun DashboardScreen(onSearch:()->Unit,onSaved:()->Unit,onApplications:()->Unit,onResume:()->Unit,onEmployer:()->Unit,onVacancy:(Int)->Unit){
    val model:DashboardViewModel=viewModel();val state by model.state.collectAsStateWithLifecycle()
    val social:SocialViewModel=viewModel();val feed by social.state.collectAsStateWithLifecycle()
    val identity by SessionIdentity.user.collectAsStateWithLifecycle()
    val employer=(state.user?:identity)?.role in listOf("employer","admin")
    LaunchedEffect(Unit){model.refreshIfStale()}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle){lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){social.refreshIfStale();while(true){delay(60000);social.refreshStories()}}}
    var initialReady by remember{mutableStateOf(false)}
    LaunchedEffect(state.loading,feed.loading,state.user,state.error){
        if(!state.loading&&!feed.loading&&(state.user!=null||state.error))initialReady=true
    }
    if(!initialReady){
        com.example.trudomix.ui.loading.TrudomixLoadingScreen()
        return
    }
    RefreshContent(state.loading||feed.loading,{model.load();social.load()}){
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=16.dp,bottom=148.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
            item(key="company-stories",contentType="stories"){TimedStoriesBar(feed.stories,onRefresh={social.refreshStories()})}
            item{AppScreenHeader(title={Text(if(employer)"Ваша команда"else "Твоя карьера")},subtitle=if(employer)"Вакансии, кандидаты и работа с командой"else "Возможности и следующие шаги в одном месте",applyInsets=false,contentPadding=PaddingValues(0.dp))}
            item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                    Text("Привет, ${state.resume?.full_name?.substringBefore(' ')?.takeIf{it.isNotBlank()}?:state.user?.email?.substringBefore('@')?:"друг"}!",style=MaterialTheme.typography.headlineSmall)
                    Text(if(employer)"Находите людей, с которыми хочется создавать больше."else "Найди команду, с которой захочется расти.",style=MaterialTheme.typography.bodyLarge)
                    Button(onClick=onSearch){Text(if(employer)"Мои вакансии"else "Найти работу")}
                }
            }}
            if(state.error)item{Text("Часть данных не загрузилась. Потяни экран вниз, чтобы обновить.",color=MaterialTheme.colorScheme.error)}
            item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                OutlinedCard(onClick=onApplications,modifier=Modifier.weight(1f),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(20.dp)){Text(state.applications?.toString()?:"—",style=MaterialTheme.typography.headlineMedium);Text("Отклики")}}
                OutlinedCard(onClick=if(state.user?.role=="candidate")onResume else onEmployer,modifier=Modifier.weight(1f),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(20.dp)){Text(state.views?.toString()?:"—",style=MaterialTheme.typography.headlineMedium);Text("Просмотры")}}
            }}
            if(state.user?.role=="candidate"){
                item{OutlinedCard(onClick=onSaved,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Мои поиски",style=MaterialTheme.typography.titleMedium);Text("Сохрани фильтры и возвращайся к подходящим вакансиям.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
                item{OutlinedCard(onClick=onResume,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(state.resume?.desired_position?.ifBlank{"Моё резюме"}?:"Создай своё резюме",style=MaterialTheme.typography.titleMedium);val count=state.resume?.let(::resumeCompletionCount)?:0;LinearProgressIndicator(progress={count/8f},modifier=Modifier.fillMaxWidth());Text("Заполнено ${resumeCompletionLabel(count)}",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
            }else item{Button(onClick=onEmployer){Text("Управлять компанией")}}
            item{SocialHomeHeader(feed,social)}
            items(feed.posts,key={"post-${it.id}"}){post->SocialPostCard(post,post.id in feed.busy,{social.like(post)},{social.delete(post)},{social.commentCount(post.id,it)})}
            if(feed.next>0)item{OutlinedButton(onClick={social.load(more=true)},enabled=!feed.loading,modifier=Modifier.fillMaxWidth()){Text("Показать ещё публикации")}}
            item{Text("Свежие возможности",style=MaterialTheme.typography.titleLarge)}
            items(state.jobs,key={it.id}){VacancyCard(it,{onVacancy(it.id)},showFavorite=false)}
        }
    }
}
