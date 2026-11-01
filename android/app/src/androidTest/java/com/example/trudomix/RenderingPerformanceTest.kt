package com.example.trudomix

import android.os.*
import android.view.FrameMetrics
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.ui.components.GlassNavigation
import com.example.trudomix.ui.screens.vacancies.*
import com.example.trudomix.ui.theme.TrudomixTheme
import dev.chrisbanes.haze.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Collections

/** Repeatable renderer workload, deliberately without network or image decoding. Not a Macrobenchmark. */
class RenderingPerformanceTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun scrollVacancyFeed(){
        val args=InstrumentationRegistry.getArguments()
        val run=args.getString("perfRun")?:"manual"
        val originalMotion=com.example.trudomix.data.local.VisualPerformance.searchMotion.value
        com.example.trudomix.data.local.VisualPerformance.setSearchMotion(compose.activity,args.getString("searchMotion")=="true")
        val frames=Collections.synchronizedList(mutableListOf<Long>())
        val thread=HandlerThread("frame-metrics").apply{start()}
        val listener=android.view.Window.OnFrameMetricsAvailableListener{_,metrics,_->frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))}
        val jobs=List(120){i->Vacancy(id=i+1,title="Android-разработчик $i",company="Trudomix $i",location="Москва",experience="Middle",skills=listOf("Kotlin","Compose","SQL"),salary_from=150000,salary_to=230000)}
        compose.setContent{TrudomixTheme{
            val haze=remember{HazeState()}
            Box(Modifier.fillMaxSize()){
                Box(Modifier.fillMaxSize().testTag("perf-feed")){
                    VacancyFeed(VacancyListUiState.Success(jobs),"",false,jobs.size,false,null,{},{},{},{},{},{},{_,_->})
                }
                GlassNavigation(haze,0,2,{},{},Modifier.align(Alignment.BottomCenter))
            }
        }}
        compose.waitForIdle()
        // Warm text, card composition, shaders and scrolling before recording.
        repeat(2){compose.onNodeWithTag("perf-feed").performTouchInput{swipeUp(durationMillis=350)}}
        compose.activityRule.scenario.onActivity{it.window.addOnFrameMetricsAvailableListener(listener,Handler(thread.looper))}
        val started=SystemClock.elapsedRealtimeNanos();val cpu=Process.getElapsedCpuTime()
        try{
            repeat(12){index->compose.onNodeWithTag("perf-feed").performTouchInput{if(index%4==3)swipeDown(durationMillis=400)else swipeUp(durationMillis=400)}}
            compose.waitForIdle()
        }finally{
            val elapsed=SystemClock.elapsedRealtimeNanos()-started
            val cpuMs=Process.getElapsedCpuTime()-cpu
            compose.activityRule.scenario.onActivity{it.window.removeOnFrameMetricsAvailableListener(listener)}
            thread.quitSafely();thread.join(3000)
            val memory=Debug.MemoryInfo();Debug.getMemoryInfo(memory)
            val samples=synchronized(frames){frames.toList().sorted()}
            fun percentile(p:Double)=if(samples.isEmpty())0.0 else samples[((samples.size-1)*p).toInt()]/1e6
            val missed=samples.count{it>16_666_667L}
            val json="""{"run":"$run","build":${BuildConfig.VERSION_CODE},"scenario":"120 local vacancies; 12 alternating 400ms swipes; no network/images","frames":${samples.size},"over16ms":$missed,"p50ms":${percentile(.5)},"p90ms":${percentile(.9)},"p95ms":${percentile(.95)},"maxMs":${percentile(1.0)},"wallMs":${elapsed/1e6},"cpuMs":$cpuMs,"pssKb":${memory.totalPss},"javaHeapBytes":${Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()}}"""
            File(compose.activity.getExternalFilesDir(null),"render-performance-$run.json").writeText(json)
            println("RENDER_PERFORMANCE $json")
            com.example.trudomix.data.local.VisualPerformance.setSearchMotion(compose.activity,originalMotion)
        }
    }
}
