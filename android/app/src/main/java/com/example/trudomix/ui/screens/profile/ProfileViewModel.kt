package com.example.trudomix.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.local.AppThemeMode
import com.example.trudomix.data.local.ResumeEventBus
import com.example.trudomix.data.local.ThemeManager
import com.example.trudomix.data.models.User
import com.example.trudomix.data.models.*
import com.example.trudomix.data.repository.bodyOrThrow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

data class ProfileOverview(val loading: Boolean = true, val resume: Resume? = null, val views: Int? = null,
    val applications: Int? = null, val accepted: Int? = null, val favorites: Int? = null,
    val employer: EmployerStats? = null, val error: Boolean = false, val resumeKnown: Boolean = false)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ApiClient.api
    private val tokenManager = ApiClient.getTokenManager()
    private val themeManager = ThemeManager(application)

    val themeModeFlow: StateFlow<AppThemeMode> = themeManager.themeModeFlow.let { flow ->
        val state = MutableStateFlow(AppThemeMode.SYSTEM)
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
        state.asStateFlow()
    }

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val _overview = MutableStateFlow(ProfileOverview())
    val overview = _overview.asStateFlow()
    private var profileJob: Job? = null
    private var statsJob: Job? = null

    init {
        loadUserProfile()
        viewModelScope.launch {
            ResumeEventBus.resumeUpdatedEvent.collect {
                loadUserProfile()
            }
        }
    }

    fun loadUserProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            if(_uiState.value !is ProfileUiState.Success) _uiState.value = ProfileUiState.Loading
            try {
                val response = api.getMe()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ProfileUiState.Success(response.body()!!)
                    com.example.trudomix.data.local.SessionIdentity.user.value=response.body()
                    loadOverview()
                } else {
                    _uiState.value = ProfileUiState.Error(com.example.trudomix.data.api.httpErrorMessage(response.code()))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(com.example.trudomix.data.api.userErrorMessage(e))
            }
        }
    }

    fun loadOverview() {
        val user = (_uiState.value as? ProfileUiState.Success)?.user ?: return
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            _overview.value = _overview.value.copy(loading = true)
            if (user.role != "candidate") {
                val stats = optional { api.getEmployerStats().bodyOrThrow() }
                _overview.value = ProfileOverview(loading=false,employer=stats,error=stats==null)
            } else coroutineScope {
                val resume = async { optional { val response=api.getMyResume(); if(response.code()==404) true to null else true to response.bodyOrThrow() } }
                val views = async { optional { api.getResumeStats().bodyOrThrow().total_views } }
                val applications = async { optional { api.getMyApplications().bodyOrThrow() } }
                val favorites = async { optional { api.getFavorites().bodyOrThrow().size } }
                val apps = applications.await(); val count=favorites.await(); val totalViews=views.await()
                val resumeData=resume.await()
                _overview.value = ProfileOverview(false,resumeData?.second,totalViews,apps?.size,apps?.count{it.status=="accepted"},count,error=apps==null||count==null||totalViews==null||resumeData==null,resumeKnown=resumeData!=null)
            }
        }
    }
    private suspend fun <T> optional(block: suspend () -> T): T? = try { block() }
        catch(e: CancellationException) { throw e } catch(_: Exception) { null }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themeManager.setThemeMode(mode)
        }
    }

    suspend fun uploadPhoto(photo: okhttp3.MultipartBody.Part) {
        val response = api.uploadUserPhoto(photo)
        if (!response.isSuccessful) throw com.example.trudomix.data.repository.ApiException(response.code())
        _uiState.value = ProfileUiState.Success(response.body() ?: error("Сервер не вернул профиль"))
        com.example.trudomix.data.local.SessionIdentity.user.value=response.body()
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            com.example.trudomix.push.DeviceRegistration.unregister(getApplication())
            tokenManager.clearSession()
            onLoggedOut()
        }
    }
}
