package com.example.trudomix.ui.screens.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.BuildConfig
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.data.repository.VacancyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.trudomix.ui.components.VacancyMapController
import com.example.trudomix.ui.components.AppScreenHeader
import com.example.trudomix.ui.components.AppSnackbarHost
import com.example.trudomix.ui.components.currentDeviceLocation
import com.example.trudomix.ui.components.hasLocationPermission
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class MapViewModel:ViewModel(){
    val vacancies=MutableStateFlow<List<Vacancy>>(emptyList())
    val loading=MutableStateFlow(true)
    val error=MutableStateFlow<String?>(null)
    private var job:Job?=null
    init{load()}
    fun load(){job?.cancel();job=viewModelScope.launch{
        loading.value=true;error.value=null
        try{
            val repo=VacancyRepository();val items=mutableListOf<Vacancy>();var offset=0
            do{val page=repo.search("",null,null,null,null,offset);items+=page.vacancies;offset+=page.vacancies.size
                if(page.vacancies.isEmpty()||offset>=page.total_count)break
            }while(isActive)
            vacancies.value=items.distinctBy{it.id}.filter{it.latitude!=null&&it.longitude!=null&&it.latitude in -90.0..90.0&&it.longitude in -180.0..180.0&&!(it.latitude==0.0&&it.longitude==0.0)}
        }catch(e:CancellationException){throw e}catch(e:Exception){error.value=com.example.trudomix.data.api.userErrorMessage(e)}
        finally{loading.value=false}
    }}
}

