package com.example.trudomix.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.api.userErrorMessage
import com.example.trudomix.data.local.ResumeEventBus
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.bodyOrThrow
import com.example.trudomix.data.repository.checkSuccess
import com.example.trudomix.ui.components.CompanyAvatar
import com.example.trudomix.ui.loading.InlineLoadingState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

data class RecommendationsState(val loading:Boolean=true,val items:List<Recommendation> = emptyList(),val hidden:Int=0,val error:String?=null,val busy:Boolean=false)
class RecommendationsViewModel:ViewModel(){
    val state=MutableStateFlow(RecommendationsState())
    private var job:Job?=null
    private var generation=0
    init {
        if(com.example.trudomix.data.local.SessionIdentity.user.value?.role=="candidate"){
            load()
            viewModelScope.launch{ResumeEventBus.resumeUpdatedEvent.collect{load(true)}}
        }else state.value=RecommendationsState(loading=false)
    }
    fun load(force:Boolean=false){
        if(state.value.busy)return
        job?.cancel();val request=++generation
        state.value=state.value.copy(loading=true,error=null)
        job=viewModelScope.launch{try{
            val response=ApiClient.api.recommendations(force)
            if(response.code()==404){state.value=state.value.copy(items=emptyList(),error="Добавь резюме в профиле для персонального подбора.");return@launch}
            val result=response.bodyOrThrow()
            if(request==generation)state.value=state.value.copy(items=result.recommendations,hidden=result.hidden_count)
        }catch(e:CancellationException){throw e}catch(e:Exception){if(request==generation)state.value=state.value.copy(error=userErrorMessage(e))}
        finally{if(request==generation)state.value=state.value.copy(loading=false)}}
    }
    fun dismiss(id:Int){
        if(state.value.busy)return
        job?.cancel();generation++
        state.value=state.value.copy(busy=true,loading=false,error=null)
        viewModelScope.launch{try{
            val session=ApiClient.getTokenManager().getToken()
            ApiClient.api.dismissRecommendation(id).checkSuccess()
            if(session!=ApiClient.getTokenManager().getToken())return@launch
            ApiClient.updateRecommendationCache(id,session)
            state.value=state.value.copy(items=state.value.items.filter{it.vacancy.id!=id},hidden=state.value.hidden+1)
        }catch(e:CancellationException){throw e}catch(e:Exception){state.value=state.value.copy(error=userErrorMessage(e))}
        finally{state.value=state.value.copy(busy=false)}}
    }
    fun restore(){
        if(state.value.busy)return
        job?.cancel();generation++
        state.value=state.value.copy(busy=true,loading=false,error=null)
        viewModelScope.launch{var restored=false;try{val session=ApiClient.getTokenManager().getToken();ApiClient.api.restoreRecommendations().checkSuccess();if(session!=ApiClient.getTokenManager().getToken())return@launch;ApiClient.updateRecommendationCache(expectedToken=session);state.value=state.value.copy(hidden=0);restored=true}
        catch(e:CancellationException){throw e}catch(e:Exception){state.value=state.value.copy(error=userErrorMessage(e))}
        finally{state.value=state.value.copy(busy=false)}
        if(restored)load(true)}
    }
}

@Composable
fun RecommendationsSection(state:RecommendationsState,model:RecommendationsViewModel){
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Text("Подходит по резюме",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);TextButton(onClick={model.load(true)},enabled=!state.loading&&!state.busy){Text("Обновить")}}
        if(state.loading)InlineLoadingState()
        state.error?.let{Text(it,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)}
        if(state.items.isEmpty()&&!state.loading&&state.error==null)Text(if(state.hidden>0)"Найденные рекомендации скрыты."else "Новых совпадений пока нет. Можно дополнить навыки в резюме.",color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RecommendationCard(item:Recommendation,busy:Boolean,onOpen:()->Unit,onDismiss:()->Unit){
    var expanded by rememberSaveable(item.vacancy.id){mutableStateOf(false)}
    OutlinedCard(shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                CompanyAvatar(item.vacancy.company_logo,item.vacancy.company,40.dp)
                Column(Modifier.weight(1f)){Text(item.vacancy.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(item.vacancy.company,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
            if(item.insights.matched_skills.isNotEmpty())Text("Совпадают: "+item.insights.matched_skills.joinToString(", "),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodySmall)
            if(expanded){
                if(item.insights.reasons.isNotEmpty()){Text("Что совпадает",fontWeight=FontWeight.SemiBold);item.insights.reasons.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}}
                if(item.insights.considerations.isNotEmpty()){Text("Что стоит уточнить",fontWeight=FontWeight.SemiBold);item.insights.considerations.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
                if(item.insights.reasons.isEmpty()&&item.insights.considerations.isEmpty())Text(item.reasoning,style=MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Скрыть объяснение"else "Почему эта вакансия")}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){OutlinedButton(onClick=onOpen){Text("Открыть")};TextButton(onClick=onDismiss,enabled=!busy){Text("Не интересно")}}
        }
    }
}
