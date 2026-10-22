package com.example.trudomix.push
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
object PushNavigation{
    val target=MutableStateFlow<String?>(null)
    fun accept(intent:Intent?){
        val conversation=intent?.getStringExtra("conversation_id")?.toIntOrNull()
        val application=intent?.getStringExtra("application_id")?.toIntOrNull()
        if(conversation!=null&&conversation>0)target.value="chat_detail/$conversation"
        else if(application!=null&&application>0)target.value="applications"
    }
}
