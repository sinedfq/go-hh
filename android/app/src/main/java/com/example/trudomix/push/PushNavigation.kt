package com.example.trudomix.push
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
object PushNavigation{
    val target=MutableStateFlow<String?>(null)
    fun accept(intent:Intent?){
        val data = listOf("type", "conversation_id", "application_id", "resume_id", "vacancy_id")
            .mapNotNull { key -> intent?.getStringExtra(key)?.let { key to it } }.toMap()
        notificationTarget(data)?.let { target.value = it }
    }
}
