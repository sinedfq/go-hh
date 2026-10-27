package com.example.trudomix

import android.graphics.Bitmap
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class WebLoaderBrandTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Test fun criticalSequenceThemesSlowBackendAndReducedMotion(){
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveWeb")=="true")
        val instrumentation=InstrumentationRegistry.getInstrumentation();val web=AtomicReference<WebView>()
        val modulesBlocked=AtomicBoolean(true);val reduced=AtomicBoolean(false);val failure=AtomicBoolean(false)
        val companyReady=CountDownLatch(1);val reducedReady=CountDownLatch(1)
        val requested=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val url=InstrumentationRegistry.getArguments().getString("webUrl")?:"http://10.0.2.2:5174/"
        val user="""{"id":9999,"email":"preview@example.test","role":"candidate"}"""
        instrumentation.runOnMainSync{compose.activity.setContentView(WebView(compose.activity).apply{
            web.set(this);setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.cacheMode=WebSettings.LOAD_NO_CACHE
            webViewClient=object:WebViewClient(){override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse?{
                val path=request.url.path.orEmpty()
                if(path.startsWith("/assets/")&&path.endsWith(".js")){
                    requested.add(path)
                    if(modulesBlocked.get())return WebResourceResponse("application/javascript","UTF-8","".byteInputStream())
                }
                if(path=="/splash.js"&&(reduced.get()||modulesBlocked.get())){
                    val body=java.net.URL(url.trimEnd('/')+"/splash.js").readText()
                    val prefix=if(modulesBlocked.get())"""window.splashTestTime=0;Object.defineProperty(performance,'now',{value:()=>window.splashTestTime});"""else """const originalMatchMedia=window.matchMedia.bind(window);window.matchMedia=q=>q==='(prefers-reduced-motion: reduce)'?{matches:true,addEventListener(){},removeEventListener(){}}:originalMatchMedia(q);"""
                    return WebResourceResponse("application/javascript","UTF-8",(prefix+body).byteInputStream())
                }
                if(path=="/health")return WebResourceResponse("application/json","UTF-8","""{"status":"ok"}""".byteInputStream())
                if(!path.startsWith("/api/"))return null
                val body=when(path){
                    "/api/companies"->{if(reduced.get())reducedReady.await(20,TimeUnit.SECONDS)else companyReady.await(25,TimeUnit.SECONDS);"[]"}
                    "/api/me"->user
                    "/api/feed"->"""{"posts":[],"next_cursor":0}"""
                    "/api/vacancies/search"->"""{"vacancies":[],"total_count":0}"""
                    else->"[]"
                }
                if(path=="/api/companies"&&failure.get())return WebResourceResponse("application/json","UTF-8",500,"Internal Server Error",emptyMap(),"{}".byteInputStream())
                return WebResourceResponse("application/json","UTF-8",body.byteInputStream())
            }};loadUrl(url)
        })}
        fun js(script:String):String{val result=AtomicReference<String>();val latch=CountDownLatch(1);instrumentation.runOnMainSync{web.get().evaluateJavascript(script){result.set(it);latch.countDown()}};if(!latch.await(2,TimeUnit.SECONDS))return "null";return result.get()}
        fun waitFor(script:String){val deadline=System.currentTimeMillis()+25000;while(System.currentTimeMillis()<deadline){if(js(script)=="true")return;Thread.sleep(80)};error("Splash condition failed: $script; ${js("document.body.innerText.slice(0,1000)")}")}
        fun shot(name:String){Thread.sleep(160);val bitmap=instrumentation.uiAutomation.takeScreenshot();File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
        try{
            waitFor("!!window.TrudomixSplash")
            js("window.reloading=true;localStorage.setItem('theme','light');location.reload()")
            waitFor("!window.reloading&&!!window.TrudomixSplash&&window.TrudomixSplash.inspect().stage==='outline'")
            assertEquals("Critical splash needs no React bundle","true",js("!document.querySelector('.app')&&document.querySelectorAll('#startup-splash').length===1"))
            assertEquals("Twelve paths start outside four actual viewport corners","true",js("(()=>{const w=innerWidth,h=innerHeight,a=TrudomixSplashMath.trajectories(w,h),groups=[0,0,0,0];for(const l of a){const p=l.points[0];if(!(p.x<0||p.x>w)||!(p.y<0||p.y>h))return false;groups[(p.y>h?2:0)+(p.x>w?1:0)]++}return a.length===12&&groups.every(n=>n===3)})()"))
            js("window.splashTestTime=1800")
            waitFor("window.TrudomixSplash.inspect().elapsed>1.3");shot("web-splash-light-lines.png")
            js("window.splashTestTime=8000")
            waitFor("window.TrudomixSplash.inspect().elapsed>7.9")
            assertEquals("Name and impact follow the outline","true",js("TrudomixSplash.inspect().wordmark>0&&TrudomixSplash.inspect().ringAlpha>0"));shot("web-splash-light-wave.png")
            js("window.splashTestTime=9800")
            waitFor("window.TrudomixSplash.inspect().animationDone")
            assertEquals("Final frame holds until initialization","true",js("!!document.querySelector('#startup-splash')&&!TrudomixSplash.inspect().ready&&TrudomixSplash.inspect().ringAlpha===0"))
            modulesBlocked.set(false)
            js("window.reloading=true;localStorage.setItem('theme','dark');localStorage.setItem('user',JSON.stringify($user));localStorage.setItem('token','preview-token');sessionStorage.setItem('currentMode','companies');location.reload()")
            waitFor("!window.reloading&&!!window.TrudomixSplash&&document.documentElement.dataset.theme==='dark'")
            waitFor("TrudomixSplash.inspect().elapsed>4.1");shot("web-splash-dark-wave.png")
            waitFor("TrudomixSplash.inspect().animationDone")
            assertEquals("Slow backend holds the final logo: "+js("JSON.stringify(TrudomixSplash.inspect())")+requested,"true",js("!!document.querySelector('#startup-splash')&&!TrudomixSplash.inspect().ready&&TrudomixSplash.inspect().wordmark===1"))
            companyReady.countDown()
            waitFor("!document.querySelector('#startup-splash')&&!!document.querySelector('.companies-page')")
            assertEquals("Content becomes interactive","true",js("!document.querySelector('#root').hasAttribute('inert')"))
            assertTrue("Active route split was loaded",requested.any{it.contains("CompaniesPage-")})
            assertFalse("Unused Markdown stays lazy",requested.any{it.contains("MarkdownContent-")})
            reduced.set(true)
            js("window.reloading=true;location.reload()")
            waitFor("!window.reloading&&!!window.TrudomixSplash&&TrudomixSplash.inspect().reduced")
            assertEquals("Reduced motion immediately shows final logo and name","true",js("TrudomixSplash.inspect().animationDone&&TrudomixSplash.inspect().wordmark===1&&TrudomixSplash.inspect().lines===0"))
            reducedReady.countDown();waitFor("!document.querySelector('#startup-splash')")
            failure.set(true);js("window.reloading=true;location.reload()")
            waitFor("!window.reloading&&!!document.querySelector('.startup-error')")
            assertEquals("HTTP 500 is a server error, not offline","true",js("TrudomixSplash.inspect().error==='SERVER'&&document.querySelector('.startup-wordmark').style.opacity==='1'"))
            failure.set(false);js("document.querySelector('.startup-error button').click()")
            waitFor("!document.querySelector('#startup-splash')&&!!document.querySelector('.companies-page')")
        }finally{companyReady.countDown();reducedReady.countDown();instrumentation.runOnMainSync{web.get().destroy()}}
    }
}
