package com.example.trudomix

import android.os.SystemClock
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.ui.loading.SplashScreen
import com.example.trudomix.ui.loading.SplashTiming
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real Choreographer clock, separate from the Compose test clock and app data. */
class SplashRuntimeTest{
    @Test fun realClockFinishesAndStopsSchedulingFrames(){
        val done=CountDownLatch(1);val frames=AtomicInteger(0)
        val stages=java.util.concurrent.atomic.AtomicIntegerArray(4)
        val scenario=ActivityScenario.launch(ComponentActivity::class.java)
        try{
            scenario.onActivity{activity->activity.enableEdgeToEdge();activity.setContent{
                val elapsed=remember{mutableFloatStateOf(0f)}
                LaunchedEffect(Unit){
                    val start=SystemClock.elapsedRealtime()
                    while(elapsed.floatValue<SplashTiming.duration){withFrameNanos{};frames.incrementAndGet();elapsed.floatValue=((SystemClock.elapsedRealtime()-start)/1000f).coerceAtMost(SplashTiming.duration);stages.incrementAndGet(when{elapsed.floatValue<1.3f->0;elapsed.floatValue<2.2f->1;elapsed.floatValue<2.8f->2;else->3})}
                    done.countDown()
                }
                SplashScreen(true,elapsed)
            }}
            assertTrue("Finite animation finishes on the real frame clock",done.await(20,TimeUnit.SECONDS))
            val finalFrames=frames.get();Thread.sleep(300)
            assertEquals("No animation frames after the final state",finalFrames,frames.get())
            InstrumentationRegistry.getInstrumentation().sendStatus(0,Bundle().apply{putString("stream","\nSplash isolated real-clock frames: $finalFrames over the selected animation duration; stages=${(0..3).map{stages.get(it)}}\n")})
        }finally{scenario.close()}
    }
}
