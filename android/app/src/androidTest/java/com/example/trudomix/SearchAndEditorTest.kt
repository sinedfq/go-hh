package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.components.*
import com.example.trudomix.ui.screens.resume.ResumeEditorContent
import com.example.trudomix.ui.screens.chat.ConversationContent
import com.example.trudomix.ui.screens.chats.ConversationState
import com.example.trudomix.ui.screens.vacancies.VacancyFilters
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class SearchAndEditorTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun cityDropdownAndCustomPositionCanBeSelected() {
        var city="";var position=""
        compose.setContent{TrudomixTheme(darkTheme=false){Column {
            var c by remember{mutableStateOf("")};var p by remember{mutableStateOf("")}
            SuggestionField(c,{c=it;city=it},"Город",listOf("Москва","Новосибирск"))
            SuggestionField(p,{p=it;position=it},"Должность",listOf("Разработчик"),allowCustom=true)
        }}}
        compose.onNodeWithText("Город").performTextInput("Нов")
        compose.onNodeWithText("Новосибирск").performClick()
        compose.runOnIdle{assertEquals("Новосибирск",city)}
        compose.onNodeWithText("Должность").performTextInput("Инженер роботов")
        compose.onNodeWithText("Использовать «Инженер роботов»").performClick()
        compose.runOnIdle{assertEquals("Инженер роботов",position)}
    }
    @Test fun editorKeepsCustomSkillsAndShowsAccuratePercentage() {
        var saved:Resume?=null
        compose.setContent{TrudomixTheme(darkTheme=true){ResumeEditorContent(Resume(id=1,full_name="Имя",desired_position="Инженер",city="Москва",experience="Junior"),DirectoryOptions(skills=listOf("Kotlin"),loading=false),false,onSave={saved=it})}}
        compose.onNodeWithText("Заполнено 50%").assertIsDisplayed()
        capture("resume-editor")
        compose.onNodeWithText("Выбрать или добавить навык").performScrollTo().performTextInput("Новый навык")
        compose.onNodeWithText("Использовать «Новый навык»").performClick()
        compose.onNodeWithText("Сохранить резюме").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(listOf("Новый навык"),saved?.skills);assertEquals(5,saved?.let(::resumeCompletionCount))}
    }
    @Test fun filtersSupportGradeYearsOfficeAndSorting() {
        var selected=setOf<String>();var selectedMode:Boolean?=null;var selectedSort="";var applied=false
        compose.setContent { TrudomixTheme(darkTheme=false) {
            var city by remember{mutableStateOf("")};var company by remember{mutableStateOf("")}
            var experience by remember{mutableStateOf(emptySet<String>())};var skills by remember{mutableStateOf(emptySet<String>())};var skill by remember{mutableStateOf("")}
            var mode by remember{mutableStateOf<Boolean?>(null)};var coordinates by remember{mutableStateOf(false)};var sort by remember{mutableStateOf("newest")}
            VacancyFilters(city,{city=it},company,{company=it},experience,{experience=it;selected=it},skills,{skills=it},skill,{skill=it},mode,{mode=it;selectedMode=it},coordinates,{coordinates=it},sort,{sort=it;selectedSort=it},DirectoryOptions(cities=listOf("Новосибирск"),loading=false),{},onApply={applied=true},onReset={})
        } }
        compose.onNodeWithText("В офисе").performScrollTo().performClick()
        compose.onNodeWithText("Junior").performScrollTo().performClick()
        compose.onNodeWithText("1-3 года").performScrollTo().performClick()
        compose.onNodeWithText("Популярные").performScrollTo().performClick()
        compose.onNodeWithText("Показать вакансии").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(setOf("Junior","1-3 года"),selected);assertEquals(false,selectedMode);assertEquals("popular",selectedSort);assertTrue(applied)}
    }
    @Test fun chatOpensVacancySendsAndRetriesFailedDraft() {
        var opened=0;var sent=""
        compose.setContent{TrudomixTheme(darkTheme=false){ConversationContent(7,ConversationState(loading=false,me=1,failedText="Неотправленное",conversation=Conversation(id=7,candidate_id=1,company_name="Команда",vacancy_id=9,vacancy_title="Разработчик",cover_letter="Моё письмо")),onVacancyClick={opened=it},onSend={sent=it})}}
        compose.onNodeWithText("Посмотреть вакансию →").performClick()
        compose.runOnIdle{assertEquals(9,opened)}
        compose.onNodeWithText("Моё письмо").assertIsDisplayed()
        capture("conversation")
        compose.onNodeWithText("Повторить отправку: Неотправленное").performClick()
        compose.runOnIdle{assertEquals("Неотправленное",sent)}
        compose.onNodeWithText("Сообщение…").performTextInput("Привет")
        compose.onNodeWithContentDescription("Отправить").performClick()
        compose.runOnIdle{assertEquals("Привет",sent)}
    }
    @Test fun realEmulatorLocationIsAvailableOnRequest() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveLocation")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(hasLocationPermission(context))
        val location=kotlinx.coroutines.runBlocking{currentDeviceLocation(context)}
        assertTrue(location.latitude in -90.0..90.0)
        assertTrue(location.longitude in -180.0..180.0)
        assertFalse(location.latitude==0.0&&location.longitude==0.0)
        assertTrue(location.accuracy>=0)
    }
    private fun capture(name:String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null),"$name.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }
}
