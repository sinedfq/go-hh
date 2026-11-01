package com.example.trudomix.ui.screens.chats

import com.example.trudomix.data.models.Message

/** Server versions win; messages sent during the request survive until the next server poll. */
internal fun mergeConversationMessages(server:List<Message>,local:List<Message>):List<Message>{
    if(server==local)return local
    val byId=HashMap<Int,Message>(server.size+local.size)
    local.forEach{byId[it.id]=it}
    server.forEach{byId[it.id]=it}
    val result=byId.values.sortedBy{it.id}
    return if(result==local)local else result
}
