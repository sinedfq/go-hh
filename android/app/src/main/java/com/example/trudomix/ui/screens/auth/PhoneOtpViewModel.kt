package com.example.trudomix.ui.screens.auth

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.api.*
import com.example.trudomix.data.local.PhoneOtpDevice
import com.example.trudomix.data.models.*
import com.example.trudomix.push.OtpNotifications
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class PhoneOtpState(val phone:String="+7",val code:String="",val config:PhoneOtpConfig?=null,val configuring:Boolean=true,val busy:Boolean=false,val challenge:String="",val receipt:String="",val retryIn:Int=0,val error:String?=null,val note:String?=null){val verified get()=receipt.isNotBlank()}

class PhoneOtpViewModel(application:Application):AndroidViewModel(application){
    private val api=ApiClient.api
    private val app=application.applicationContext
    private val state=MutableStateFlow(PhoneOtpState())
    val uiState=state.asStateFlow()
    private var operation:Job?=null
    private var configuration:Job?=null
    private var clock:Job?=null
    private var nextSend=0L
    private var expires=0L
    private var receiptExpires=0L
    private var pendingCode:String?=null
    init{loadConfig()}
    fun deviceKey()=PhoneOtpDevice.key(app)
    fun loadConfig(){configuration?.cancel();configuration=viewModelScope.launch{
        state.update{it.copy(configuring=true,error=null)}
        try{val config=api.phoneOtpConfig().phoneOtpBody();state.update{it.copy(config=config,configuring=false,error=if(config.enabled)null else "Доставка кодов пока недоступна. Повторите подключение позже")}}
        catch(e:CancellationException){throw e}catch(e:Exception){state.update{it.copy(configuring=false,error=phoneOtpError(e))}}
    }}
    fun editPhone(phone:String){if(phone==state.value.phone)return;operation?.cancel();clock?.cancel();OtpNotifications.clear(app);pendingCode=null;nextSend=0;expires=0;receiptExpires=0;state.update{it.copy(phone=phone,code="",challenge="",receipt="",retryIn=0,busy=false,error=null,note=null)}}
    fun editCode(code:String){state.update{it.copy(code=code.filter(Char::isDigit).take(6),error=null)}}
    fun requestCode(){
        if(state.value.busy||state.value.retryIn>0||state.value.config?.enabled!=true)return
        val phone=state.value.phone
        operation=viewModelScope.launch{
            state.update{it.copy(busy=true,error=null,receipt="")}
            try{
                val result=api.requestPhoneOtp(PhoneOtpRequest(phone,deviceKey())).phoneOtpBody()
                if(phone!=state.value.phone)return@launch
                pendingCode=result.development_code
                val now=SystemClock.elapsedRealtime();nextSend=now+result.retry_after*1000L;expires=now+result.expires_in*1000L;receiptExpires=0
                state.update{it.copy(challenge=result.challenge_id,code="",retryIn=result.retry_after,note=if(result.development_code!=null)"Код отправлен в уведомление Trudomix. Откройте шторку уведомлений"else "Код отправлен по SMS")}
                if(result.development_code!=null&&!OtpNotifications.show(app,phone,result.development_code,result.expires_in))state.update{it.copy(error="Уведомления Android запрещены. Разрешите их и нажмите «Показать уведомление»")}
                startClock()
            }catch(e:CancellationException){throw e}catch(e:Exception){if(e is PhoneOtpException&&e.retryAfter>0){nextSend=SystemClock.elapsedRealtime()+e.retryAfter*1000L;startClock()};state.update{it.copy(error=phoneOtpError(e))}}
            finally{state.update{it.copy(busy=false)}}
        }
    }
    fun showAgain(){val code=pendingCode?:return;val remaining=((expires-SystemClock.elapsedRealtime())/1000).toInt();if(remaining>0&&OtpNotifications.show(app,state.value.phone,code,remaining))state.update{it.copy(error=null,note="Код показан в уведомлении Trudomix")}}
    fun verify(){
        val current=state.value
        if(current.busy||current.challenge.isBlank()||current.code.length!=6)return
        operation=viewModelScope.launch{
            state.update{it.copy(busy=true,error=null)}
            try{
                val result=api.verifyPhoneOtp(PhoneOtpVerify(current.challenge,current.code,deviceKey())).phoneOtpBody()
                if(!result.verified)error("Номер не подтверждён")
                receiptExpires=SystemClock.elapsedRealtime()+result.expires_in*1000L;pendingCode=null;OtpNotifications.clear(app)
                state.update{it.copy(receipt=result.verification_receipt,code="",note="Код подтверждён. Можно продолжить")};startClock()
            }catch(e:CancellationException){throw e}catch(e:Exception){state.update{it.copy(error=phoneOtpError(e))}}
            finally{state.update{it.copy(busy=false)}}
        }
    }
    private fun startClock(){clock?.cancel();clock=viewModelScope.launch{
        do{
            val now=SystemClock.elapsedRealtime()
            state.update{it.copy(retryIn=((nextSend-now+999)/1000).toInt().coerceAtLeast(0))}
            if(receiptExpires>0&&now>=receiptExpires){receiptExpires=0;state.update{it.copy(receipt="",challenge="",code="",error="Подтверждение истекло. Запросите новый код")}}
            if(expires>0&&now>=expires){expires=0;pendingCode=null;OtpNotifications.clear(app);if(!state.value.verified)state.update{it.copy(challenge="",code="",error="Код истёк. Запросите новый код")}}
            delay(1000)
        }while(SystemClock.elapsedRealtime()<maxOf(nextSend,expires,receiptExpires))
    }}
    override fun onCleared(){OtpNotifications.clear(app);pendingCode=null;super.onCleared()}
}
