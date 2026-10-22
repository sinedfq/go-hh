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
import com.example.trudomix.ui.theme.TrudomixTheme
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Exercises real navigation with a disposable API. Restores the user's session. */
class MobileGrowthTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun homeSavedSearchesMarkdownProfileAndResume() {
        val calls=CopyOnWriteArrayList<String>()
        val savedQuery=AtomicReference("")
        val searchQuery=AtomicReference("")
        val initialSearch="""{"id":1,"name":"Go в офисе","filters":{"query":"Go","location":"Новосибирск","experience":"Middle","remote":false,"skills":["Go"],"salary_min":120000,"currency":"RUB","employment":"full_time","schedule":"flexible"}}"""
        val saved=AtomicReference("[$initialSearch]")
        val user="""{"id":9999,"email":"preview@example.test","role":"candidate","photo_url":"","created_at":"2026-01-01T00:00:00Z"}"""
        val resume="""{"id":99,"user_id":9999,"full_name":"Александр Иванов","desired_position":"Go-разработчик","city":"Новосибирск","experience":"Middle","skills":["Go","SQL"],"total_experience_months":36,"total_experience":"3 года","work_experience":[{"id":1,"company":"Первая команда","position":"Инженер","start_date":"2023-01-01T00:00:00Z","end_date":"2026-01-01T00:00:00Z","description":"Создавал сервисы"}]}"""
        val vacancy="""{"id":1,"title":"Go-разработчик платформы","company":"Северный Код","company_id":99,"company_it_accredited":true,"location":"Новосибирск","experience":"Middle","remote":false,"salary_from":120000,"salary_to":180000,"currency":"RUB","employment":"full_time","schedule":"flexible","description":"## Наша команда\n\n**Разрабатываем сервисы** вместе.\n\n- Go и PostgreSQL\n- Поддержка наставника\n\n![Команда на работе](/uploads/team.png)"}"""
        val jobs=(1..20).joinToString(",",prefix="[",postfix="]"){if(it==1)vacancy else vacancy.replace("\"id\":1,","\"id\":$it,").replace("Go-разработчик платформы","Go-разработчик $it")}
        val server=MockWebServer()
        server.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse {
            val path=request.requestUrl!!.encodedPath;calls.add(request.method+" "+path)
            if(path=="/uploads/team.png")return MockResponse().setHeader("Content-Type","image/png").setBody(okio.Buffer().write(android.util.Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII=",0)))
            val body=when(path){
                "/api/me"->user
                "/api/my-resume"->resume
                "/api/my-resume/stats"->"""{"total_views":28}"""
                "/api/my-applications","/api/favorites","/api/skills","/api/positions","/api/conversations"->"[]"
                "/api/conversations/unread-count"->"""{"count":0}"""
                "/api/vacancies/search"->{searchQuery.set(request.requestUrl!!.encodedQuery.orEmpty());"""{"vacancies":$jobs,"total_count":20}"""}
                "/api/vacancies/1"->vacancy
                "/api/vacancies/1/application"->"""{"applied":false}"""
                "/api/companies/99"->"""{"id":99,"name":"Северный Код","it_accredited":true}"""
                "/api/saved-searches"->if(request.method=="POST"){savedQuery.set(request.body.readUtf8());saved.set("[$initialSearch,"+initialSearch.replace("\"id\":1","\"id\":2").replace("Go в офисе","Мой Go")+"]");initialSearch.replace("\"id\":1","\"id\":2").replace("Go в офисе","Мой Go")}else saved.get()
                "/api/saved-searches/2"->{saved.set("[$initialSearch]");return MockResponse().setResponseCode(204)}
                else->"[]"
            }
            return MockResponse().setHeader("Content-Type","application/json").setBody(body).setResponseCode(if(request.method=="POST"&&path=="/api/saved-searches")201 else 200)
        }}
        server.start()
        val oldServer=ApiClient.baseUrl
        val oldIdentity=SessionIdentity.user.value
        val oldToken=runBlocking{ApiClient.getTokenManager().getToken()}
        var active by mutableStateOf(true)
        fun waitText(text:String){compose.waitUntil(20000){compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
        fun screenshot(name:String){
            compose.waitForIdle();Thread.sleep(900)
            val instrumentation=InstrumentationRegistry.getInstrumentation()
            val bitmap=instrumentation.uiAutomation.takeScreenshot()
            File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
        try {
            runBlocking {ApiClient.setServer(server.url("/").toString());ApiClient.getTokenManager().saveToken("preview-token")}
            SessionIdentity.user.value=null
            compose.setContent {TrudomixTheme(darkTheme=true){if(active)AppNavGraph()}}
            waitText("Твоя карьера")
            waitText("Привет, Александр!")
            assertEquals("A single identity request at startup",1,calls.count{it=="GET /api/me"})
            assertEquals("Home feed is loaded once",1,calls.count{it=="GET /api/vacancies/search"})
            assertEquals("Search tab should not load favorites on startup",0,calls.count{it=="GET /api/favorites"})
            screenshot("mobile-home-new.png")
            compose.onNodeWithContentDescription("Поиск").performClick()
            waitText("Найди свою команду")
            compose.onNodeWithText("Мои поиски").performClick()
            waitText("Go в офисе")
            screenshot("mobile-saved-searches.png")
            compose.onNodeWithText("Показать вакансии").performClick()
            waitText("Найди свою команду")
            compose.waitUntil(10000){searchQuery.get().contains("remote=false")&&searchQuery.get().contains("salary_min=120000")}
            compose.onNodeWithText("Сохранить поиск").performClick()
            compose.onNodeWithText("Название").performTextClearance()
            compose.onNodeWithText("Название").performTextInput("Мой Go")
            compose.onNodeWithText("Сохранить",useUnmergedTree=true).performClick()
            compose.waitUntil(10000){savedQuery.get().contains("remote=false")&&savedQuery.get().contains("salary_min")}
            compose.onNodeWithText("Мои поиски").performClick()
            waitText("Мой Go")
            compose.onNodeWithContentDescription("Удалить поиск: Мой Go").performClick()
            compose.onNodeWithText("Удалить",useUnmergedTree=true).performClick()
            compose.waitUntil(10000){compose.onAllNodesWithText("Мой Go").fetchSemanticsNodes().isEmpty()}
            compose.onNodeWithContentDescription("Назад").performClick()
            waitText("Найди свою команду")
            compose.onNodeWithContentDescription("Поиск").performClick()
            waitText("Найди свою команду")
            compose.onNodeWithText("Go-разработчик платформы").performScrollTo().performClick()
            waitText("Детали вакансии")
            compose.waitUntil(20000){compose.onAllNodesWithContentDescription("IT-аккредитация указана компанией").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithContentDescription("IT-аккредитация указана компанией").performClick()
            waitText("Компания имеет статус аккредитованной IT-компании.")
            screenshot("mobile-it-comment.png")
            androidx.test.espresso.Espresso.pressBack()
            compose.onNodeWithText("Наша команда").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Разрабатываем сервисы вместе.",substring=true).assertExists()
            screenshot("mobile-markdown-vacancy.png")
            compose.onNodeWithContentDescription("Профиль").performClick()
            waitText("Александр Иванов")
            compose.onNodeWithText("Моё резюме",useUnmergedTree=true).performScrollTo().performClick()
            waitText("Александр Иванов")
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Общий опыт: 3 года"))
            compose.onNodeWithText("Общий опыт: 3 года").performScrollTo().assertIsDisplayed()
            screenshot("mobile-resume-experience.png")
            compose.onNodeWithContentDescription("Назад").performClick()
            waitText("Профиль")
            androidx.test.espresso.Espresso.pressBack()
            waitText("Детали вакансии")
        } catch(failure:Throwable) {
            val instrumentation=InstrumentationRegistry.getInstrumentation()
            File(instrumentation.targetContext.getExternalFilesDir(null),"mobile-growth-failure.txt").writeText(calls.joinToString("\n")+"\n\n"+compose.onRoot().printToString())
            screenshot("mobile-growth-failure.png")
            throw failure
        } finally {
            compose.runOnIdle{active=false}
            runBlocking{ApiClient.setServer(oldServer);if(oldToken!=null)ApiClient.getTokenManager().saveToken(oldToken)else ApiClient.getTokenManager().clearSession()}
            SessionIdentity.user.value=oldIdentity
            server.shutdown()
        }
    }
}
