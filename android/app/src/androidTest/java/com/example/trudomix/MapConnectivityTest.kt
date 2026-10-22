package com.example.trudomix

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import com.example.trudomix.ui.theme.TrudomixTheme
import androidx.activity.ComponentActivity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.ui.components.YandexVacancyMap
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in check against the real provider, excluded from ordinary offline UI tests. */
class MapConnectivityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun liveYandexMapLoads() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveMaps") == "true")
        val opened = java.util.concurrent.atomic.AtomicInteger(0)
        compose.setContent {
            TrudomixTheme(darkTheme=InstrumentationRegistry.getArguments().getString("darkMap")=="true") {
                com.example.trudomix.ui.screens.map.VacancyMapContent(listOf(Vacancy(id=1, title="Android-разработчик",company="Команда",location="Новосибирск", latitude=55.03, longitude=82.92)),onVacancy={opened.set(it)})
            }
        }
        // Allow the real JS API and map tiles to load, then retain a screenshot for visual review.
        Thread.sleep(InstrumentationRegistry.getArguments().getString("mapWaitMs")?.toLong() ?: 30000)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference<String>()
        val evaluated = CountDownLatch(1)
        fun webView(view: View): WebView? {
            if (view is WebView) return view
            if (view is ViewGroup) for (index in 0 until view.childCount) webView(view.getChildAt(index))?.let { return it }
            return null
        }
        instrumentation.runOnMainSync {
            val web = webView(compose.activity.window.decorView) ?: error("Map WebView is missing")
            web.evaluateJavascript("JSON.stringify({ready:window.ready,api:typeof window.ymaps,state:document.readyState,width:innerWidth,height:innerHeight,mapHeight:document.getElementById('map').getBoundingClientRect().height,style:document.querySelector('style').textContent,root:document.getElementById('map').outerHTML.slice(0,1200),computed:JSON.stringify({display:getComputedStyle(document.getElementById('map')).display,height:getComputedStyle(document.getElementById('map')).height,position:getComputedStyle(document.getElementById('map')).position}),html:document.documentElement.outerHTML.slice(-2000)})") {
                result.set(it); evaluated.countDown()
            }
        }
        assertTrue("WebView did not evaluate page state", evaluated.await(10, TimeUnit.SECONDS))
        File(instrumentation.targetContext.getExternalFilesDir(null), "map-state.json").writeText(result.get())
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "map-smoke.png")
        file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
        assertTrue("Map did not initialize: ${result.get()}", result.get().contains("\\\"ready\\\":true"))
        val state = org.json.JSONObject(org.json.JSONTokener(result.get()).nextValue() as String)
        assertTrue("Map container has no height", state.getDouble("mapHeight") >= state.getDouble("height") * 0.9)
        val themed=CountDownLatch(1)
        val filter=AtomicReference<String>()
        instrumentation.runOnMainSync {
            webView(compose.activity.window.decorView)!!.evaluateJavascript("window.jobMap.panes.get('ground').getElement().style.filter"){filter.set(it);themed.countDown()}
        }
        assertTrue(themed.await(5,TimeUnit.SECONDS))
        if(InstrumentationRegistry.getArguments().getString("darkMap")=="true")assertTrue(filter.get().contains("invert"))else assertTrue(filter.get().contains("none"))
        val markerPosition=AtomicReference<String>()
        val markerEvaluated=CountDownLatch(1)
        val location=IntArray(2)
        var scale=1f
        instrumentation.runOnMainSync {
            val web=webView(compose.activity.window.decorView)!!
            web.getLocationOnScreen(location);scale=web.width/state.getDouble("width").toFloat()
            web.evaluateJavascript("(function(){var r=document.querySelector('.job-pin').getBoundingClientRect();return JSON.stringify({x:r.left+r.width/2,y:r.top+r.height/2});})()") {markerPosition.set(it);markerEvaluated.countDown()}
        }
        assertTrue(markerEvaluated.await(5,TimeUnit.SECONDS))
        val point=org.json.JSONObject(org.json.JSONTokener(markerPosition.get()).nextValue() as String)
        val x=location[0]+point.getDouble("x").toFloat()*scale
        val y=location[1]+point.getDouble("y").toFloat()*scale
        val time=android.os.SystemClock.uptimeMillis()
        for(action in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP)) {
            val event=android.view.MotionEvent.obtain(time,android.os.SystemClock.uptimeMillis(),action,x,y,0)
            event.source=android.view.InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.uiAutomation.injectInputEvent(event,true);event.recycle();Thread.sleep(50)
        }
        compose.waitUntil(5000){compose.onAllNodesWithText("Посмотреть вакансию").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Посмотреть вакансию").performClick()
        compose.runOnIdle{org.junit.Assert.assertEquals(1,opened.get())}
    }
}
