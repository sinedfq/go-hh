package com.example.trudomix.data.api

import retrofit2.Response
import org.json.JSONObject

class PhoneOtpException(val status:Int,message:String,val retryAfter:Int=0):Exception(message)
fun <T> Response<T>.phoneOtpBody():T{
    if(isSuccessful)return body()?:throw PhoneOtpException(500,"Сервер вернул пустой ответ")
    val text=runCatching{JSONObject(errorBody()?.string().orEmpty()).optString("error")}.getOrNull()?.takeIf{it.isNotBlank()&&it.length<=300&&!it.contains('<')}
    throw PhoneOtpException(code(),text?:httpErrorMessage(code()),headers()["Retry-After"]?.toIntOrNull()?:0)
}
fun phoneOtpError(error:Throwable)=if(error is PhoneOtpException)error.message.orEmpty()else userErrorMessage(error)
