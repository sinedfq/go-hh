package com.example.trudomix.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.repository.bodyOrThrow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

data class DirectoryOptions(val cities: List<String> = emptyList(), val skills: List<String> = emptyList(),
    val positions: List<String> = emptyList(), val companies: List<String> = emptyList(), val experiences: List<String> = emptyList(),
    val loading: Boolean = true, val failed: Boolean = false)

class DirectoryViewModel : ViewModel() {
    val state = MutableStateFlow(DirectoryOptions())
    private var job: Job? = null
    init { load() }
    fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            state.value = state.value.copy(loading = true, failed = false)
            var failed = false
            suspend fun <T> optional(block: suspend () -> List<T>): List<T> = try { block() }
                catch (e: CancellationException) { throw e } catch (_: Exception) { failed = true; emptyList() }
            val api = ApiClient.api
            val vacancies = optional { api.getVacancies().bodyOrThrow() }
            val skills = optional { api.getSkills().bodyOrThrow() }.map { it.name }
            val positions = optional { api.getPositions().bodyOrThrow() }.map { it.name }
            fun List<String>.clean() = map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.sorted()
            val old = state.value
            state.value = DirectoryOptions((old.cities + vacancies.map { it.location }).clean(),
                (old.skills + skills + vacancies.flatMap { it.skills }).clean(),
                (old.positions + positions + vacancies.map { it.title }).clean(),
                (old.companies + vacancies.map { it.company }).clean(),
                (old.experiences + vacancies.map { it.experience }).clean(), loading = false, failed = failed)
        }
    }
}
