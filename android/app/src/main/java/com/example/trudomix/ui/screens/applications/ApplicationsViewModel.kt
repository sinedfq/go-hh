package com.example.trudomix.ui.screens.applications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.JobApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ApplicationsUiState {
    data object Loading : ApplicationsUiState
    data class Success(val applications: List<JobApplication>) : ApplicationsUiState
    data class Error(val message: String) : ApplicationsUiState
}

class ApplicationsViewModel : ViewModel() {
    private val api = ApiClient.api

    private val _uiState = MutableStateFlow<ApplicationsUiState>(ApplicationsUiState.Loading)
    val uiState: StateFlow<ApplicationsUiState> = _uiState.asStateFlow()

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()
    private val _busy = MutableStateFlow<Set<Int>>(emptySet())
    val busy = _busy.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.value = ApplicationsUiState.Loading
            try {
                val response = api.getMyApplications()

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ApplicationsUiState.Success(response.body()!!)
                } else {
                    _uiState.value = ApplicationsUiState.Error(com.example.trudomix.data.api.httpErrorMessage(response.code()))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {

                Log.e("API_ERROR", "Exception: ${e.message}")
                _uiState.value = ApplicationsUiState.Error(com.example.trudomix.data.api.userErrorMessage(e))
            }
        }
    }

    fun cancelApplication(applicationId: Int) {
        if(applicationId in _busy.value)return
        _busy.value += applicationId
        viewModelScope.launch {
            try {
                val response = api.cancelApplication(applicationId)
                if (response.isSuccessful) {
                    _messageEvent.emit("Отклик отменён")
                    loadApplications()
                } else {
                    _messageEvent.emit("Не удалось отменить отклик")
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                _messageEvent.emit(com.example.trudomix.data.api.userErrorMessage(e))
            } finally { _busy.value -= applicationId }
        }
    }

    fun startChat(applicationId: Int, onChat: (Int) -> Unit) {
        if(applicationId in _busy.value)return
        _busy.value += applicationId
        viewModelScope.launch {
            try {
                val response = api.startChat(applicationId)
                if (response.isSuccessful && response.body() != null) onChat(response.body()!!.conversation_id)
                else _messageEvent.emit("Не удалось открыть чат")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _messageEvent.emit(com.example.trudomix.data.api.userErrorMessage(e)) }
            finally { _busy.value -= applicationId }
        }
    }
}
