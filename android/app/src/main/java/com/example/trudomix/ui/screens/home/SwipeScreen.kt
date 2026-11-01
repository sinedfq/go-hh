package com.example.trudomix.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.ui.components.*
import com.example.trudomix.ui.loading.*
import kotlinx.coroutines.*
import kotlin.math.abs

@Composable fun SwipeScreen(onVacancy:(Int)->Unit,model:SwipeViewModel=viewModel()){
    val scope=rememberCoroutineScope()
    val density=LocalDensity.current
    val x=remember{Animatable(0f)}
    val drag=remember{mutableFloatStateOf(0f)}
    var moving by remember{mutableStateOf(false)}
    var transition by remember{mutableStateOf<Job?>(null)}
    val vacancy=model.items.firstOrNull()
    BoxWithConstraints(Modifier.fillMaxSize()){
        val compact=maxHeight<620.dp||density.fontScale>1.5f
        val exit=with(density){(maxWidth+80.dp).toPx()}
        val threshold=with(density){minOf(maxWidth*.25f,100.dp).toPx()}
        fun finish(save:Boolean?){
            val item=vacancy?:return
            if(transition?.isActive==true||model.busy)return
            transition=scope.launch{
                x.snapTo(drag.floatValue);moving=true
                try{
                    if(save==null)x.animateTo(0f,tween(180))
                    else{
                        x.animateTo(if(save)exit else -exit,tween(200,easing=FastOutSlowInEasing))
                        if(!model.decide(item.id,save))x.animateTo(0f,tween(200))
                    }
                }finally{drag.floatValue=0f;x.snapTo(0f);moving=false}
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().then(if(compact)Modifier.verticalScroll(rememberScrollState())else Modifier).padding(horizontal=20.dp).padding(bottom=132.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            AppScreenHeader(title={Text("Свайпы")},subtitle="Выбирай вакансии в своём ритме",applyInsets=false,contentPadding=PaddingValues(0.dp))
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(if(model.loading&&model.total==0)"Подбираем вакансии…"else "Просмотрено ${model.reviewed} из ${model.total}",style=MaterialTheme.typography.labelLarge)
                        Text("Сохранено: ${model.saved}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick=model::undo,enabled=model.canUndo&&!moving){Icon(Icons.AutoMirrored.Filled.Undo,"Вернуть последнюю вакансию")}
                }
                LinearProgressIndicator(progress={if(model.total>0)(model.reviewed.toFloat()/model.total).coerceIn(0f,1f)else 0f},modifier=Modifier.fillMaxWidth())
            }
            Box((if(compact)Modifier.height(360.dp)else Modifier.weight(1f)).fillMaxWidth(),contentAlignment=Alignment.Center){
                if(vacancy!=null){
                    if(model.items.size>1)Surface(Modifier.matchParentSize().padding(top=12.dp,start=12.dp,end=12.dp),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh){}
                    Box(Modifier.fillMaxSize().padding(bottom=8.dp).testTag("swipe-card").graphicsLayer{translationX=if(moving)x.value else drag.floatValue;rotationZ=(translationX/exit)*10f}.pointerInput(vacancy.id,moving,model.busy){
                        detectHorizontalDragGestures(onDragStart={if(!moving&&!model.busy)drag.floatValue=0f},onHorizontalDrag={change,amount->if(!moving&&!model.busy){change.consume();drag.floatValue+=amount}},onDragEnd={if(!moving&&!model.busy)finish(if(abs(drag.floatValue)>=threshold)drag.floatValue>0 else null)},onDragCancel={if(!moving&&!model.busy)finish(null)})
                    }){
                        SwipeVacancyCard(vacancy){if(!moving)onVacancy(vacancy.id)}
                        SwipeHint(true,{if(moving)x.value else drag.floatValue},threshold)
                        SwipeHint(false,{if(moving)x.value else drag.floatValue},threshold)
                    }
                    if(model.busy&&moving)AppLoader(Modifier.size(32.dp))
                }else if(model.loading)PageLoadingState(label="Готовим подборку…")
                else Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Icon(if(model.error!=null)Icons.Default.CloudOff else Icons.Default.CheckCircleOutline,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary)
                    Text(if(model.error!=null)if(model.loadFailed)"Подборка недоступна"else "Не удалось вернуть вакансию"else if(model.total==0)"Пока нет вакансий"else "Подборка просмотрена",style=MaterialTheme.typography.titleLarge)
                    Text(model.error?:"Сохранённые вакансии доступны в избранном поиска.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick={if(model.error!=null&&!model.loadFailed&&model.canUndo)model.undo()else model.load(restart=model.error==null)},enabled=!model.busy){Text(if(model.error!=null)"Повторить"else "Начать заново")}
                }
            }
            if(vacancy!=null){
                model.error?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall);if(model.loadFailed&&!model.loading)TextButton(onClick={model.load()}){Text("Повторить загрузку")}}
                Text("Влево — пропустить · вправо — в избранное",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    OutlinedButton(onClick={finish(false)},enabled=!moving&&!model.busy,modifier=Modifier.weight(1f).heightIn(min=52.dp),contentPadding=PaddingValues(horizontal=12.dp)){Icon(Icons.Default.Close,null,Modifier.size(20.dp));Spacer(Modifier.width(6.dp));Text("Пропустить",maxLines=1,overflow=TextOverflow.Ellipsis)}
                    Button(onClick={finish(true)},enabled=!moving&&!model.busy,modifier=Modifier.weight(1f).heightIn(min=52.dp),contentPadding=PaddingValues(horizontal=12.dp)){Icon(Icons.Default.FavoriteBorder,null,Modifier.size(20.dp));Spacer(Modifier.width(6.dp));Text("Сохранить",maxLines=1,overflow=TextOverflow.Ellipsis)}
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun SwipeVacancyCard(vacancy:Vacancy,onOpen:()->Unit){
    val scroll=key(vacancy.id){rememberScrollState()}
    Card(Modifier.fillMaxSize(),shape=RoundedCornerShape(28.dp),border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                CompanyAvatar(vacancy.company_logo,vacancy.company,52.dp)
                Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Text(vacancy.company,Modifier.weight(1f,fill=false),style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis);if(vacancy.company_it_accredited)ITAccreditationBadge()}
                if(vacancy.is_favorite)Icon(Icons.Default.Favorite,"Уже в избранном",tint=MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Text(vacancy.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
            VacancyTerms(vacancy)
            if(vacancy.skills.isNotEmpty())FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                vacancy.skills.take(8).forEach{skill->Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh){Text(skill,Modifier.padding(horizontal=10.dp,vertical=6.dp),style=MaterialTheme.typography.labelMedium)}}
            }
            val summary=remember(vacancy.description){vacancy.description.take(1600).replace(SwipeLinks,"$1").replace(SwipeMarkup,"").trim().take(360)}
            if(summary.isNotBlank()){Text("О вакансии",style=MaterialTheme.typography.titleSmall);Text(summary,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=6,overflow=TextOverflow.Ellipsis)}
            TextButton(onClick=onOpen,modifier=Modifier.fillMaxWidth()){Text("Открыть вакансию");Spacer(Modifier.width(6.dp));Icon(Icons.Default.OpenInNew,null,Modifier.size(18.dp))}
        }
    }
}
private val SwipeLinks=Regex("!?\\[([^]]*)]\\([^)]*\\)")
private val SwipeMarkup=Regex("[#*_`>]")

@Composable private fun BoxScope.SwipeHint(save:Boolean,offset:()->Float,threshold:Float){
    Surface(Modifier.align(if(save)Alignment.TopStart else Alignment.TopEnd).padding(16.dp).clearAndSetSemantics{}.graphicsLayer{alpha=((if(save)offset()else -offset())/threshold).coerceIn(0f,1f);rotationZ=if(save)-8f else 8f},shape=RoundedCornerShape(12.dp),color=if(save)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer){
        Text(if(save)"В ИЗБРАННОЕ"else "ПРОПУСТИТЬ",Modifier.padding(horizontal=14.dp,vertical=10.dp),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
    }
}
