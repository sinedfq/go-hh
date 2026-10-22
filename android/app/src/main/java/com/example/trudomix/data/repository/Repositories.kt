package com.example.trudomix.data.repository

import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.models.*
import retrofit2.Response

class ApiException(val status: Int) : Exception(com.example.trudomix.data.api.httpErrorMessage(status))

fun <T> Response<T>.bodyOrThrow(): T {
    if (!isSuccessful) throw ApiException(code())
    return body() ?: error("Сервер вернул пустой ответ")
}
fun Response<*>.checkSuccess() { if (!isSuccessful) throw ApiException(code()) }

class VacancyRepository {
    private val api = ApiClient.api
    suspend fun search(query: String, location: String?, experience: String?, remote: Boolean?, skills: String?, offset: Int,
                       company: String? = null, hasCoordinates: Boolean = false, sort: String = "newest",salaryMin:Int?=null,salaryMax:Int?=null,currency:String?=null,salaryOnly:Boolean=false,employment:String?=null,schedule:String?=null) =
        api.searchVacancies(query, location, experience, remote, skills, 20, offset, company, hasCoordinates, sort,salaryMin,salaryMax,currency,salaryOnly,employment,schedule).bodyOrThrow()
    suspend fun favorites() = api.getFavorites().bodyOrThrow()
    suspend fun favorite(id: Int, selected: Boolean) {
        if (selected) api.addFavorite(FavoriteRequest(id)).checkSuccess() else api.removeFavorite(id).checkSuccess()
    }
}
class ChatRepository {
    private val api = ApiClient.api
    suspend fun conversations() = api.getConversations().bodyOrThrow()
    suspend fun messages(id: Int) = api.getMessages(id).bodyOrThrow()
    suspend fun send(id: Int, text: String) = api.sendMessage(id, MessageRequest(text)).bodyOrThrow()
    suspend fun typing(id: Int) = api.getTyping(id).bodyOrThrow().typing_users
    suspend fun markTyping(id: Int) = api.markTyping(id).checkSuccess()
    suspend fun me() = api.getMe().bodyOrThrow()
    suspend fun unread() = api.getUnreadCount().bodyOrThrow().count
}
