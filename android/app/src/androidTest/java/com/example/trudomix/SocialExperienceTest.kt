package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.local.SessionIdentity
import com.example.trudomix.data.models.User
import com.example.trudomix.ui.screens.social.*
import com.example.trudomix.ui.theme.TrudomixTheme
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class SocialExperienceTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun subscriptionsPostsLikesAndCommentsWorkWithApi(){
        val following=AtomicBoolean(false);val liked=AtomicBoolean(false);val posted=AtomicBoolean(false);val commented=AtomicBoolean(false)
        val server=MockWebServer()
        val expiry=Instant.now().plusSeconds(86400).toString()
        fun post(id:Int,body:String,own:Boolean)="""{"id":$id,"user_id":${if(own)9999 else 8888},"company_id":${if(own)"null"else "99"},"author_name":"${if(own)"Александр"else "Команда"}","body":"$body","created_at":"2026-10-25T10:00:00Z","likes":${if(liked.get())1 else 0},"liked":${liked.get()},"comments":${if(commented.get())1 else 0},"can_delete":$own}"""
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse{
            val path=request.requestUrl!!.encodedPath
            val json=when{
                path.endsWith("/subscription")->{if(request.method=="PUT")following.set(true);if(request.method=="DELETE")following.set(false);"""{"following":${following.get()},"followers":${if(following.get())1 else 0}}"""}
                path=="/api/feed"->"""{"posts":[${if(posted.get())post(2,"Мой новый пост",true)+","else ""}${post(1,"Новости команды",false)}],"next_cursor":0}"""
                path=="/api/stories"->"""[{"id":1,"company_id":99,"company_name":"Команда","media_url":"/photo.png","expires_at":"$expiry","caption":"День команды"}]"""
                path=="/api/posts"&&request.method=="POST"->{val body=request.body.readUtf8();assertTrue(body.contains("Мой новый пост"));assertTrue(body.contains("as_company"));posted.set(true);"""{"id":2}"""}
                path.endsWith("/like")->{liked.set(request.method=="PUT");"""{"liked":${liked.get()},"likes":${if(liked.get())1 else 0}}"""}
                path.endsWith("/comments")->{if(request.method=="POST"){assertTrue(request.body.readUtf8().contains("Спасибо команде"));commented.set(true);"""{"id":7}"""}else """{"comments":${if(commented.get())"""[{"id":7,"author_name":"Александр","body":"Спасибо команде","can_delete":true}]"""else "[]"},"next_cursor":0}"""}
                else->"{}"
            }
            return MockResponse().setHeader("Content-Type","application/json").setBody(json)
        }}
        server.start()
        val oldServer=ApiClient.baseUrl;val oldIdentity=SessionIdentity.user.value;val oldToken=runBlocking{ApiClient.getTokenManager().getToken()}
        try{
            runBlocking{ApiClient.setServer(server.url("/").toString());ApiClient.getTokenManager().saveToken("preview-token")}
            SessionIdentity.user.value=User(id=9999,role="employer",company_id=99)
            compose.setContent{TrudomixTheme(darkTheme=true){val model:SocialViewModel=viewModel();val state by model.state.collectAsState();LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                item{CompanyFollowButton(99)};item{SocialHomeHeader(state,model)}
                items(state.posts,key={it.id}){post->SocialPostCard(post,post.id in state.busy,{model.like(post)},{model.delete(post)},{model.commentCount(post.id,it)})}
            }}}
            compose.waitUntil(10000){compose.onAllNodes(hasText("Подписаться") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Подписаться").performScrollTo().performClick()
            waitText("Вы подписаны");assertTrue(following.get())
            val preview=compose.onNodeWithTag("story-preview-99",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val logo=compose.onNode(hasContentDescription("Компания: Команда") and hasAnyAncestor(hasTestTag("story-preview-99")),useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertEquals("Story logo is horizontally centered",preview.center.x,logo.center.x,1f)
            assertEquals("Story logo is vertically centered",preview.center.y,logo.center.y,1f)
            compose.onNodeWithText("Написать пост").performScrollTo().performClick()
            compose.onNode(hasSetTextAction() and hasText("Текст публикации")).performTextInput("Мой новый пост")
            compose.onNodeWithText("Опубликовать").performScrollTo().performClick()
            compose.waitUntil(10000){posted.get()}
            compose.onNodeWithText("Мой новый пост").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithContentDescription("Нравится").onFirst().performScrollTo().performClick()
            compose.waitUntil(10000){liked.get()}
            compose.onAllNodesWithText("Комментарии · 0").onFirst().performScrollTo().performClick()
            compose.onNode(hasSetTextAction() and hasText("Ваш комментарий")).performTextInput("Спасибо команде")
            compose.onNodeWithText("Отправить").performScrollTo().performClick()
            waitText("Спасибо команде");assertTrue(commented.get())
            val instrumentation=InstrumentationRegistry.getInstrumentation();val bitmap=instrumentation.uiAutomation.takeScreenshot();File(instrumentation.targetContext.getExternalFilesDir(null),"social-comments.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }finally{compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);runBlocking{ApiClient.setServer(oldServer);if(oldToken!=null)ApiClient.getTokenManager().saveToken(oldToken)else ApiClient.getTokenManager().clearSession()};SessionIdentity.user.value=oldIdentity;server.shutdown()}
    }
    private fun waitText(text:String){compose.waitUntil(10000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
}