@Composable
fun VacancyMapScreen(onBack:()->Unit,onVacancy:(Int)->Unit) {
    val model:MapViewModel=viewModel()
    val items by model.vacancies.collectAsState()
    val loading by model.loading.collectAsState()
    val error by model.error.collectAsState()
    VacancyMapContent(items,loading,error,onBack,onVacancy,model::load)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VacancyMapContent(items:List<Vacancy>,loading:Boolean=false,error:String?=null,onBack:()->Unit={},onVacancy:(Int)->Unit={},onRetry:()->Unit={}) {
    val context=LocalContext.current
    val controller=remember{VacancyMapController()}
    val scope=rememberCoroutineScope()
    var locating by remember{mutableStateOf(false)}
    val snackbar=remember{SnackbarHostState()}
    fun notifyLocation(message:String) {
        scope.launch { snackbar.currentSnackbarData?.dismiss();snackbar.showSnackbar(message,duration=SnackbarDuration.Short) }
    }
    var locationJob by remember{mutableStateOf<Job?>(null)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP){locationJob?.cancel();locating=false;snackbar.currentSnackbarData?.dismiss()}}
        lifecycle.addObserver(observer)
        onDispose{lifecycle.removeObserver(observer);locationJob?.cancel()}
    }
    fun locate() {
        if(locating)return
        locationJob=scope.launch {
            locating=true;snackbar.currentSnackbarData?.dismiss()
            try { val position=currentDeviceLocation(context);controller.locate(position.latitude,position.longitude,position.accuracy);notifyLocation("Ты на карте · точность около ${position.accuracy.toInt()} м") }
            catch(e:CancellationException){throw e}
            catch(e:Exception){notifyLocation(if(e is IllegalStateException)e.message?:"Не удалось получить геопозицию" else "Не удалось получить геопозицию. Проверь разрешения и повтори попытку.")}
            finally{locating=false}
        }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result->
        if(result.values.any{it})locate() else notifyLocation("Доступ к геопозиции не разрешён. Его можно включить в настройках приложения.")
    }
    var query by rememberSaveable{mutableStateOf("")}
    var office by rememberSaveable{mutableStateOf(false)}
    var selected by remember{mutableStateOf<Vacancy?>(null)}
    var mapError by remember{mutableStateOf<String?>(null)}
    var attempt by remember{mutableIntStateOf(0)}
    val visible=remember(items,query,office){items.filter{(!office||!it.remote)&&(query.isBlank()||listOf(it.title,it.company,it.location).any{part->part.contains(query.trim(),ignoreCase=true)})}}
    LaunchedEffect(visible){selected=selected?.let{old->visible.find{it.id==old.id}}}
    Box(Modifier.fillMaxSize()) {
        key(attempt){com.example.trudomix.ui.components.YandexVacancyMap(visible,{selected=it},{mapError=it},Modifier.fillMaxSize(),controller)}
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().widthIn(max=700.dp).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Surface(shape=RoundedCornerShape(24.dp),shadowElevation=4.dp,color=MaterialTheme.colorScheme.background) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    AppScreenHeader(title={Text("Работа рядом")},subtitle=if(loading)"Загружаем вакансии…" else "На карте: ${visible.size}",applyInsets=false,contentPadding=PaddingValues(0.dp),
                        navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}},
                        actions={IconButton(onClick=onRetry){Icon(Icons.Default.Refresh,"Обновить карту")}})
                    OutlinedTextField(query,{query=it},placeholder={Text("Должность, компания, город")},singleLine=true,shape=RoundedCornerShape(16.dp),leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(query.isNotEmpty())IconButton(onClick={query=""}){Icon(Icons.Default.Close,"Очистить")}},modifier=Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        FilterChip(!office,{office=false},label={Text("Все вакансии")})
                        FilterChip(office,{office=true},label={Text("В офисе")})
                    }
                    if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            (error?:mapError)?.let{message->Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.errorContainer){Column(Modifier.padding(12.dp)){Text(message,color=MaterialTheme.colorScheme.onErrorContainer,style=MaterialTheme.typography.bodySmall);TextButton(onClick={mapError=null;attempt++;onRetry()}){Text("Повторить")}}}}
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(onClick={controller.zoom(1)},modifier=Modifier.size(48.dp)){Icon(Icons.Default.Add,"Приблизить карту")}
            FilledTonalIconButton(onClick={controller.zoom(-1)},modifier=Modifier.size(48.dp)){Icon(Icons.Default.Remove,"Отдалить карту")}
            FilledTonalIconButton(onClick=controller::fit,modifier=Modifier.size(48.dp)){Icon(Icons.Default.CenterFocusStrong,"Показать все вакансии")}
        }
        FilledTonalIconButton(onClick={if(hasLocationPermission(context))locate()else permission.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION))},enabled=!locating,
            modifier=Modifier.align(Alignment.CenterStart).padding(start=16.dp).size(52.dp)) {
            if(locating)CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)else Icon(Icons.Default.MyLocation,"Моё местоположение")
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().widthIn(max=700.dp).padding(start=16.dp,end=16.dp,bottom=40.dp)) {
            AppSnackbarHost(snackbar,Modifier.fillMaxWidth())
            val vacancy=selected
            if(vacancy!=null)Surface(shape=RoundedCornerShape(24.dp),shadowElevation=6.dp,color=MaterialTheme.colorScheme.surface){
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row{Column(Modifier.weight(1f)){Text(vacancy.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);Text(vacancy.company,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={selected=null}){Icon(Icons.Default.Close,"Закрыть карточку")}}
                    Text(vacancy.address.ifBlank{vacancy.location},style=MaterialTheme.typography.bodySmall)
                    com.example.trudomix.ui.components.VacancyTerms(vacancy)
                    if(vacancy.company_it_accredited)com.example.trudomix.ui.components.ITAccreditationBadge()
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Button(onClick={onVacancy(vacancy.id)}){Text("Посмотреть вакансию")}
                        TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://yandex.ru/maps/?rtext=~${vacancy.latitude},${vacancy.longitude}&rtt=auto")))}}){Text("Маршрут")}
                    }
                }
            } else Surface(shape=RoundedCornerShape(20.dp),shadowElevation=2.dp,color=MaterialTheme.colorScheme.surface.copy(alpha=.96f)) {
                Text(if(visible.isEmpty()&&!loading)"Нет вакансий с координатами по этому запросу" else "Выбери маркер, чтобы узнать о вакансии",Modifier.padding(16.dp),style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
