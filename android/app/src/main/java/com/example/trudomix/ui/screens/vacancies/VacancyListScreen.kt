package com.example.trudomix.ui.screens.vacancies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.ui.components.*
import com.example.trudomix.ui.screens.home.SavedSearchesViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable
fun VacancyListScreen(viewModel: VacanciesViewModel = viewModel(), onVacancyClick: (Int)->Unit = {},onSavedSearches:()->Unit={}) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val favorites by viewModel.favoritesOnly.collectAsState()
    val total by viewModel.total.collectAsState()
    val loading by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val restoredVersion by viewModel.restoredSearchVersion.collectAsState()
    var filters by remember { mutableStateOf(false) }
    var saveDialog by remember{mutableStateOf(false)}
    var name by rememberSaveable{mutableStateOf("")}
    var saveError by remember{mutableStateOf<String?>(null)}
    val searches:SavedSearchesViewModel=viewModel(key="saved-searches")
    val saving by searches.busy.collectAsState()
    val scope=rememberCoroutineScope()
    VacancyFeed(state,query,favorites,total,loading,error,viewModel::onSearchQueryChanged,viewModel::toggleFavorites,{filters=true},
        {viewModel.loadVacancies(true)},viewModel::loadMore,onVacancyClick,viewModel::toggleFavorite,onSavedSearches,{name=query.ifBlank{viewModel.location.value.ifBlank{"Подходящие вакансии"}};saveError=null;saveDialog=true},restoredVersion)
    if(saveDialog)AlertDialog(onDismissRequest={if(!saving)saveDialog=false},title={Text("Сохранить поиск")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){OutlinedTextField(name,{name=it.take(100)},label={Text("Название")},singleLine=true);Text(viewModel.snapshotFilters().summary(),style=MaterialTheme.typography.bodySmall);saveError?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={TextButton(enabled=name.isNotBlank()&&!saving,onClick={scope.launch{try{searches.save(name,viewModel.snapshotFilters());saveDialog=false}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){saveError=com.example.trudomix.data.api.userErrorMessage(e)}}}){Text(if(saving)"Сохранение…" else "Сохранить")}},dismissButton={TextButton(enabled=!saving,onClick={saveDialog=false}){Text("Отмена")}})
    if(filters) {
        val directory:DirectoryViewModel=viewModel()
        val options by directory.state.collectAsState()
        var city by remember{mutableStateOf(viewModel.location.value)}
        var company by remember{mutableStateOf(viewModel.company.value)}
        var experience by remember{mutableStateOf(viewModel.experience.value.split('|').filter{it.isNotBlank()}.toSet())}
        var skills by remember{mutableStateOf(viewModel.skills.value.split(',').map{it.trim()}.filter{it.isNotBlank()}.toSet())}
        var skill by remember{mutableStateOf("")}
        var mode by remember{mutableStateOf(viewModel.workMode.value)}
        var coordinates by remember{mutableStateOf(viewModel.hasCoordinates.value)}
        var sort by remember{mutableStateOf(viewModel.sort.value)}
        var extra by remember{mutableStateOf(viewModel.extended.value)}
        var salaryError by remember{mutableStateOf(false)}
        ModalBottomSheet(onDismissRequest={filters=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            VacancyFilters(city,{city=it},company,{company=it},experience,{experience=it},skills,{skills=it},skill,{skill=it},mode,{mode=it},coordinates,{coordinates=it},sort,{sort=it},options,directory::load,
                onApply={if(extra.salary_min>0&&extra.salary_max>0&&extra.salary_min>extra.salary_max){salaryError=true}else{viewModel.location.value=city;viewModel.company.value=company;viewModel.experience.value=experience.joinToString("|");viewModel.skills.value=skills.joinToString(",");viewModel.workMode.value=mode;viewModel.hasCoordinates.value=coordinates;viewModel.sort.value=sort;viewModel.extended.value=extra;viewModel.applyFilters();filters=false}},
                onReset={city="";company="";experience=emptySet();skills=emptySet();skill="";mode=null;coordinates=false;sort="newest";extra=com.example.trudomix.data.models.SavedSearchFilters();salaryError=false},extraFilters={
                    Text("Зарплата и условия",style=MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedTextField(extra.salary_min.takeIf{it>0}?.toString().orEmpty(),{extra=extra.copy(salary_min=it.filter(Char::isDigit).take(9).toIntOrNull()?:0);salaryError=false},label={Text("От")},modifier=Modifier.weight(1f),singleLine=true)
                        OutlinedTextField(extra.salary_max.takeIf{it>0}?.toString().orEmpty(),{extra=extra.copy(salary_max=it.filter(Char::isDigit).take(9).toIntOrNull()?:0);salaryError=false},label={Text("До")},modifier=Modifier.weight(1f),singleLine=true)
                    }
                    if(salaryError)Text("Сумма «до» должна быть не меньше суммы «от».",color=MaterialTheme.colorScheme.error)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("" to "Любая валюта","RUB" to "₽","USD" to "$","EUR" to "€").forEach{(value,label)->FilterChip(extra.currency==value,{extra=extra.copy(currency=value)},label={Text(label)})}}
                    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(extra.salary_only,{extra=extra.copy(salary_only=it)});Text("Только с указанной зарплатой")}
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("" to "Любая занятость","full_time" to "Полная","part_time" to "Частичная","project" to "Проектная","internship" to "Стажировка").forEach{(value,label)->FilterChip(extra.employment==value,{extra=extra.copy(employment=value)},label={Text(label)})}}
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("" to "Любой график","full_day" to "Полный день","flexible" to "Гибкий","shift" to "Сменный").forEach{(value,label)->FilterChip(extra.schedule==value,{extra=extra.copy(schedule=value)},label={Text(label)})}}
                })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VacancyFilters(city:String,onCity:(String)->Unit,company:String,onCompany:(String)->Unit,experience:Set<String>,onExperience:(Set<String>)->Unit,
    skills:Set<String>,onSkills:(Set<String>)->Unit,skill:String,onSkill:(String)->Unit,mode:Boolean?,onMode:(Boolean?)->Unit,coordinates:Boolean,onCoordinates:(Boolean)->Unit,
    sort:String,onSort:(String)->Unit,options:DirectoryOptions,onRetry:()->Unit,onApply:()->Unit,onReset:()->Unit,extraFilters: @Composable ColumnScope.()->Unit={}) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).imePadding().padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Твоя следующая работа",style=MaterialTheme.typography.headlineSmall)
        Text("Настрой поиск под себя",color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(options.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(options.failed){Text("Часть подсказок недоступна. Поля можно заполнить вручную.",style=MaterialTheme.typography.bodySmall);TextButton(onClick=onRetry){Text("Загрузить подсказки")}}
        SuggestionField(city,onCity,"Город",options.cities,Modifier.fillMaxWidth())
        SuggestionField(company,onCompany,"Компания",options.companies,Modifier.fillMaxWidth())
        Text("Формат работы",style=MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(null to "Любой",true to "Удалённо",false to "В офисе").forEach{(value,label)->FilterChip(mode==value,{onMode(value)},label={Text(label)})}}
        Text("Грейд",style=MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Intern","Junior","Middle","Senior","Lead").forEach{value->FilterChip(value in experience,{onExperience(if(value in experience)experience-value else experience+value)},label={Text(value)})}}
        Text("Требуемый опыт в годах",style=MaterialTheme.typography.titleMedium)
        val years=(listOf("Без опыта","До 1 года","1-3 года","3-6 лет","6+ лет")+options.experiences.filter{value->listOf("Intern","Junior","Middle","Senior","Lead").none{it.equals(value,true)}}).distinct()
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){years.forEach{value->FilterChip(experience.any{it.equals(value,true)},{onExperience(if(value in experience)experience-value else experience+value)},label={Text(value)})}}
        Text("Можно выбрать несколько значений: подойдёт любое из них. Грейд не заменяет стаж в годах.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        SuggestionField(skill,onSkill,"Навык",options.skills.filter{it !in skills},Modifier.fillMaxWidth(),allowCustom=true,onSelect={onSkills(skills+it);onSkill("")})
        TextButton(onClick={if(skill.isNotBlank()){onSkills(skills+skill.trim());onSkill("")}},enabled=skill.isNotBlank()){Text("Добавить навык")}
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){skills.forEach{value->InputChip(true,{onSkills(skills-value)},label={Text("$value ×")})}}
        if(skills.isNotEmpty())Text("Подойдёт хотя бы один выбранный навык",style=MaterialTheme.typography.bodySmall)
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(coordinates,onCoordinates);Text("Только с адресом на карте",Modifier.weight(1f))}
        extraFilters()
        Text("Сортировка",style=MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("newest" to "Сначала новые","oldest" to "Сначала старые","popular" to "Популярные","title" to "По названию").forEach{(value,label)->FilterChip(sort==value,{onSort(value)},label={Text(label)})}}
        Button(onClick=onApply,modifier=Modifier.fillMaxWidth()){Text("Показать вакансии")}
        TextButton(onClick=onReset,modifier=Modifier.fillMaxWidth()){Text("Сбросить фильтры")}
    }
}
