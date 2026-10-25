package com.example.trudomix

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.local.AppIcons
import com.example.trudomix.ui.loading.TrudomixLoadingScreen
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LoaderBrandTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun loaderUsesSelectionAndThemeAndLeavesWhenReady(){
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val original=AppIcons.chosen(context);val originalFollow=AppIcons.appearance.value.followTheme
        val originalDark=context.getSharedPreferences("app_icon",0).getBoolean("last_dark",false)
        val selected=AppIcons.choices.first{it.design==3&&it.color=="black"}
        val dark=mutableStateOf(false);val loading=mutableStateOf(true)
        try{
            AppIcons.followTheme(context,true);AppIcons.select(context,selected);AppIcons.applyTheme(context,false)
            compose.mainClock.autoAdvance=false
            compose.setContent{TrudomixTheme(darkTheme=dark.value){if(loading.value)TrudomixLoadingScreen()else Text("Готово")}}
            compose.mainClock.advanceTimeBy(200)
            val light=AppIcons.choices.first{it.design==3&&it.color=="white"}
            compose.onNodeWithContentDescription("Логотип Трудомикс").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,light.title))
            val first=compose.onNodeWithContentDescription("Логотип Трудомикс").fetchSemanticsNode().boundsInRoot.width
            compose.mainClock.advanceTimeBy(1000)
            val second=compose.onNodeWithContentDescription("Логотип Трудомикс").fetchSemanticsNode().boundsInRoot.width
            assertTrue("Logo pulse changes its visible size",kotlin.math.abs(first-second)>.5f)
            screenshot("loader-light.png")
            compose.runOnIdle{dark.value=true;AppIcons.applyTheme(context,true)}
            compose.mainClock.advanceTimeBy(32)
            compose.onNodeWithContentDescription("Логотип Трудомикс").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,selected.title))
            @Suppress("DEPRECATION") val launchers=context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName),0)
            assertEquals(1,launchers.size)
            assertEquals("${context.packageName}.Launcher${selected.number}",launchers.single().activityInfo.name)
            assertEquals(selected,AppIcons.chosen(context))
            screenshot("loader-dark.png")
            compose.runOnIdle{loading.value=false};compose.mainClock.advanceTimeBy(32)
            compose.onNodeWithContentDescription("Логотип Трудомикс").assertDoesNotExist()
            compose.onNodeWithText("Готово").assertIsDisplayed()
        }finally{AppIcons.select(context,original);AppIcons.followTheme(context,originalFollow);AppIcons.applyTheme(context,originalDark)}
    }
    private fun screenshot(name:String){compose.waitForIdle();val instrumentation=InstrumentationRegistry.getInstrumentation();instrumentation.waitForIdleSync();Thread.sleep(250);val bitmap=instrumentation.uiAutomation.takeScreenshot();File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
}
