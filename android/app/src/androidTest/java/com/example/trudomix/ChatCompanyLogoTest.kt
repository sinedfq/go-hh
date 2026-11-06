package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.screens.chats.ConversationCard
import com.example.trudomix.ui.screens.chats.ConversationState
import com.example.trudomix.ui.screens.chat.ConversationContent
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class ChatCompanyLogoTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun imageServer():MockWebServer {
        val bytes=ByteArrayOutputStream().also{stream->val bitmap=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.BLUE);bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);bitmap.recycle()}.toByteArray()
        return MockWebServer().apply{dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest)=MockResponse().setHeader("Content-Type","image/png").setBody(Buffer().write(bytes))};start()}
    }
    @Test fun candidateSeesCompanyLogoAndEmployerSeesCandidatePhoto(){
        val server=imageServer()
        try {
            var user by mutableIntStateOf(1)
            val conversation=Conversation(id=7,company_name="Команда",candidate_id=1,candidate_name="Кандидат",company_logo=server.url("/company.png").toString(),candidate_photo=server.url("/candidate.png").toString())
            compose.setContent{MaterialTheme{ConversationCard(conversation,user)}}
            compose.onNodeWithContentDescription("Логотип: Команда").assertExists()
            compose.runOnIdle{user=2}
            compose.onNodeWithContentDescription("Аватар: Кандидат").assertExists()
            compose.onNodeWithContentDescription("Логотип: Команда").assertDoesNotExist()
        }finally{server.shutdown()}
    }
    @Test fun conversationHeaderUsesCompanyLogoWhileMessageKeepsSenderAvatar(){
        val server=imageServer()
        try {
            val conversation=Conversation(id=7,company_name="Команда",candidate_id=1,company_logo=server.url("/company.png").toString())
            val message=Message(id=1,conversation_id=7,sender_id=2,content="Здравствуйте",created_at="2026-11-06T10:00:00Z",sender_name="Рекрутер",sender_photo=server.url("/sender.png").toString())
            compose.setContent{MaterialTheme{ConversationContent(7,ConversationState(me=1,loading=false,conversation=conversation,messages=listOf(message)))}}
            compose.onNodeWithContentDescription("Логотип: Команда").assertExists()
            compose.onNodeWithContentDescription("Аватар: Рекрутер").assertExists()
        }finally{server.shutdown()}
    }
}
