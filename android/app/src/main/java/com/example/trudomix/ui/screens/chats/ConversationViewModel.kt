package com.example.trudomix.ui.screens.chats

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.models.Message
import com.example.trudomix.data.models.Conversation
import com.example.trudomix.data.repository.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ConversationState(val messages:List<Message> = emptyList(),val me:Int=0,val loading:Boolean=true,val sending:Boolean=false,val error:String?=null,val failedText:String?=null,val typing:Boolean=false,val conversation:Conversation?=null,val contextError:String?=null,val mePhoto:String="",val meName:String="")
class ConversationViewModel:ViewModel(){
    private val repo=ChatRepository()
    private val _state=MutableStateFlow(ConversationState())
    val state=_state.asStateFlow()
    private var lastTyping=0L
    suspend fun observe(id:Int){
        // Called from repeatOnLifecycle; its cancellation owns the only refresh loop.
        while(currentCoroutineContext().isActive){
            if (_state.value.conversation == null) {
                try {
                    val conversation = repo.conversations().firstOrNull { it.id == id }
                    _state.update { it.copy(conversation = conversation, contextError = if (conversation == null) "Информация об отклике недоступна" else null) }
                } catch(e:CancellationException) { throw e }
                catch(_:Exception) { _state.update { it.copy(contextError = "Не удалось загрузить информацию об отклике. Повторяем подключение…") } }
            }
            try{
                if(_state.value.me==0) { val me=repo.me(); _state.update{it.copy(me=me.id,mePhoto=me.photo_url,meName=me.email)} }
                val messages=repo.messages(id)
                val typing=repo.typing(id).isNotEmpty()
                _state.update{it.copy(messages=(messages+it.messages.filter { old -> messages.none { m -> m.id==old.id } }).sortedBy{m->m.id},loading=false,error=null,typing=typing)}
            }catch(e:CancellationException){throw e}
            catch(e:Exception){_state.update{it.copy(loading=false,error=com.example.trudomix.data.api.userErrorMessage(e))}}
            delay(3000)
        }
    }
    fun send(id:Int,text:String){
        val content=text.trim()
        if(content.isEmpty()||content.length>4000||_state.value.sending)return
        _state.update{it.copy(sending=true,failedText=null,error=null)}
        viewModelScope.launch{
            try{
                val message=repo.send(id,content)
                _state.update{it.copy(messages=(it.messages+message).distinctBy{m->m.id}.sortedBy{m->m.id},sending=false)}
            }catch(e:CancellationException){throw e}
            catch(e:Exception){_state.update{it.copy(sending=false,failedText=content,error="Сообщение не отправлено. Проверьте соединение.")}}
        }
    }
    fun typing(id:Int){
        val now=SystemClock.elapsedRealtime()
        if(now-lastTyping<2500)return
        lastTyping=now
        viewModelScope.launch{try{repo.markTyping(id)}catch(e:CancellationException){throw e}catch(_:Exception){}}
    }
}
