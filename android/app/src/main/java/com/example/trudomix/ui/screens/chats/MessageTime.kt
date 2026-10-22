package com.example.trudomix.ui.screens.chats
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
fun formatMessageTime(value:String):String=try{OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))}catch(_:Exception){value.take(16).replace('T',' ')}
