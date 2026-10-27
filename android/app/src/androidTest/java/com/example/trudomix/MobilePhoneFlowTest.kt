package com.example.trudomix

import android.app.Notification
import android.app.NotificationManager
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.push.*
import com.example.trudomix.ui.screens.auth.*
import com.example.trudomix.ui.screens.settings.NotificationEventSettings
import com.example.trudomix.ui.theme.TrudomixTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MobilePhoneFlowTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun registerAndLoginUsingActualAndroidCodeNotifications(){
        val fixture=InstrumentationRegistry.getArguments().getString("phoneFixture")
        assumeTrue(fixture!=null)
        val base=fixture!!
        val previousUrl=ApiClient.baseUrl
        val tokenManager=ApiClient.getTokenManager()
        val previousToken=runBlocking{tokenManager.getToken()}
        val previousUser=com.example.trudomix.data.local.SessionIdentity.user.value
        val phase=mutableIntStateOf(0)
        val app=compose.activity.application
        val auth=AuthViewModel()
        try{
            runBlocking{ApiClient.setServer(base)}
            compose.setContent{TrudomixTheme{
                val state by auth.uiState.collectAsState()
                when(phase.intValue){
                    0->RegisterScreen(auth,{phase.intValue=1},{})
                    1->Text("Регистрация завершена")
                    2->{val phone=androidx.lifecycle.viewmodel.compose.viewModel<PhoneOtpViewModel>(key="e2e-login");LaunchedEffect(state){if(state is AuthUiState.Success)phase.intValue=3};PhoneLoginContent(auth,phone,{}, {})}
                    else->Text("Вход по телефону выполнен")
                }
            }}
            compose.onNodeWithText("Почта").performTextReplacement("mobile-otp@example.test")
            compose.onNodeWithText("Пароль").performScrollTo().performTextReplacement("Mobile-test-123")
            compose.onNodeWithText("Номер телефона").performScrollTo().performTextReplacement("+79990000001")
            compose.waitUntil(15000){compose.onAllNodesWithText("Получить код").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Получить код").performScrollTo().performClick()
            val code=notificationCode()
            compose.onNodeWithText("Код из уведомления").performScrollTo().performTextReplacement(if(code=="000000")"111111"else "000000")
            compose.onNodeWithText("Подтвердить код").performScrollTo().performClick()
            compose.waitUntil(10000){compose.onAllNodesWithText("Код неверный",substring=true).fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Зарегистрироваться").assertIsNotEnabled()
            compose.onNodeWithText("Код из уведомления").performScrollTo().performTextReplacement(code)
            compose.onNodeWithText("Подтвердить код").performScrollTo().performClick()
            compose.waitUntil(10000){compose.onAllNodesWithText("Код подтверждён. Можно продолжить").fetchSemanticsNodes().isNotEmpty()}
            screenshot("phone-registration-verified.png")
            compose.onNodeWithText("Зарегистрироваться").performScrollTo().performClick()
            compose.waitUntil(15000){phase.intValue==1}
            assertEquals("mobile-otp@example.test",com.example.trudomix.data.local.SessionIdentity.user.value?.email)
            runBlocking{withContext(Dispatchers.IO){val connection=URL(base.trimEnd('/')+"/test/resend-ready").openConnection() as HttpURLConnection;connection.requestMethod="POST";assertEquals(204,connection.responseCode);connection.disconnect()}}
            compose.runOnIdle{auth.resetState();phase.intValue=2}
            compose.onNodeWithText("Номер телефона").performTextReplacement("+79990000001")
            compose.waitUntil(10000){compose.onAllNodesWithText("Получить код").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Получить код").performScrollTo().performClick()
            val loginCode=notificationCode()
            compose.onNodeWithText("Код из уведомления").performScrollTo().performTextReplacement(loginCode)
            compose.onNodeWithText("Подтвердить код").performScrollTo().performClick()
            compose.waitUntil(15000){phase.intValue==3}
            compose.onNodeWithText("Вход по телефону выполнен").assertIsDisplayed()
        }finally{
            OtpNotifications.clear(app)
            runBlocking{ApiClient.setServer(previousUrl);if(previousToken.isNullOrBlank())tokenManager.clearSession()else tokenManager.saveToken(previousToken)}
            com.example.trudomix.data.local.SessionIdentity.user.value=previousUser
        }
    }
    @Test fun eventSwitchesPersistAndSuppressOnlyTheirOwnNotifications(){
        val context=compose.activity.applicationContext
        val manager=context.getSystemService(NotificationManager::class.java)
        val master=NotificationPreferences.isEnabled(context)
        val previous=NotificationEvent.entries.associateWith{NotificationPreferences.eventEnabled(context,it)}
        try{
            NotificationPreferences.setEnabled(context,true)
            NotificationPreferences.setEvent(context,NotificationEvent.RESUME_VIEWS,true)
            NotificationPreferences.setEvent(context,NotificationEvent.MESSAGES,true)
            val resume=mapOf("type" to "resume_viewed","resume_id" to "987654","viewer_user_id" to "998877")
            val message=mapOf("conversation_id" to "987654")
            assertTrue(AppNotifications.show(context,"Проверка просмотра","Тест",resume,"test-resume-event"))
            assertTrue(AppNotifications.show(context,"Проверка сообщения","Тест",message,"test-message-event"))
            compose.setContent{TrudomixTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){NotificationEventSettings(true)}}}
            compose.onNodeWithContentDescription("Уведомления: Просмотры резюме").performScrollTo().assertIsOn().performClick().assertIsOff()
            NotificationPreferences.initialize(context)
            assertFalse(NotificationPreferences.eventEnabled(context,NotificationEvent.RESUME_VIEWS))
            assertTrue(NotificationPreferences.eventEnabled(context,NotificationEvent.MESSAGES))
            assertFalse(AppNotifications.show(context,"Не должно появиться","Тест",resume,"test-resume-event"))
            assertFalse(manager.activeNotifications.any{it.notification.extras.getString("trudomix_event")==NotificationEvent.RESUME_VIEWS.name})
            assertTrue(manager.activeNotifications.any{it.notification.extras.getString("trudomix_event")==NotificationEvent.MESSAGES.name})
            screenshot("notification-event-switches.png")
        }finally{
            manager.cancel("resume-987654-998877".hashCode());manager.cancel("conversation-987654".hashCode())
            previous.forEach{(event,value)->NotificationPreferences.setEvent(context,event,value)}
            NotificationPreferences.setEnabled(context,master);AppNotifications.refreshSummary(context)
        }
    }
    private fun notificationCode():String{
        val manager=compose.activity.getSystemService(NotificationManager::class.java)
        var code:String?=null
        compose.waitUntil(15000){code=manager.activeNotifications.firstOrNull{it.id==OtpNotifications.ID}?.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.let{Regex("[0-9]{6}").find(it)?.value};code!=null}
        return code!!
    }
    private fun screenshot(name:String){val bitmap=compose.onRoot().captureToImage().asAndroidBitmap();File(compose.activity.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
}
