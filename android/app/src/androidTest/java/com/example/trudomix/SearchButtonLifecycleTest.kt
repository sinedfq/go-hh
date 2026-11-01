package com.example.trudomix

import android.app.ActivityManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.example.trudomix.data.local.VisualPerformance
import com.example.trudomix.ui.components.DotSearchButton
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SearchButtonLifecycleTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun optionalWaveStopsInBackgroundAndClickDispatchesImmediately(){
        val context=compose.activity
        val manager=context.getSystemService(ActivityManager::class.java)
        assumeTrue(!manager.isLowRamDevice&&manager.memoryClass>=192)
        val previous=VisualPerformance.searchMotion.value
        val frames=AtomicInteger();var clicks=0
        val thread=HandlerThread("button-metrics").apply{start()}
        val listener=Window.OnFrameMetricsAvailableListener{_,_,_->frames.incrementAndGet()}
        try{
            compose.mainClock.autoAdvance=false
            VisualPerformance.setSearchMotion(context,true)
            compose.setContent{TrudomixTheme{Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){DotSearchButton({clicks++})}}}
            compose.mainClock.advanceTimeBy(64)
            val image=compose.onNodeWithContentDescription("Поиск").captureToImage().asAndroidBitmap()
            java.io.File(context.getExternalFilesDir(null),"dot-search-button.png").outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            image.recycle()
            compose.activityRule.scenario.onActivity{it.window.addOnFrameMetricsAvailableListener(listener,Handler(thread.looper))}
            compose.onNodeWithContentDescription("Поиск").assertHasClickAction().performClick()
            compose.runOnIdle{assertEquals(1,clicks)}
            val initial=frames.get()
            compose.mainClock.advanceTimeBy(1000)
            compose.waitUntil(5000){frames.get()>initial}
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(250)
            val stopped=frames.get()
            Thread.sleep(500)
            assertTrue("Stopped Activity must not keep drawing",frames.get()-stopped<=1)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.mainClock.advanceTimeBy(1000)
            compose.waitUntil(5000){frames.get()>stopped+1}
        }finally{
            compose.activityRule.scenario.onActivity{it.window.removeOnFrameMetricsAvailableListener(listener)}
            VisualPerformance.setSearchMotion(context,previous)
            thread.quitSafely();thread.join(3000)
        }
    }
}
