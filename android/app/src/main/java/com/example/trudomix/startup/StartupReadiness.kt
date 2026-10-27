package com.example.trudomix.startup

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.api.ServerCheckException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/** One startup attempt at a time; network errors are terminal states, never readiness. */
object StartupReadiness {
    val ready=MutableStateFlow(false)
    val visible=MutableStateFlow(true)
    val revealing=MutableStateFlow(false)
    val failure=MutableStateFlow<NetworkIssue?>(null)
    val connection=MutableStateFlow<NetworkIssue?>(null)
    val busy=MutableStateFlow(false)
    val attempt=MutableStateFlow(0)
    val animationDone=MutableStateFlow(false)
    private val pending=mutableSetOf<Any>("session")
    private val calls=mutableSetOf<Call>()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    val state=combine(ready,failure,visible,animationDone){ready,issue,visible,animated->
        when{!visible->StartupState.READY;!animated->StartupState.ANIMATING;issue!=null->issue.state;ready->StartupState.READY;else->StartupState.INITIALIZING}
    }.stateIn(scope,SharingStarted.Eagerly,StartupState.INITIALIZING)
    private var quiet:Job?=null
    private var watchdog:Job?=null
    private var retryJob:Job?=null
    private lateinit var connectivity:ConnectivityManager
    private fun offline()=::connectivity.isInitialized&&connectivity.activeNetwork==null
    fun initialize(context:Context){
        connectivity=context.getSystemService(ConnectivityManager::class.java)
        connectivity.registerDefaultNetworkCallback(object:ConnectivityManager.NetworkCallback(){
            override fun onLost(network:Network){if(!visible.value&&offline())connection.value=NetworkIssue.from(offline=true)}
        })
        watchdog()
        val generation=attempt.value
        scope.launch{begin("health");try{ApiClient.checkServer(ApiClient.baseUrl)}catch(error:Exception){if(generation==attempt.value)report(error)}finally{if(generation==attempt.value)finish("health")}}
    }
    private fun watchdog(){watchdog?.cancel();watchdog=scope.launch{delay(20000);if(visible.value&&!ready.value&&failure.value==null){report(SocketTimeoutException());synchronized(this@StartupReadiness){calls.toList()}.forEach{it.cancel()}}}}
    @Synchronized fun begin(key:Any){if(!visible.value)return;quiet?.cancel();pending.add(key);ready.value=false}
    @Synchronized fun finish(key:Any){
        if(!visible.value)return
        pending.remove(key);quiet?.cancel()
        if(pending.isEmpty())quiet=scope.launch{delay(180);synchronized(this@StartupReadiness){if(pending.isEmpty()){ready.value=failure.value==null;busy.value=false;watchdog?.cancel()}}}
    }
    fun report(error:Throwable?=null,status:Int?=null){
        val issue=NetworkIssue.from(error,offline(),status?:(error as? ServerCheckException)?.status)
        if(issue.kind=="AUTH")return // Existing session/navigation logic handles authorization.
        if(visible.value){failure.value=issue;ready.value=false;busy.value=false}else connection.value=issue
    }
    fun retry(){
        if(busy.value||retryJob?.isActive==true)return
        busy.value=true
        retryJob=scope.launch{
            try{
                ApiClient.checkServer(ApiClient.baseUrl)
                if(visible.value){
                    val oldCalls=synchronized(this@StartupReadiness){attempt.value++;val old=calls.toList();calls.clear();pending.clear();pending.add("session");quiet?.cancel();ready.value=false;failure.value=null;old}
                    oldCalls.forEach{it.cancel()};watchdog()
                }else{connection.value=null;busy.value=false}
            }catch(error:Exception){report(error,(error as? ServerCheckException)?.status);busy.value=false}
        }
    }
    @Synchronized fun dismiss(){visible.value=false;pending.clear();calls.clear();quiet?.cancel();watchdog?.cancel();connection.value=null;busy.value=false}
    fun listener():EventListener {
        val generation=attempt.value
        return object:EventListener(){
            override fun callStart(call:Call){if(visible.value&&generation==attempt.value){call.timeout().timeout(15,TimeUnit.SECONDS);synchronized(this@StartupReadiness){calls.add(call)};begin(call)}}
            override fun responseHeadersEnd(call:Call,response:Response){if(generation!=attempt.value)return;if(response.code>=500)report(status=response.code)else if(response.isSuccessful&&!visible.value)connection.value=null}
            override fun callEnd(call:Call){synchronized(this@StartupReadiness){calls.remove(call)};if(generation==attempt.value)finish(call)}
            override fun callFailed(call:Call,ioe:IOException){synchronized(this@StartupReadiness){calls.remove(call)};if(generation==attempt.value){if(!call.isCanceled()||ioe is java.io.InterruptedIOException)report(ioe);finish(call)}}
        }
    }
}
