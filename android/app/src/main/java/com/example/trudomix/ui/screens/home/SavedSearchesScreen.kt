package com.example.trudomix.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.api.*
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.*
import com.example.trudomix.ui.components.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SavedSearchesViewModel:ViewModel(){
    val searches=MutableStateFlow<List<SavedSearch>>(emptyList())
    val loading=MutableStateFlow(false)
    val error=MutableStateFlow<String?>(null)
    val busy=MutableStateFlow(false)
    private var job:Job?=null
    init{if(com.example.trudomix.data.local.SessionIdentity.user.value?.role!="employer")load()}
    fun load(){job?.cancel();job=viewModelScope.launch {loading.value=true;error.value=null;try{searches.value=ApiClient.api.getSavedSearches().bodyOrThrow()}catch(e:CancellationException){throw e}catch(e:Exception){error.value=userErrorMessage(e)}finally{loading.value=false}}}
    suspend fun save(name:String,filters:SavedSearchFilters){
        if(busy.value)return
        busy.value=true
        try{val item=ApiClient.api.saveSearch(SavedSearchRequest(name.trim(),filters.toQuery())).bodyOrThrow();searches.value=listOf(item)+searches.value}
        finally{busy.value=false}
    }
    fun remove(id:Int){if(busy.value)return;viewModelScope.launch{busy.value=true;error.value=null;try{ApiClient.api.deleteSavedSearch(id).checkSuccess();searches.value=searches.value.filterNot{it.id==id}}catch(e:CancellationException){throw e}catch(e:Exception){error.value=userErrorMessage(e)}finally{busy.value=false}}}
}

@Composable
fun SavedSearchesScreen(model:SavedSearchesViewModel,onBack:()->Unit,onOpen:(SavedSearchFilters)->Unit){
    val items by model.searches.collectAsState();val loading by model.loading.collectAsState();val error by model.error.collectAsState();val busy by model.busy.collectAsState()
    var deleting by remember{mutableStateOf<SavedSearch?>(null)}
    RefreshContent(loading,model::load){
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=16.dp,bottom=148.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            item{AppScreenHeader(title={Text("Мои поиски")},subtitle="Сохранённые условия поиска синхронизируются с сайтом",applyInsets=false,contentPadding=PaddingValues(0.dp),navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}})}
            error?.let{item{Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=model::load){Text("Повторить")}}}
            if(!loading&&items.isEmpty())item{OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Default.BookmarkBorder,null);Text("Твой следующий поиск — в один тап",style=MaterialTheme.typography.titleMedium);Text("Выбери фильтры на экране поиска и нажми «Сохранить поиск». Новые подходящие вакансии появятся в уведомлениях на сайте.")}}}
            items(items,key={it.id}){search->OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Row {Text(search.name,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);IconButton(enabled=!busy,onClick={deleting=search}){Icon(Icons.Default.DeleteOutline,"Удалить поиск: ${search.name}")}}
                    Text(search.filters.summary(),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick={onOpen(search.filters)}){Text("Показать вакансии")}
                }
            }}
        }
    }
    deleting?.let{search->AlertDialog(onDismissRequest={deleting=null},title={Text("Удалить поиск?")},text={Text("«${search.name}» исчезнет и на сайте, и в приложении.")},confirmButton={TextButton(onClick={model.remove(search.id);deleting=null}){Text("Удалить")}},dismissButton={TextButton(onClick={deleting=null}){Text("Отмена")}})}
}
