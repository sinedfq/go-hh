package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.ui.screens.auth.*
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OtpPresentationTest{
 @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
 @Test fun pastedCodeIsFourDigitsAndSuccessDoesNotChangeTheCode(){
  var code by mutableStateOf("")
  var checking by mutableStateOf(false)
  var verified by mutableStateOf(false)
  compose.setContent{TrudomixTheme{OtpCodeField(code,{code=it},"Одноразовый код",verified,!checking,checking)}}
  compose.onNodeWithContentDescription("Одноразовый код").performTextInput("0 1-2345")
  compose.runOnIdle{assertEquals("0123",code)}
  compose.mainClock.autoAdvance=false
  compose.runOnUiThread{checking=true}
  compose.mainClock.advanceTimeBy(350)
  compose.runOnUiThread{checking=false;verified=true}
  compose.mainClock.autoAdvance=true
  compose.onNodeWithContentDescription("Одноразовый код").assertTextEquals("0123").assertIsNotEnabled()
  shot("otp-four-success.png")
  compose.runOnIdle{assertEquals("0123",code)}
 }
 @Test fun loginActionsAreVisibleWithoutScrolling(){
  compose.setContent{TrudomixTheme{LoginContent(AuthUiState.Idle,{_,_->},{},onPhoneLogin={})}}
  compose.onNodeWithText("Войти").assertIsDisplayed()
  compose.onNodeWithText("Создать аккаунт").assertIsDisplayed()
  shot("compact-login14.png")
 }
 private fun shot(name:String){
  val bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
  java.io.File(compose.activity.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  bitmap.recycle()
 }
}
