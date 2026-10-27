package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.startup.StartupReadiness
import com.example.trudomix.ui.loading.TrudomixLoadingScreen
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class LoaderBrandTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun sharedLoaderSupportsBothThemesAndLeavesWhenReady(){
        val original=StartupReadiness.visible.value
        StartupReadiness.visible.value=false
        val dark=mutableStateOf(false);val loading=mutableStateOf(true)
        try{
            compose.mainClock.autoAdvance=false
            compose.setContent{TrudomixTheme(darkTheme=dark.value){if(loading.value)TrudomixLoadingScreen()else Text("Готово")}}
            for(theme in listOf(false,true)){
                compose.runOnIdle{dark.value=theme};compose.mainClock.advanceTimeBy(200)
                compose.onNodeWithContentDescription("Загрузка").assertIsDisplayed()
                compose.onNodeWithText("Загружаем…").assertIsDisplayed()
                val image=compose.onRoot().captureToImage().asAndroidBitmap()
                File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"loader-${if(theme)"dark"else "light"}.png").outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
            }
            compose.runOnIdle{loading.value=false};compose.mainClock.advanceTimeBy(32)
            compose.onNodeWithContentDescription("Загрузка").assertDoesNotExist()
            compose.onNodeWithText("Готово").assertIsDisplayed()
        }finally{StartupReadiness.visible.value=original}
    }
}
