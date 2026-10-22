package com.example.trudomix.ui.screens.chats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.data.models.Conversation
import com.example.trudomix.ui.components.AppScreenHeader

@Composable
fun ChatListScreen(viewModel:ChatViewModel=viewModel(),onChatClick:(Int)->Unit={}) {
    val state by viewModel.uiState.collectAsState()
    com.example.trudomix.ui.components.RefreshContent(state is ChatListUiState.Loading,{viewModel.loadConversations()}) {
    Box(Modifier.fillMaxSize().statusBarsPadding(),contentAlignment=Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max=760.dp).fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=20.dp,bottom=148.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item { AppScreenHeader(title={Text("Сообщения")},subtitle="Обсуди детали с будущей командой",applyInsets=false,contentPadding=PaddingValues(0.dp),
) }
            when(val current=state) {
                ChatListUiState.Loading->item{LinearProgressIndicator(Modifier.fillMaxWidth())}
                is ChatListUiState.Error->item { OutlinedCard { Column(Modifier.padding(20.dp)) { Text(current.message,color=MaterialTheme.colorScheme.error);TextButton(onClick={viewModel.loadConversations()}){Text("Повторить")}} } }
                is ChatListUiState.Success->{
                    val conversations=current.conversations.filterNotNull()
                    if(conversations.isEmpty())item { OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)) { Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Default.ChatBubbleOutline,null,tint=MaterialTheme.colorScheme.primary);Text("Чаты пока отсутствуют",style=MaterialTheme.typography.titleMedium);Text("Переписка с компанией появится здесь после отклика.",color=MaterialTheme.colorScheme.onSurfaceVariant)} } }
                    items(conversations,key={it.id}){conversation->ConversationCard(conversation,viewModel.userId){if(conversation.id>0)onChatClick(conversation.id)}}
                }
            }
        }
    }
}

}

@Composable
fun ConversationCard(conversation:Conversation,currentUserId:Int=0,onClick:()->Unit={}) {
    val name=if(conversation.candidate_id==currentUserId)conversation.company_name else conversation.candidate_name?:"Собеседник"
    val time=conversation.last_message_time?.takeIf{it.isNotBlank()}?:conversation.last_message_at.orEmpty()
    OutlinedCard(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(48.dp)){Box(contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onPrimaryContainer)}}
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row { Text(name,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);if(time.isNotBlank())Text(formatMessageTime(time),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                conversation.vacancy_title?.takeIf{it.isNotBlank()}?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary,maxLines=2,overflow=TextOverflow.Ellipsis)}
                Row(verticalAlignment=Alignment.CenterVertically){Text(conversation.last_message?.takeIf{it.isNotBlank()}?:"Начните разговор",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis);if(conversation.unread_count>0)Badge{Text(conversation.unread_count.toString())}}
            }
        }
    }
}
