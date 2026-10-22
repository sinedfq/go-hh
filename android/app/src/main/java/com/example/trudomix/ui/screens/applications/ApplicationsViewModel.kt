package com.example.trudomix.ui.screens.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.api.userErrorMessage
import com.example.trudomix.data.models.ApplicationEvent
import com.example.trudomix.data.models.JobApplication
import com.example.trudomix.data.repository.bodyOrThrow
import com.example.trudomix.data.repository.checkSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ApplicationsUiState {
    data object Loading : ApplicationsUiState
    data class Success(val applications: List<JobApplication>) : ApplicationsUiState
    data class Error(val message: String) : ApplicationsUiState
}
sealed interface ApplicationHistoryState {
    data object Loading : ApplicationHistoryState
    data class Success(val events: List<ApplicationEvent>) : ApplicationHistoryState
    data class Error(val message: String) : ApplicationHistoryState
}

class ApplicationsViewModel : ViewModel() {
    private val api = ApiClient.api
    private val refreshMutex = Mutex()
    private val _uiState = MutableStateFlow<ApplicationsUiState>(ApplicationsUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()
    private val _busy = MutableStateFlow<Set<Int>>(emptySet())
    val busy = _busy.asStateFlow()
    private val _histories = MutableStateFlow<Map<Int,ApplicationHistoryState>>(emptyMap())
    val histories = _histories.asStateFlow()
    private var historyGeneration = 0
    private fun invalidateHistories() { historyGeneration++; _histories.value = emptyMap() }

    suspend fun observe() {
        while (currentCoroutineContext().isActive) {
            refresh(silent = _uiState.value is ApplicationsUiState.Success)
            delay(15000)
        }
    }
    fun loadApplications() {
        viewModelScope.launch {
            invalidateHistories()
            refresh(silent = false)
        }
    }
    private suspend fun refresh(silent: Boolean) = refreshMutex.withLock {
        val previous = _uiState.value as? ApplicationsUiState.Success
        if (!silent && previous == null) _uiState.value = ApplicationsUiState.Loading
        try {
            val applications = api.getMyApplications().bodyOrThrow()
            if (previous?.applications != applications) invalidateHistories()
            _uiState.value = ApplicationsUiState.Success(applications)
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) {
            if (previous == null) _uiState.value = ApplicationsUiState.Error(userErrorMessage(e))
            else if (!silent) _messageEvent.emit(userErrorMessage(e))
        }
    }
    fun loadHistory(applicationId: Int) {
        if (applicationId <= 0 || _histories.value[applicationId] is ApplicationHistoryState.Loading || _histories.value[applicationId] is ApplicationHistoryState.Success) return
        _histories.update { it + (applicationId to ApplicationHistoryState.Loading) }
        val generation = historyGeneration
        viewModelScope.launch {
            try {
                val events = api.applicationHistory(applicationId).bodyOrThrow()
                if(generation == historyGeneration) _histories.update { it + (applicationId to ApplicationHistoryState.Success(events)) }
            } catch(e: CancellationException) {
                if(generation == historyGeneration) _histories.update { it - applicationId }
                throw e
            } catch(e: Exception) { if(generation == historyGeneration) _histories.update { it + (applicationId to ApplicationHistoryState.Error(userErrorMessage(e))) } }
        }
    }
    fun cancelApplication(applicationId: Int) {
        if (applicationId in _busy.value) return
        _busy.value += applicationId
        viewModelScope.launch {
            try {
                api.cancelApplication(applicationId).checkSuccess()
                _messageEvent.emit("Отклик отменён")
                refresh(silent = true)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { _messageEvent.emit(userErrorMessage(e)) }
            finally { _busy.value -= applicationId }
        }
    }
    fun startChat(applicationId: Int, onChat: (Int) -> Unit) {
        if (applicationId in _busy.value) return
        _busy.value += applicationId
        viewModelScope.launch {
            try { onChat(api.startChat(applicationId).bodyOrThrow().conversation_id) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { _messageEvent.emit(userErrorMessage(e)) }
            finally { _busy.value -= applicationId }
        }
    }
}
