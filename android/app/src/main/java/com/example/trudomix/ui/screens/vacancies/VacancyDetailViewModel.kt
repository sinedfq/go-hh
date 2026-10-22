package com.example.trudomix.ui.screens.vacancies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.local.LogCollector
import com.example.trudomix.data.local.ResumeEventBus
import com.example.trudomix.data.models.ApplyRequest
import com.example.trudomix.data.models.FavoriteRequest
import com.example.trudomix.data.repository.checkSuccess
import com.example.trudomix.data.models.Vacancy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VacancyDetailUiState {
    data object Loading : VacancyDetailUiState
    data class Success(val vacancy: Vacancy) : VacancyDetailUiState
    data class Error(val message: String) : VacancyDetailUiState
}

class VacancyDetailViewModel : ViewModel() {
    private val api = ApiClient.api

    private val _uiState = MutableStateFlow<VacancyDetailUiState>(VacancyDetailUiState.Loading)
    val uiState: StateFlow<VacancyDetailUiState> = _uiState.asStateFlow()

    private val _hasResume = MutableStateFlow<Boolean?>(null)
    val hasResume: StateFlow<Boolean?> = _hasResume.asStateFlow()

    private val _hasApplied = MutableStateFlow(false)
    val hasApplied: StateFlow<Boolean> = _hasApplied.asStateFlow()

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    init {
        viewModelScope.launch {
            ResumeEventBus.resumeUpdatedEvent.collect {
                checkUserResume()
            }
        }
    }

    fun loadVacancyDetail(vacancyId: Int) {
        viewModelScope.launch {
            _uiState.value = VacancyDetailUiState.Loading
            checkUserResume()
            checkApplicationStatus(vacancyId)
            LogCollector.log("VacancyDetailViewModel", "Loading detail for vacancy ID: $vacancyId")
            try {
                val response = api.getVacancyDetail(vacancyId)
                if (response.isSuccessful && response.body() != null) {
                    val item = response.body()!!
                    val favorites = api.getFavorites()
                    _uiState.value = VacancyDetailUiState.Success(item.copy(is_favorite = favorites.body()?.any { it.id == item.id } == true))
                    LogCollector.log("VacancyDetailViewModel", "Loaded vacancy: ${response.body()?.title}")
                } else {
                    _uiState.value = VacancyDetailUiState.Error(com.example.trudomix.data.api.httpErrorMessage(response.code()))
                    LogCollector.log("VacancyDetailViewModel", "Failed to load vacancy: ${response.code()}", isError = true)
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                _uiState.value = VacancyDetailUiState.Error(com.example.trudomix.data.api.userErrorMessage(e))
                LogCollector.log("VacancyDetailViewModel", "Error loading vacancy: ${e.message}", isError = true)
            }
        }
    }

    private suspend fun checkUserResume() {
        try {
            val response = api.getMyResume()
            _hasResume.value = response.isSuccessful && response.body() != null
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
            _hasResume.value = false
        }
    }

    private suspend fun checkApplicationStatus(vacancyId: Int) {
        try {
            val response = api.getVacancyApplicationStatus(vacancyId)
            if (response.isSuccessful && response.body() != null) {
                _hasApplied.value = response.body()!!.applied
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
            _hasApplied.value = false
        }
    }

    fun toggleFavorite(vacancyId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                if (isFavorite) {
                    api.addFavorite(FavoriteRequest(vacancyId)).checkSuccess()
                    _messageEvent.emit("Добавлено в избранное")
                } else {
                    api.removeFavorite(vacancyId).checkSuccess()
                    _messageEvent.emit("Удалено из избранного")
                }
                val current = _uiState.value as? VacancyDetailUiState.Success
                if (current != null && current.vacancy.id == vacancyId) {
                    _uiState.value = current.copy(vacancy = current.vacancy.copy(is_favorite = isFavorite))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                LogCollector.log("VacancyDetailViewModel", "Error toggling favorite: ${e.message}", isError = true)
                _messageEvent.emit("Не удалось изменить избранное")
            }
        }
    }

    fun applyToVacancy(vacancyId: Int, coverLetter: String) {
        viewModelScope.launch {
            _isApplying.value = true
            LogCollector.log("VacancyDetailViewModel", "Applying to vacancy $vacancyId")
            try {
                val response = api.applyToVacancy(vacancyId, ApplyRequest(cover_letter = coverLetter))
                if (response.isSuccessful) {
                    _hasApplied.value = true
                    _messageEvent.emit("Отклик успешно отправлен!")
                    LogCollector.log("VacancyDetailViewModel", "Successfully applied to vacancy $vacancyId")
                } else if (response.code() == 409) {
                    _hasApplied.value = true
                    _messageEvent.emit("Вы уже откликались на эту вакансию.")
                    LogCollector.log("VacancyDetailViewModel", "Duplicate application (409 Conflict)", isError = true)
                } else {
                    _messageEvent.emit("Не удалось отправить отклик (код ${response.code()})")
                    LogCollector.log("VacancyDetailViewModel", "Failed to apply: ${response.code()}", isError = true)
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                _messageEvent.emit(com.example.trudomix.data.api.userErrorMessage(e))
                LogCollector.log("VacancyDetailViewModel", "Error applying: ${e.message}", isError = true)
            } finally {
                _isApplying.value = false
            }
        }
    }
}
