package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.startup.*
import com.example.trudomix.ui.loading.*
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class SplashNetworkStateTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun failureKeepsBrandControlsAndRecoveryDoesNotReplay(){
        val original=StartupReadiness.visible.value;val previousFailure=StartupReadiness.failure.value;val previousReady=StartupReadiness.ready.value
        val model=SplashViewModel().apply{elapsed.floatValue=com.example.trudomix.ui.loading.SplashTiming.duration;animationDone=true}
        val dark=mutableStateOf(false)
        try{
            StartupReadiness.visible.value=true;StartupReadiness.ready.value=false;StartupReadiness.failure.value=NetworkIssue.from(offline=true)
            compose.setContent{TrudomixTheme(darkTheme=dark.value){StartupSplashHost(dark.value,compose.activity.window,model)}}
            for(theme in listOf(false,true)){
                compose.runOnIdle{dark.value=theme}
                compose.onNodeWithText("ТРУДОМИКС").assertIsDisplayed()
                compose.onNodeWithText("Нет подключения к интернету").assertIsDisplayed()
                compose.onNodeWithText("Повторить подключение").assertIsDisplayed().assertIsEnabled()
                val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
                File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"native-offline-${if(theme)"dark"else "light"}.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            }
            compose.runOnIdle{StartupReadiness.busy.value=true}
            compose.onNodeWithText("Подключение…").assertIsNotEnabled()
            compose.runOnIdle{StartupReadiness.busy.value=false;StartupReadiness.failure.value=null;StartupReadiness.ready.value=true}
            compose.waitUntil(4000){model.closed}
            assertEquals(com.example.trudomix.ui.loading.SplashTiming.duration,model.elapsed.floatValue,0f)
            compose.onNodeWithText("Повторить подключение").assertDoesNotExist()
        }finally{StartupReadiness.visible.value=original;StartupReadiness.failure.value=previousFailure;StartupReadiness.ready.value=previousReady;StartupReadiness.busy.value=false}
    }
}
