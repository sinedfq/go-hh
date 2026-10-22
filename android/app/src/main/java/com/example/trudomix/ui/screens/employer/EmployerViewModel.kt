package com.example.trudomix.ui.screens.employer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.EmployerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
data class EmployerState(val company:Company=Company(),val vacancies:List<Vacancy> = emptyList(),val applications:List<JobApplication> = emptyList(),val loading:Boolean=true,val busy:Boolean=false,val error:String?=null)
class EmployerViewModel:ViewModel(){
    private val repo=EmployerRepository()
    val state=MutableStateFlow(EmployerState())
    init{load()}
    private suspend fun refresh(){val mine=repo.vacancies();val apps=repo.applications();state.value=EmployerState(mine.company,mine.vacancies,apps,loading=false)}
    fun load(){if(state.value.busy)return;viewModelScope.launch{state.update{it.copy(loading=true,error=null)};try{refresh()}catch(e:CancellationException){throw e}catch(e:Exception){state.update{it.copy(loading=false,error=com.example.trudomix.data.api.userErrorMessage(e))}}}}
    private fun operate(block:suspend ()->Unit){if(state.value.busy)return;state.update{it.copy(busy=true,error=null)};viewModelScope.launch{try{block();refresh()}catch(e:CancellationException){throw e}catch(e:Exception){state.update{it.copy(error=com.example.trudomix.data.api.userErrorMessage(e))}}finally{state.update{it.copy(busy=false)}}}}
    fun company(request:CompanyRequest,onSaved:()->Unit)=operate{repo.createCompany(request);onSaved()}
    fun save(id:Int?,request:VacancyRequest,onSaved:()->Unit)=operate{repo.saveVacancy(id,request);onSaved()}
    fun archive(v:Vacancy)=operate{repo.archive(v.id,!v.is_archived)}
    fun delete(v:Vacancy)=operate{repo.delete(v.id)}
    fun status(id:Int,status:String)=operate{repo.status(id,status)}
    fun chat(id:Int,onChat:(Int)->Unit)=operate{val conversation=repo.startChat(id);onChat(conversation)}
    suspend fun geocode(address:String)=repo.geocode(address)
}
