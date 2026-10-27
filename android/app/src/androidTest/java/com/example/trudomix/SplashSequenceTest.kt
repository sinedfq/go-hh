package com.example.trudomix

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.ui.loading.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class SplashSequenceTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun bothThemesShowTheSpecifiedStagesWithoutRepeating(){
        val time=mutableFloatStateOf(0f);val dark=mutableStateOf(false)
        compose.setContent{SplashScreen(dark.value,time)}
        for(isDark in listOf(false,true)){
            compose.runOnIdle{dark.value=isDark;time.floatValue=.9f}
            stage("outline");screenshot("splash-${if(isDark)"dark"else "light"}-lines.png")
            compose.runOnIdle{time.floatValue=1.1f};stage("outline")
            compose.runOnIdle{time.floatValue=2.5f};stage("wave");screenshot("splash-${if(isDark)"dark"else "light"}-wave.png")
            compose.runOnIdle{time.floatValue=2.7f};stage("wave")
            compose.runOnIdle{time.floatValue=3f};stage("wordmark")
            compose.runOnIdle{time.floatValue=SplashTiming.duration};stage("ready");screenshot("splash-${if(isDark)"dark"else "light"}-final.png")
            compose.runOnIdle{time.floatValue=20f};stage("ready")
        }
    }
    @Test fun reducedMotionShowsFinalStateImmediately(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        fun shell(command:String)=instrumentation.uiAutomation.executeShellCommand(command).use{android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText()}
        val original=shell("settings get global animator_duration_scale").trim()
        val model=SplashViewModel()
        try{
            shell("settings put global animator_duration_scale 0")
            compose.setContent{StartupSplashHost(false,compose.activity.window,model)}
            val start=SystemClock.elapsedRealtime()
            compose.waitUntil(2500){model.animationDone}
            assertEquals(SplashTiming.duration,model.elapsed.floatValue,0f)
            assertTrue(SystemClock.elapsedRealtime()-start<2500)
        }finally{shell(if(original=="null")"settings delete global animator_duration_scale"else "settings put global animator_duration_scale $original")}
    }
    private fun stage(value:String){compose.onNodeWithContentDescription("Загрузка Трудомикс").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,value))}
    private fun screenshot(name:String){
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation();val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
}
