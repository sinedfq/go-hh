package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.data.local.SessionIdentity
import com.example.trudomix.ui.navigation.AppNavGraph
import com.example.trudomix.ui.screens.companies.CompanyScreen
import com.example.trudomix.ui.theme.TrudomixTheme
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class CompanyExperienceTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val company="""{"id":99,"name":"Команда","description":"Создаём продукты","size":"11-50","city":"Новосибирск","team_story":"Развиваем полезные сервисы","benefits":["Обучение за счёт компании"],"hiring_process":"Знакомство и встреча с командой","photos":["/uploads/one.png","/uploads/two.png"],"videos":["/uploads/story.mp4"]}"""

    @Test fun companyShowsAllSectionsAndPhotoVideoStories(){withServer(false){
        compose.setContent{TrudomixTheme(darkTheme=true){CompanyScreen(99,{},{})}}
        waitText("Жизнь команды")
        compose.onNodeWithTag("company-content").performScrollToNode(hasText("Наша команда"))
        compose.onNodeWithTag("company-content").performScrollToNode(hasText("Как проходит найм"))
        compose.onNodeWithText("Как проходит найм").assertIsDisplayed()
        compose.onNodeWithTag("company-content").performScrollToNode(hasText("Обучение за счёт компании",substring=true))
        compose.onNodeWithTag("company-content").performScrollToNode(hasContentDescription("Фото компании 1"))
        compose.onNodeWithContentDescription("Фото компании 1").performClick()
        waitText("1 / 3")
        compose.onNodeWithContentDescription("История 1").performTouchInput{swipeLeft()}
        waitText("2 / 3")
        compose.onNodeWithText("Далее").performClick()
        waitText("3 / 3")
        compose.waitUntil(20000){compose.onAllNodesWithContentDescription("Видео воспроизводится").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("Пауза").performClick()
        compose.onNodeWithContentDescription("Видео на паузе").assertExists()
        Thread.sleep(300)
        screenshot("company-story-video.png")
        compose.onNodeWithContentDescription("Закрыть истории").performClick()
        compose.onNodeWithContentDescription("Закрыть истории").assertDoesNotExist()
    }}

    @Test fun employerTabsOpenOwnVacanciesApplicationsAndCompany(){withServer(true){calls->
        compose.setContent{TrudomixTheme(darkTheme=false){AppNavGraph()}}
        waitText("Ваша команда")
        compose.onNodeWithContentDescription("Вакансии").performClick()
        waitText("Мои вакансии")
        waitText("Новая вакансия")
        compose.onNodeWithContentDescription("Отклики").performClick()
        waitText("Александр")
        assertFalse(calls.contains("/api/my-applications"))
        compose.onNodeWithContentDescription("Моя компания").performClick()
        waitText("О компании")
        waitText("Добавить фото или видео")
        waitText("Жизнь команды")
        screenshot("employer-bottom-tabs.png")
        compose.onNodeWithContentDescription("Карта вакансий").assertDoesNotExist()
        compose.onNodeWithContentDescription("Поиск").assertDoesNotExist()
    }}

    private fun waitText(text:String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
    private fun screenshot(name:String){compose.waitForIdle();val instrumentation=InstrumentationRegistry.getInstrumentation();val bitmap=instrumentation.uiAutomation.takeScreenshot();File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
    private fun withServer(employer:Boolean,block:(CopyOnWriteArrayList<String>)->Unit){
        val calls=CopyOnWriteArrayList<String>()
        val video=InstrumentationRegistry.getInstrumentation().context.assets.open("company-story.mp4").use{it.readBytes()}
        val image=ByteArrayOutputStream().also{Bitmap.createBitmap(20,30,Bitmap.Config.ARGB_8888).apply{eraseColor(android.graphics.Color.BLUE);compress(Bitmap.CompressFormat.PNG,100,it);recycle()}}.toByteArray()
        val server=MockWebServer()
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse{
            val path=request.requestUrl!!.encodedPath;calls.add(path)
            if(path.endsWith(".mp4")){
                val range=request.getHeader("Range")?.removePrefix("bytes=")?.split('-')
                val start=range?.firstOrNull()?.toIntOrNull()?:0
                val end=(range?.getOrNull(1)?.toIntOrNull()?:video.lastIndex).coerceAtMost(video.lastIndex)
                val response=MockResponse().setHeader("Content-Type","video/mp4").setHeader("Accept-Ranges","bytes").setBody(okio.Buffer().write(video.copyOfRange(start,end+1)))
                if(range!=null)response.setResponseCode(206).setHeader("Content-Range","bytes $start-$end/${video.size}")
                return response
            }
            if(path.endsWith(".png"))return MockResponse().setHeader("Content-Type","image/png").setBody(okio.Buffer().write(image))
            val body=when(path){
                "/api/me"->"""{"id":9999,"email":"preview@example.test","role":"${if(employer)"employer"else "candidate"}","company_id":99,"created_at":"2026-01-01T00:00:00Z"}"""
                "/api/companies/99","/api/my-company"->company
                "/api/my-vacancies"->"""{"company":$company,"vacancies":[]}"""
                "/api/employer/applications"->"""[{"id":8,"vacancy_id":1,"resume_full_name":"Александр","vacancy_title":"Инженер","status":"new"}]"""
                "/api/vacancies/search"->"""{"vacancies":[],"total_count":0}"""
                "/api/employer/stats"->"""{"total_applications":1,"total_vacancy_views":5}"""
                "/api/conversations/unread-count"->"""{"count":0}"""
                else->"[]"
            }
            return MockResponse().setHeader("Content-Type","application/json").setBody(body)
        }}
        server.start()
        val oldServer=ApiClient.baseUrl;val oldIdentity=SessionIdentity.user.value;val oldToken=runBlocking{ApiClient.getTokenManager().getToken()}
        try{runBlocking{ApiClient.setServer(server.url("/").toString());ApiClient.getTokenManager().saveToken("preview-token")};SessionIdentity.user.value=null;block(calls)}
        finally{compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);runBlocking{ApiClient.setServer(oldServer);if(oldToken!=null)ApiClient.getTokenManager().saveToken(oldToken)else ApiClient.getTokenManager().clearSession()};SessionIdentity.user.value=oldIdentity;server.shutdown()}
    }
}
