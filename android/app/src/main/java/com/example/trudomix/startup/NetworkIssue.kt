package com.example.trudomix.startup

import java.net.SocketTimeoutException
import java.io.InterruptedIOException

enum class StartupState { INITIALIZING, ANIMATING, READY, OFFLINE, SERVER_UNAVAILABLE, ERROR }
data class NetworkIssue(val state:StartupState,val kind:String,val title:String,val message:String) {
    companion object {
        fun from(error:Throwable?=null,offline:Boolean=false,status:Int?=null):NetworkIssue = when {
            status==401||status==403 -> NetworkIssue(StartupState.ERROR,"AUTH","Необходимо войти в аккаунт","Войдите снова, чтобы продолжить.")
            status!=null&&status>=500 -> NetworkIssue(StartupState.ERROR,"SERVER","Ошибка сервера","Сервер временно не может обработать запрос. Попробуйте ещё раз.")
            error is SocketTimeoutException||error is InterruptedIOException -> NetworkIssue(StartupState.ERROR,"TIMEOUT","Сервер не ответил вовремя","Проверьте соединение и повторите подключение.")
            offline -> NetworkIssue(StartupState.OFFLINE,"OFFLINE","Нет подключения к интернету","Проверьте Wi-Fi или мобильное соединение и попробуйте снова.")
            else -> NetworkIssue(StartupState.SERVER_UNAVAILABLE,"UNAVAILABLE","Нет соединения с сервером","Интернет может работать, но сервер временно недоступен. Попробуйте ещё раз.")
        }
    }
}
