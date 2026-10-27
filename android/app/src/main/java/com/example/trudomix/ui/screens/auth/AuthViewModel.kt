package com.example.trudomix.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.LoginRequest
import com.example.trudomix.data.models.RegisterRequest
import com.example.trudomix.data.models.PhoneLoginRequest
import com.example.trudomix.data.api.phoneOtpBody
import com.example.trudomix.data.api.phoneOtpError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {
    private val api = ApiClient.api
    private val tokenManager = ApiClient.getTokenManager()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Заполните все поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.login(LoginRequest(email = email, password = password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    com.example.trudomix.data.local.SessionIdentity.user.value=authResponse.user
                    tokenManager.saveToken(authResponse.token)
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error(com.example.trudomix.data.api.httpErrorMessage(response.code(), authentication = true))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(phoneOtpError(e))
            }
        }
    }

    fun register(email: String, password: String, role: String) = registerWithPhone(email,password,role,"","","")
    fun registerWithPhone(email: String, password: String, role: String,phone:String,receipt:String,deviceKey:String) {
        if(_uiState.value is AuthUiState.Loading)return
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Заполните все поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val request=RegisterRequest(email=email,password=password,role=role,phone=phone,phone_receipt=receipt,device_key=deviceKey)
                val response = if(phone.isNotBlank())api.registerPhone(request)else api.register(request)
                if(!response.isSuccessful&&phone.isNotBlank())response.phoneOtpBody()
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    com.example.trudomix.data.local.SessionIdentity.user.value=authResponse.user
                    tokenManager.saveToken(authResponse.token)
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error(com.example.trudomix.data.api.httpErrorMessage(response.code(), registration = true))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(phoneOtpError(e))
            }
        }
    }

    fun loginPhone(phone:String,receipt:String,key:String){
        if(_uiState.value is AuthUiState.Loading)return
        viewModelScope.launch{
            _uiState.value=AuthUiState.Loading
            try{
                val result=api.phoneLogin(PhoneLoginRequest(phone,receipt,key)).phoneOtpBody()
                com.example.trudomix.data.local.SessionIdentity.user.value=result.user
                tokenManager.saveToken(result.token);_uiState.value=AuthUiState.Success
            }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){_uiState.value=AuthUiState.Error(phoneOtpError(e))}
        }
    }
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
