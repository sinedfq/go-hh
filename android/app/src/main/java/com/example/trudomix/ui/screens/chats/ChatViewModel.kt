package com.example.trudomix.ui.screens.chats
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trudomix.data.models.Conversation
import com.example.trudomix.data.repository.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
sealed interface ChatListUiState {
    data object Loading:ChatListUiState
    data class Success(val conversations:List<Conversation>):ChatListUiState
    data class Error(val message:String):ChatListUiState
}
class ChatViewModel:ViewModel(){
    private val repo=ChatRepository()
    private val _uiState=MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState=_uiState.asStateFlow()
    var userId by androidx.compose.runtime.mutableIntStateOf(0);private set
    private var loading=false
    init{loadConversations()}
    fun loadConversations(silent:Boolean=false){
        if(loading)return
        loading=true
        viewModelScope.launch{
            if(!silent)_uiState.value=ChatListUiState.Loading
            try{if(userId==0)userId=com.example.trudomix.data.local.SessionIdentity.user.value?.id?:repo.me().id;_uiState.value=ChatListUiState.Success(repo.conversations())}
            catch(e:CancellationException){throw e}
            catch(e:Exception){if(!silent || _uiState.value !is ChatListUiState.Success)_uiState.value=ChatListUiState.Error(com.example.trudomix.data.api.userErrorMessage(e))}
            finally{loading=false}
        }
    }
}
