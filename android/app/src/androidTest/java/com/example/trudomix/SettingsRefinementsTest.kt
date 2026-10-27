package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.local.AppThemeMode
import com.example.trudomix.push.AppNotifications
import com.example.trudomix.push.NotificationPreferences
import com.example.trudomix.ui.screens.settings.*
import com.example.trudomix.ui.theme.ThemeWaveHost
import com.example.trudomix.ui.theme.TrudomixTheme
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class SettingsRefinementsTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun themeCardsRevealFromSelectionAndKeepNavigationContent(){
        val mode=mutableStateOf(AppThemeMode.LIGHT)
        compose.mainClock.autoAdvance=false
        compose.setContent{TrudomixTheme(darkTheme=mode.value==AppThemeMode.DARK){
            ThemeWaveHost(compose.activity.window,mode.value==AppThemeMode.DARK){ThemePicker(mode.value){mode.value=it}}
        }}
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Тёмная").performClick()
        compose.waitUntil(5000){mode.value==AppThemeMode.DARK}
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithTag("theme-wave-overlay").assertIsDisplayed()
        screenshot("native-theme-wave-middle.png")
        compose.mainClock.advanceTimeBy(1200)
        compose.onNodeWithTag("theme-wave-overlay").assertDoesNotExist()
        compose.onNodeWithText("Выберите оформление и посмотрите пример").assertIsDisplayed()
        screenshot("native-theme-picker-dark.png")
    }
    @Test fun legalDocumentsAreOfflineDraftsWithContactAndNoAutomaticAcceptance(){
        val catalog=compose.activity.assets.open("documents.json").bufferedReader().use{Json{ignoreUnknownKeys=true}.decodeFromString<LegalCatalog>(it.readText())}
        assertEquals("draft",catalog.status);assertEquals("denis.khukhareff@yandex.ru",catalog.contactEmail)
        assertEquals(6,catalog.documents.size)
        compose.setContent{TrudomixTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){LegalDocuments()}}}
        compose.onNodeWithText("Политика конфиденциальности").performClick()
        compose.onNodeWithText(catalog.notice).assertIsDisplayed()
        val screen=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"legal-reader.png").outputStream().use{screen.compress(Bitmap.CompressFormat.PNG,100,it)};screen.recycle()
        compose.onNodeWithText("Написать нам").assertIsDisplayed()
        compose.onNodeWithText("6. Обращения пользователя").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Закрыть").performClick()
        compose.onNodeWithText("Проекты документов сервиса").assertIsDisplayed()
    }
    @Test fun disablingNotificationsPersistsAndBlocksDeliveryImmediately(){
        val context=compose.activity.applicationContext
        val original=NotificationPreferences.isEnabled(context)
        try{
            NotificationPreferences.setEnabled(context,true)
            compose.setContent{TrudomixTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){NotificationSettings()}}}
            compose.onNodeWithContentDescription("Получать уведомления").assertIsOn().performClick()
            compose.onNodeWithContentDescription("Получать уведомления").assertIsOff()
            assertFalse(NotificationPreferences.isEnabled(context))
            NotificationPreferences.initialize(context)
            assertFalse(NotificationPreferences.enabled.value)
            assertFalse(AppNotifications.allowed(context))
            assertFalse(AppNotifications.show(context,"Проверка","Не должно появиться",emptyMap(),"disabled-check"))
            compose.onNodeWithText("Уведомления отключены. Сообщения и события остаются доступны внутри приложения.").assertIsDisplayed()
        }finally{NotificationPreferences.setEnabled(context,original)}
    }
    private fun screenshot(name:String){
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
}
