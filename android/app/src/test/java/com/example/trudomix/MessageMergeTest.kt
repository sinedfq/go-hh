package com.example.trudomix

import com.example.trudomix.data.models.Message
import com.example.trudomix.ui.screens.chats.mergeConversationMessages
import org.junit.Assert.*
import org.junit.Test

class MessageMergeTest{
    private fun message(id:Int,read:Boolean=false)=Message(id,1,2,"Message $id","",is_read=read)
    @Test fun serverReadStatusWinsAndConcurrentLocalSendSurvives(){
        val local=listOf(message(1),message(2),message(3))
        val result=mergeConversationMessages(listOf(message(2,true),message(1,true)),local)
        assertEquals(listOf(1,2,3),result.map{it.id})
        assertTrue(result[0].is_read);assertTrue(result[1].is_read)
        assertEquals(local[2],result[2])
    }
    @Test fun unchangedPollReusesTheExistingList(){
        val local=(1..5000).map{message(it)}
        assertSame(local,mergeConversationMessages(local.toList(),local))
        assertEquals(5001,mergeConversationMessages((1..5000).map{message(it,true)},local+message(5001)).size)
    }
}
