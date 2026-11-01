package com.example.trudomix.ui.screens.home

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.VacancyRepository
import kotlinx.coroutines.*

internal interface SwipeDataSource{
    suspend fun page(offset:Int):VacancySearchResponse
    suspend fun favorites():List<Vacancy>
    suspend fun favorite(id:Int,selected:Boolean)
}
private class ServerSwipeDataSource:SwipeDataSource{
    private val repository=VacancyRepository()
    override suspend fun page(offset:Int)=repository.search("",null,null,null,null,offset)
    override suspend fun favorites()=repository.favorites()
    override suspend fun favorite(id:Int,selected:Boolean)=repository.favorite(id,selected)
}
internal data class SwipeDecision(val vacancy:Vacancy,val saved:Boolean,val addedFavorite:Boolean)

class SwipeViewModel internal constructor(private val source:SwipeDataSource):ViewModel(){
    constructor():this(ServerSwipeDataSource())
    var items by mutableStateOf<List<Vacancy>>(emptyList());private set
    var busy by mutableStateOf(false);private set
    var loading by mutableStateOf(false);private set
    var error by mutableStateOf<String?>(null);private set
    var loadFailed by mutableStateOf(false);private set
    var reviewed by mutableIntStateOf(0);private set
    var saved by mutableIntStateOf(0);private set
    var total by mutableIntStateOf(0);private set
    private var offset=0
    private var favoriteIds=emptySet<Int>()
    private val seen=mutableSetOf<Int>()
    private var last by mutableStateOf<SwipeDecision?>(null)
    val canUndo:Boolean get()=last!=null&&!busy
    init{load()}
    fun load(restart:Boolean=false){
        if(loading||busy)return
        if(!restart&&offset>0&&offset>=total)return
        if(restart){offset=0;total=0;items=emptyList();seen.clear();reviewed=0;saved=0;last=null}
        loading=true;error=null;loadFailed=false
        viewModelScope.launch{
            try{
                val page=coroutineScope{
                    val favorites=if(offset==0)async{source.favorites()}else null
                    val page=async{source.page(offset)}
                    favorites?.await()?.let{favoriteIds=it.map{vacancy->vacancy.id}.toSet()}
                    page.await()
                }
                offset+=page.vacancies.size;total=page.total_count
                if(page.vacancies.isEmpty())offset=total
                items=(items+page.vacancies.filter{it.id !in seen}.map{it.copy(is_favorite=it.id in favoriteIds)}).distinctBy{it.id}
            }catch(e:CancellationException){throw e}catch(e:Exception){loadFailed=true;error=com.example.trudomix.data.api.userErrorMessage(e)}finally{loading=false}
        }
    }
    suspend fun decide(id:Int,save:Boolean):Boolean{
        val item=items.firstOrNull()?.takeIf{it.id==id}?:return false
        if(busy)return false
        busy=true;error=null;loadFailed=false
        var advanced=false
        return try{
            val added=save&&id !in favoriteIds
            if(added){source.favorite(id,true);favoriteIds=favoriteIds+id}
            last=SwipeDecision(item,save,added)
            seen+=id;reviewed++;if(save)saved++
            items=items.drop(1);advanced=true;true
        }catch(e:CancellationException){throw e}catch(e:Exception){error=com.example.trudomix.data.api.userErrorMessage(e);false}
        finally{busy=false;if(advanced&&items.size<=3)load()}
    }
    fun undo(){
        val decision=last?:return
        if(busy)return
        busy=true;error=null;loadFailed=false
        viewModelScope.launch{
            try{
                if(decision.addedFavorite){source.favorite(decision.vacancy.id,false);favoriteIds=favoriteIds-decision.vacancy.id}
                seen-=decision.vacancy.id
                items=listOf(decision.vacancy.copy(is_favorite=decision.vacancy.id in favoriteIds))+items.filter{it.id!=decision.vacancy.id}
                reviewed=(reviewed-1).coerceAtLeast(0);if(decision.saved)saved=(saved-1).coerceAtLeast(0)
                last=null
            }catch(e:CancellationException){throw e}catch(e:Exception){error=com.example.trudomix.data.api.userErrorMessage(e)}finally{busy=false}
        }
    }
}
