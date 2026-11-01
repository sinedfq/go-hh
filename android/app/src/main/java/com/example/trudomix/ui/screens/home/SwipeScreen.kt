package com.example.trudomix.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.data.repository.VacancyRepository
import com.example.trudomix.ui.components.AppScreenHeader
import com.example.trudomix.ui.components.VacancyCard
import com.example.trudomix.ui.loading.PageLoadingState
import kotlinx.coroutines.*

class SwipeViewModel:ViewModel(){
    var items by mutableStateOf<List<Vacancy>>(emptyList());private set
    var busy by mutableStateOf(false);private set
    var error by mutableStateOf<String?>(null);private set
    private val repository=VacancyRepository()
    private var offset=0
    private var total=Int.MAX_VALUE
    init{load()}
    fun load(restart:Boolean=false){
        if(busy)return
        if(restart){offset=0;total=Int.MAX_VALUE;items=emptyList()}
        if(offset>=total)return
        busy=true;error=null
        viewModelScope.launch{try{val page=repository.search("",null,null,null,null,offset);items=items+page.vacancies;offset+=page.vacancies.size;total=page.total_count}
        catch(e:CancellationException){throw e}catch(e:Exception){error=com.example.trudomix.data.api.userErrorMessage(e)}finally{busy=false}}
    }
    suspend fun decide(id:Int,save:Boolean):Boolean{
        if(busy||items.firstOrNull()?.id!=id)return false
        busy=true;error=null
        return try{if(save)repository.favorite(id,true);items=items.drop(1);true}
        catch(e:CancellationException){throw e}catch(e:Exception){error=com.example.trudomix.data.api.userErrorMessage(e);false}
        finally{busy=false;if(items.isEmpty())load()}
    }
}

@Composable fun SwipeScreen(onVacancy:(Int)->Unit,model:SwipeViewModel=viewModel()){
    val scope=rememberCoroutineScope()
    val density=LocalDensity.current
    val threshold=with(density){100.dp.toPx()}
    val exit=with(density){480.dp.toPx()}
    val x=remember{Animatable(0f)}
    var moving by remember{mutableStateOf(false)}
    var drag by remember{mutableFloatStateOf(0f)}
    val vacancy=model.items.firstOrNull()
    fun decide(save:Boolean){
        val item=vacancy?:return
        if(moving||model.busy)return
        moving=true
        scope.launch{try{x.snapTo(drag);x.animateTo(if(save)exit else -exit);model.decide(item.id,save);x.snapTo(0f);drag=0f}finally{moving=false}}
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=20.dp).padding(bottom=132.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        AppScreenHeader(title={Text("Свайпы")},subtitle="Вправо — в избранное, влево — пропустить",applyInsets=false,contentPadding=PaddingValues(0.dp))
        if(vacancy!=null){
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).graphicsLayer{translationX=if(moving)x.value else drag;rotationZ=(translationX/exit)*14}.pointerInput(vacancy.id,moving,model.busy){
                detectHorizontalDragGestures(onDragStart={drag=0f},onHorizontalDrag={change,amount->if(!moving&&!model.busy){change.consume();drag+=amount}},onDragEnd={if(kotlin.math.abs(drag)>threshold)decide(drag>0)else drag=0f},onDragCancel={drag=0f})
            }){VacancyCard(vacancy,{if(!moving)onVacancy(vacancy.id)},showFavorite=false)}
            model.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                OutlinedButton(onClick={decide(false)},enabled=!moving&&!model.busy){Icon(Icons.Default.Close,null);Spacer(Modifier.width(8.dp));Text("Пропустить")}
                Button(onClick={decide(true)},enabled=!moving&&!model.busy){Icon(Icons.Default.FavoriteBorder,null);Spacer(Modifier.width(8.dp));Text("Сохранить")}
            }
        }else if(model.busy)PageLoadingState()
        else Column(Modifier.fillMaxWidth().weight(1f),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
            Text(model.error?:"Все вакансии просмотрены")
            Button(onClick={model.load(restart=model.error==null)}){Text(if(model.error==null)"Начать заново"else "Повторить")}
        }
    }
}
