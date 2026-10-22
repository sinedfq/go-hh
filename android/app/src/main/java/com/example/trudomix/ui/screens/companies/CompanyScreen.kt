package com.example.trudomix.ui.screens.companies

import com.example.trudomix.ui.components.AppScreenHeader
import com.example.trudomix.ui.components.CompanyAvatar
import com.example.trudomix.ui.components.ITAccreditationBadge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.bodyOrThrow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class CompanyState(val company:Company?=null,val vacancies:List<Vacancy> = emptyList(),val error:String?=null,val loading:Boolean=true)
class CompanyViewModel:ViewModel(){
    val state=MutableStateFlow(CompanyState())
    private var job:Job?=null
    fun load(id:Int){job?.cancel();job=viewModelScope.launch{
        state.value=CompanyState()
        try{val company=ApiClient.api.getCompany(id).bodyOrThrow();val vacancies=ApiClient.api.getCompanyVacancies(id).bodyOrThrow();state.value=CompanyState(company,vacancies,loading=false)}
        catch(e:CancellationException){throw e}catch(e:Exception){state.value=CompanyState(error=e.localizedMessage?:"Ошибка загрузки",loading=false)}
    }}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyScreen(id:Int,onBack:()->Unit,onVacancy:(Int)->Unit){
    val model:CompanyViewModel=viewModel(key="company-$id")
    val state by model.state.collectAsState()
    LaunchedEffect(id){model.load(id)}
    Scaffold(topBar={AppScreenHeader(title={Text("О компании")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            if(state.loading)item{CircularProgressIndicator()}
            state.error?.let{item{Text(it);Button(onClick={model.load(id)}){Text("Повторить")}}}
            state.company?.let{c->item{Column(verticalArrangement=Arrangement.spacedBy(12.dp)){CompanyAvatar(c.logo_url,c.name,80.dp);Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Text(c.name,Modifier.weight(1f,fill=false),style=MaterialTheme.typography.headlineMedium);if(c.it_accredited)ITAccreditationBadge()};if(c.city.isNotBlank())Text(c.city);if(c.industry.isNotBlank())Text(c.industry);if(c.description.isNotBlank())Text(c.description);if(c.website.isNotBlank())Text(c.website)}}}
            items(state.vacancies,key={it.id}){v->com.example.trudomix.ui.components.VacancyCard(v,{onVacancy(v.id)},showFavorite=false)}
        }
    }
}
