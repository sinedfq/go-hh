package com.example.trudomix.ui.screens.resume

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.local.ResumeEventBus
import com.example.trudomix.data.models.Resume
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ResumeUiState {
    data object Loading : ResumeUiState
    data class Success(val resume: Resume?) : ResumeUiState
    data class Error(val message: String) : ResumeUiState
}

class ResumeViewModel : ViewModel() {
    private val api = ApiClient.api

    private val _uiState = MutableStateFlow<ResumeUiState>(ResumeUiState.Loading)
    val uiState: StateFlow<ResumeUiState> = _uiState.asStateFlow()

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadResume()
        viewModelScope.launch {
            ResumeEventBus.resumeUpdatedEvent.collect {
                loadResume()
            }
        }
    }

    fun loadResume() {
        viewModelScope.launch {
            _uiState.value = ResumeUiState.Loading
            try {
                val response = api.getMyResume()
                if (response.isSuccessful) {
                    _uiState.value = ResumeUiState.Success(response.body())
                } else if (response.code() == 404) {
                    _uiState.value = ResumeUiState.Success(null)
                } else {
                    _uiState.value = ResumeUiState.Error(com.example.trudomix.data.api.httpErrorMessage(response.code()))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                _uiState.value = ResumeUiState.Error("Не удалось подключиться к серверу. Проверьте соединение и повторите попытку.")
            }
        }
    }

    fun saveResume(resume: Resume, isUpdate: Boolean, onSaved: () -> Unit) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val response = if (isUpdate) {
                    api.updateMyResume(resume)
                } else {
                    api.createResume(resume)
                }

                if (response.isSuccessful) {
                    // Keep custom entries available in the shared dropdown catalog.
                    // Catalog availability must not turn a saved resume into a failed save.
                    kotlinx.coroutines.withTimeoutOrNull(3000) {
                        try {
                            api.createPosition(com.example.trudomix.data.models.DirectoryEntry(resume.desired_position.trim()))
                            resume.skills.filter{it.isNotBlank()}.distinctBy{it.lowercase()}.forEach {
                                api.createSkill(com.example.trudomix.data.models.DirectoryEntry(it.trim()))
                            }
                        } catch(e:kotlinx.coroutines.CancellationException){throw e} catch(_:Exception){ }
                    }
                    _messageEvent.emit("Резюме успешно сохранено!")
                    _uiState.value = ResumeUiState.Success(response.body() ?: resume)
                    ResumeEventBus.notifyResumeUpdated()
                    onSaved()
                } else {
                    _messageEvent.emit(com.example.trudomix.data.api.httpErrorMessage(response.code()))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                _messageEvent.emit(com.example.trudomix.data.api.userErrorMessage(e))
            } finally {
                _isSaving.value = false
            }
        }
    }

    suspend fun uploadPhoto(photo: okhttp3.MultipartBody.Part) {
        val response = api.uploadResumePhoto(photo)
        if (!response.isSuccessful) throw com.example.trudomix.data.repository.ApiException(response.code())
        _uiState.value = ResumeUiState.Success(response.body() ?: error("Сервер не вернул резюме"))
    }
    fun addWorkExperience(request:com.example.trudomix.data.models.WorkExperienceRequest)=changeWork {api.addWorkExperience(request)}
    fun deleteWorkExperience(id:Int)=changeWork {api.deleteWorkExperience(id)}
    private fun changeWork(block:suspend()->retrofit2.Response<Resume>){
        if(_isSaving.value)return
        viewModelScope.launch{_isSaving.value=true;try{val response=block();if(!response.isSuccessful)throw com.example.trudomix.data.repository.ApiException(response.code());_uiState.value=ResumeUiState.Success(response.body());_messageEvent.emit("Опыт работы обновлён");ResumeEventBus.notifyResumeUpdated()}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){_messageEvent.emit(com.example.trudomix.data.api.userErrorMessage(e))}finally{_isSaving.value=false}}
    }
}
