package com.example.trudomix.ui.screens.vacancies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.data.models.SavedSearchFilters
import com.example.trudomix.data.repository.VacancyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface VacancyListUiState {
    data object Loading : VacancyListUiState
    data class Success(val vacancies: List<Vacancy>) : VacancyListUiState
    data class Error(val message: String) : VacancyListUiState
}
class VacanciesViewModel : ViewModel() {
    private val repository = VacancyRepository()
    private val _uiState = MutableStateFlow<VacancyListUiState>(VacancyListUiState.Loading)
    val uiState = _uiState.asStateFlow()
    val searchQuery = MutableStateFlow("")
    val location = MutableStateFlow("")
    val experience = MutableStateFlow("")
    val skills = MutableStateFlow("")
    val workMode = MutableStateFlow<Boolean?>(null)
    val company = MutableStateFlow("")
    val hasCoordinates = MutableStateFlow(false)
    val sort = MutableStateFlow("newest")
    val favoritesOnly = MutableStateFlow(false)
    val extended = MutableStateFlow(SavedSearchFilters())
    val restoredSearchVersion=MutableStateFlow(0)
    val isRefreshing = MutableStateFlow(false)
    val total = MutableStateFlow(0)
    val error = MutableStateFlow<String?>(null)
    private var loadJob: Job? = null
    private var generation = 0
    private var offset = 0
    private var favoriteIds = setOf<Int>()
    private var cachedFavorites:List<Vacancy>?=null
    private var favoritesLoadedAt=0L
    private val favoriteJobs = mutableSetOf<Int>()
    init { loadVacancies() }
    fun loadVacancies(isPullToRefresh: Boolean = false) { if(isPullToRefresh)favoritesLoadedAt=0;reload(0) }
    private fun reload(delayMs: Long) {
        loadJob?.cancel()
        val request = ++generation
        loadJob = viewModelScope.launch {
            delay(delayMs)
            if(_uiState.value !is VacancyListUiState.Success) _uiState.value = VacancyListUiState.Loading
            isRefreshing.value = true
            error.value = null
            try {
                val (favorites,page)=coroutineScope {
                    val saved=async {if(cachedFavorites!=null&&System.currentTimeMillis()-favoritesLoadedAt<60000)cachedFavorites!! else repository.favorites().also{cachedFavorites=it;favoritesLoadedAt=System.currentTimeMillis()}}
                    val result=if(!favoritesOnly.value)async{searchPage(0)}else null
                    saved.await() to result?.await()
                }
                favoriteIds = favorites.map { it.id }.toSet()
                val items = if (favoritesOnly.value) {
                    favorites.filter { matchesFilters(it) }.let { filtered -> when(sort.value) {
                        "oldest" -> filtered.sortedBy { it.created_at }
                        "popular" -> filtered.sortedByDescending { it.views }
                        "title" -> filtered.sortedBy { it.title.lowercase() }
                        else -> filtered.sortedByDescending { it.created_at }
                    } }
                } else {
                    total.value = page!!.total_count
                    offset = page.vacancies.size
                    page.vacancies
                }
                if(favoritesOnly.value) total.value = items.size
                if(request == generation) _uiState.value = VacancyListUiState.Success(items.map { it.copy(is_favorite = it.id in favoriteIds) })
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(request == generation) _uiState.value = VacancyListUiState.Error(com.example.trudomix.data.api.userErrorMessage(e)) }
            finally { if(request == generation) isRefreshing.value = false }
        }
    }
    fun loadMore() {
        val state = _uiState.value as? VacancyListUiState.Success ?: return
        if(favoritesOnly.value || isRefreshing.value || offset >= total.value) return
        val request = generation
        isRefreshing.value = true
        error.value = null
        loadJob = viewModelScope.launch {
            try {
                val page = searchPage(offset)
                if(request == generation) {
                    offset += page.vacancies.size
                    total.value = page.total_count
                    val current = (_uiState.value as? VacancyListUiState.Success)?.vacancies ?: state.vacancies
                    _uiState.value = VacancyListUiState.Success((current + page.vacancies.map { it.copy(is_favorite = it.id in favoriteIds) }).distinctBy { it.id })
                }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { error.value = com.example.trudomix.data.api.userErrorMessage(e) }
            finally { if(request == generation) isRefreshing.value = false }
        }
    }
    fun toggleFavorite(id: Int, selected: Boolean) {
        if(!favoriteJobs.add(id)) return
        viewModelScope.launch {
            try {
                repository.favorite(id, selected)
                favoriteIds = if(selected) favoriteIds + id else favoriteIds - id
                favoritesLoadedAt=0
                val current = _uiState.value as? VacancyListUiState.Success
                if(current != null) _uiState.value = VacancyListUiState.Success(current.vacancies.map { if(it.id==id) it.copy(is_favorite=selected) else it }.filter { !favoritesOnly.value || it.is_favorite })
                if (favoritesOnly.value) total.value = (_uiState.value as? VacancyListUiState.Success)?.vacancies?.size ?: 0
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { error.value = "Не удалось изменить избранное" }
            finally { favoriteJobs.remove(id) }
        }
    }
    fun onSearchQueryChanged(query: String) { searchQuery.value=query; reload(400) }
    fun toggleFavorites() { favoritesOnly.value=!favoritesOnly.value; reload(0) }
    fun applyFilters() { reload(0) }
    fun snapshotFilters():SavedSearchFilters=extended.value.copy(query=searchQuery.value.trim(),location=location.value.trim(),company=company.value.trim(),experience=experience.value,skills=skills.value.split(',').map{it.trim()}.filter{it.isNotBlank()},remote=workMode.value,has_coordinates=hasCoordinates.value,sort=sort.value)
    fun restoreSearch(filter:SavedSearchFilters){
        restoredSearchVersion.value++
        searchQuery.value=filter.query;location.value=filter.location;company.value=filter.company;experience.value=filter.experience
        skills.value=filter.skills.joinToString(",");workMode.value=filter.remote;hasCoordinates.value=filter.has_coordinates;sort.value=filter.sort.ifBlank{"newest"};extended.value=filter;favoritesOnly.value=false;reload(0)
    }
    private suspend fun searchPage(offset: Int) = repository.search(searchQuery.value.trim(), location.value.trim().ifBlank { null },
        experience.value.trim().ifBlank { null }, workMode.value, skills.value.trim().ifBlank { null }, offset,
        company.value.trim().ifBlank { null }, hasCoordinates.value, sort.value,extended.value.salary_min.takeIf{it>0},extended.value.salary_max.takeIf{it>0},extended.value.currency.ifBlank{null},extended.value.salary_only,extended.value.employment.ifBlank{null},extended.value.schedule.ifBlank{null})
    private fun matchesFilters(v: Vacancy): Boolean {
        val q=searchQuery.value.trim()
        val selectedSkills=skills.value.split(',').map{it.trim()}.filter{it.isNotBlank()}
        return (q.isEmpty()||listOf(v.title,v.company,v.description).any{it.contains(q,true)}) &&
            (location.value.isBlank()||v.location.equals(location.value.trim(),true)) &&
            (company.value.isBlank()||v.company.equals(company.value.trim(),true)) &&
            (experience.value.isBlank()||experience.value.split('|').any{v.experience.equals(it,true)}) &&
            (workMode.value==null||v.remote==workMode.value) &&
            (selectedSkills.isEmpty()||selectedSkills.any{s->v.skills.any{it.equals(s,true)}}) &&
            (!hasCoordinates.value||(v.latitude!=null&&v.longitude!=null&&v.latitude in -90.0..90.0&&v.longitude in -180.0..180.0&&!(v.latitude==0.0&&v.longitude==0.0)))
    }
}
