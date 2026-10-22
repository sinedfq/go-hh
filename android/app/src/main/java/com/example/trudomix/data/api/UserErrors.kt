package com.example.trudomix.data.api

import com.example.trudomix.data.repository.ApiException
import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException

fun httpErrorMessage(status: Int, authentication: Boolean = false, registration: Boolean = false): String = when {
    authentication && status in listOf(401,403) -> "Неверная почта или пароль. Проверь данные и попробуй ещё раз."
    registration && status == 409 -> "Аккаунт с такой почтой уже существует. Попробуй войти."
    status == 400 || status == 422 -> "Проверь заполненные поля и попробуй ещё раз."
    status == 401 -> "Сессия истекла. Войди в аккаунт заново."
    status == 403 -> "У тебя нет доступа к этому действию."
    status == 404 -> "Данные больше не доступны. Обнови экран."
    status == 409 -> "Данные уже изменились. Обнови экран и повтори действие."
    status == 413 -> "Файл слишком большой. Выбери фотографию меньшего размера."
    status == 429 -> "Слишком много попыток. Подожди немного и попробуй снова."
    else -> "Сервер временно недоступен. Попробуй немного позже."
}
fun userErrorMessage(error: Throwable): String = when (error) {
    is kotlinx.coroutines.CancellationException -> throw error
    is SocketTimeoutException -> "Сервер не ответил вовремя. Проверь соединение и повтори попытку."
    is IOException -> "Нет соединения с сервером. Проверь интернет и адрес сервера в настройках."
    is ApiException -> httpErrorMessage(error.status)
    is HttpException -> httpErrorMessage(error.code())
    else -> "Не получилось выполнить действие. Попробуй ещё раз."
}
