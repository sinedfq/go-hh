package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.local.LoadingStyle
import com.example.trudomix.data.local.LoadingStyles
import com.example.trudomix.ui.components.RefreshContent
import com.example.trudomix.ui.loading.SplashScreen
import com.example.trudomix.ui.loading.SplashTiming
import com.example.trudomix.ui.screens.settings.LoadingStylePicker
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LoadingStyleSettingsTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun selectionSurvivesReloadAndCanBePreviewed(){
        val context=compose.activity.applicationContext
        val original=LoadingStyles.style.value
        try{
            compose.setContent{TrudomixTheme{LoadingStylePicker()}}
            for(style in LoadingStyle.entries){
                compose.onNodeWithText(style.title).performClick()
                compose.runOnIdle{assertEquals(style,LoadingStyles.style.value);LoadingStyles.initialize(context);assertEquals(style,LoadingStyles.style.value)}
                compose.onNodeWithText("Повторить анимацию").performClick()
            }
        }finally{LoadingStyles.select(context,original)}
    }
    @Test fun threeStylesRenderInBothThemesAndReachFinalState(){
        val time=mutableFloatStateOf(1f);val style=mutableStateOf(LoadingStyle.IMPACT);val dark=mutableStateOf(false)
        compose.setContent{TrudomixTheme(darkTheme=dark.value){SplashScreen(dark.value,time,style=style.value)}}
        for(theme in listOf(false,true))for(choice in LoadingStyle.entries){
            compose.runOnIdle{dark.value=theme;style.value=choice;time.floatValue=1f}
            screenshot("loading-${choice.id}-${if(theme)"dark"else "light"}-middle.png")
            compose.runOnIdle{time.floatValue=SplashTiming.duration}
            compose.onNodeWithText("ТРУДОМИКС").assertIsDisplayed()
            screenshot("loading-${choice.id}-${if(theme)"dark"else "light"}-final.png")
        }
    }
    @Test fun pullGestureRefreshesOnceAndShowsBrandedIndicator(){
        var calls=0;val refreshing=mutableStateOf(false)
        compose.setContent{TrudomixTheme{RefreshContent(refreshing.value,{calls++;refreshing.value=true}){
            LazyColumn(Modifier.fillMaxSize().testTag("refresh-list")){items(30){Text("Вакансия $it",Modifier.fillMaxWidth().height(64.dp))}}
        }}}
        compose.onNodeWithTag("refresh-list").performTouchInput{swipeDown()}
        compose.onNodeWithContentDescription("Обновление").assertIsDisplayed()
        compose.runOnIdle{assertEquals(1,calls)}
        compose.mainClock.advanceTimeBy(350)
        screenshot("refresh-branded.png")
        compose.runOnIdle{refreshing.value=false}
        compose.onNodeWithContentDescription("Обновление").assertDoesNotExist()
    }
    private fun screenshot(name:String){
        compose.waitForIdle()
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
}
