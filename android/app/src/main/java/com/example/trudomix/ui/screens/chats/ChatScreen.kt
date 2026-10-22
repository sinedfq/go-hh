package com.example.trudomix.ui.screens.chat

import com.example.trudomix.ui.components.AppScreenHeader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import com.example.trudomix.ui.components.SenderAvatar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.ui.screens.chats.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(conversationId:Int,onBackClick:()->Unit,onVacancyClick:(Int)->Unit = {}){
    val model:ConversationViewModel=viewModel(key="conversation-$conversationId")
    val state by model.state.collectAsState()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(conversationId,lifecycle){lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){model.observe(conversationId)}}
    ConversationContent(conversationId,state,onBackClick,onVacancyClick,{model.send(conversationId,it)},{model.typing(conversationId)})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationContent(conversationId:Int,state:ConversationState,onBackClick:()->Unit={},onVacancyClick:(Int)->Unit={},onSend:(String)->Unit={},onTyping:()->Unit={}) {
    var text by rememberSaveable(conversationId){mutableStateOf("")}
    val list=rememberLazyListState()
    val lastId=state.messages.lastOrNull()?.id
    LaunchedEffect(lastId,state.conversation?.cover_letter){if(state.messages.isNotEmpty())list.animateScrollToItem(state.messages.lastIndex + if(!state.conversation?.cover_letter.isNullOrBlank()) 1 else 0)}
    Scaffold(topBar={AppScreenHeader(title={Text(state.conversation?.let{if(it.candidate_id==state.me)it.company_name else it.candidate_name}?.takeIf{it.isNotBlank()} ?: "Переписка",maxLines=2,overflow=TextOverflow.Ellipsis)},subtitle=if(state.typing)"Собеседник печатает…" else "Обсудите будущую работу",navigationIcon={IconButton(onClick=onBackClick){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Назад")}})}){padding->
        Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.TopCenter){Column(Modifier.widthIn(max=840.dp).fillMaxSize().imePadding()){
            state.conversation?.let { conversation ->
                conversation.vacancy_id?.takeIf { it > 0 }?.let { vacancyId ->
                    OutlinedCard(onClick = { onVacancyClick(vacancyId) }, shape=RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(conversation.vacancy_title ?: "Вакансия", style=MaterialTheme.typography.titleSmall, maxLines=2, overflow=TextOverflow.Ellipsis)
                            Text(if(conversation.vacancy_archived) "В архиве · Посмотреть вакансию →" else "Посмотреть вакансию →",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            state.contextError?.let { Text(it,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(12.dp)) }
            if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(state.error!=null)Text(state.error!!,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(12.dp))
            LazyColumn(state=list,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(horizontal=20.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                state.conversation?.cover_letter?.takeIf { it.isNotBlank() }?.let { letter ->
                    item(key="cover-letter") {
                        OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text("Сопроводительное письмо",style=MaterialTheme.typography.titleSmall)
                                Text(letter,style=MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if(!state.loading && state.messages.isEmpty())item{Text("Начните разговор",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                items(state.messages,key={it.id}){message->
                    val mine=message.sender_id==state.me
                    val name=message.sender_name.ifBlank{if(mine)state.meName.ifBlank{"Вы"}else "Собеседник"}
                    val photo=message.sender_photo.ifBlank{if(mine)state.mePhoto else ""}
                    val colors=MaterialTheme.colorScheme
                    Row(Modifier.fillMaxWidth().padding(start=if(mine)32.dp else 0.dp,end=if(mine)0.dp else 32.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,if(mine)Alignment.End else Alignment.Start),verticalAlignment=Alignment.Bottom){
                        if(!mine)SenderAvatar(photo,name)
                        Surface(shape=RoundedCornerShape(24.dp),color=if(mine)colors.primaryContainer else colors.surface,contentColor=colors.onSurface,border=BorderStroke(1.dp,if(mine)colors.primary.copy(alpha=.2f)else colors.outlineVariant),modifier=Modifier.weight(1f,fill=false).widthIn(max=540.dp)){
                            Column(Modifier.padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                                Text(if(mine)"Вы" else name,style=MaterialTheme.typography.labelMedium,color=colors.primary,maxLines=1,overflow=TextOverflow.Ellipsis)
                                SelectionContainer{Text(message.content)}
                                Text(formatMessageTime(message.created_at)+(if(mine && message.is_read) " · Прочитано" else ""),modifier=Modifier.align(Alignment.End),style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
                            }
                        }
                        if(mine)SenderAvatar(photo,name)
                    }
                }
            }
            if(state.failedText!=null)TextButton(onClick={onSend(state.failedText)},enabled=!state.sending){Text("Повторить отправку: "+state.failedText.take(35))}
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(text,{text=it;if(it.isNotBlank())onTyping()},shape=RoundedCornerShape(24.dp),modifier=Modifier.weight(1f),placeholder={Text("Сообщение…")},maxLines=5,isError=text.length>4000,supportingText=if(text.length>3800){{Text("${text.length} / 4000")}}else null)
                FilledIconButton(onClick={onSend(text);text=""},modifier=Modifier.size(48.dp),enabled=text.isNotBlank()&&text.length<=4000&&!state.sending){if(state.sending)CircularProgressIndicator(Modifier.size(24.dp))else Icon(Icons.AutoMirrored.Filled.Send,"Отправить")}
            }
        }
        }
    }
}
