package com.example.trudomix

import android.graphics.Bitmap
import android.graphics.Canvas
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
import java.util.concurrent.atomic.*

class WebStartupNetworkTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Test fun offlineTimeoutServerRecoveryAndRuntimeLoss(){
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveWeb")=="true")
        val instrumentation=InstrumentationRegistry.getInstrumentation();val web=AtomicReference<WebView>()
        val mode=AtomicReference("offline");val reduced=AtomicBoolean(false);val probes=AtomicInteger()
        val url=InstrumentationRegistry.getArguments().getString("webUrl")?:"http://10.0.2.2:5174/"
        instrumentation.runOnMainSync{compose.activity.setContentView(WebView(compose.activity).apply{
            web.set(this);setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.cacheMode=WebSettings.LOAD_NO_CACHE;settings.useWideViewPort=false
            webViewClient=object:WebViewClient(){override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse?{
                val path=request.url.path.orEmpty()
                if(path=="/splash.js"){
                    val motion=if(reduced.get())"const media=matchMedia.bind(window);window.matchMedia=q=>q==='(prefers-reduced-motion: reduce)'?{matches:true,addEventListener(){},removeEventListener(){}}:media(q);"else ""
                    // Exercise Axios' real browser adapter for transport failures and timeouts.
                    val fixture="""window.fixtureMode='${mode.get()}';Object.defineProperty(navigator,'onLine',{get:()=>fixtureMode!=='offline'});const xhrOpen=XMLHttpRequest.prototype.open,xhrSend=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.open=function(...args){this.fixtureUrl=args[1];return xhrOpen.apply(this,args)};XMLHttpRequest.prototype.send=function(...args){if(/\/(api\/|health)/.test(this.fixtureUrl)&&['offline','unavailable','timeout'].includes(fixtureMode)){setTimeout(()=>this.dispatchEvent(new ProgressEvent(fixtureMode==='timeout'?'timeout':'error')),100);return}return xhrSend.apply(this,args)};"""
                    return WebResourceResponse("application/javascript","UTF-8",(motion+fixture+java.net.URL(url.trimEnd('/')+"/splash.js").readText()).byteInputStream())
                }
                if(path!="/health"&&!path.startsWith("/api/"))return null
                if(path=="/health")probes.incrementAndGet()
                if(mode.get()=="server")return WebResourceResponse("application/json","UTF-8",503,"Service Unavailable",emptyMap(),"{}".byteInputStream())
                val body=when(path){"/health"->"""{"status":"ok"}""";"/api/feed"->"""{"posts":[],"next_cursor":0}""";"/api/vacancies/search"->"""{"vacancies":[],"total_count":0}""";else->"[]"}
                return WebResourceResponse("application/json","UTF-8",body.byteInputStream())
            }};loadUrl(url)
        })}
        fun js(script:String):String{val result=AtomicReference<String>();val latch=CountDownLatch(1);instrumentation.runOnMainSync{web.get().evaluateJavascript(script){result.set(it);latch.countDown()}};if(!latch.await(2,TimeUnit.SECONDS))return "null";return result.get()}
        fun waitFor(script:String){val until=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<until){if(js(script)=="true")return;Thread.sleep(100)};error("Network scenario: $script; ${js("JSON.stringify(window.TrudomixSplash?.inspect())")}")}
        fun shot(name:String){instrumentation.runOnMainSync{val v=web.get();val bitmap=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap));File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}}
        try{
            waitFor("!!window.TrudomixSplash&&!!document.querySelector('.top-header')")
            js("window.networkReload=true;localStorage.removeItem('user');localStorage.removeItem('token');sessionStorage.setItem('currentMode','companies');location.reload()")
            waitFor("!window.networkReload&&!!window.TrudomixSplash&&TrudomixSplash.inspect().elapsed<5.5")
            assertEquals("Error waits until animation ends","false",js("!!document.querySelector('.startup-error')"))
            waitFor("!!document.querySelector('.startup-error')")
            assertEquals("Offline is explicit","true",js("TrudomixSplash.inspect().state==='OFFLINE'&&TrudomixSplash.inspect().elapsed===9.6&&document.querySelector('.startup-wordmark').style.opacity==='1'"))
            for(theme in listOf("light","dark")){
                instrumentation.runOnMainSync{val v=web.get();v.layoutParams=v.layoutParams.apply{width=kotlin.math.round(360*v.resources.displayMetrics.density).toInt()}}
                js("document.documentElement.dataset.theme='$theme'")
                waitFor("Math.abs(innerWidth-360)<=1")
                assertEquals("Error is below the wordmark","true",js("document.querySelector('.startup-error').getBoundingClientRect().top>document.querySelector('.startup-wordmark').getBoundingClientRect().bottom&&document.querySelector('.startup-error button').getBoundingClientRect().bottom<innerHeight&&document.documentElement.scrollWidth<=innerWidth+1"))
                Thread.sleep(150);shot("web-offline-$theme-360.png")
            }
            // A failed probe keeps the brand and controls. Then one successful probe remounts data only.
            mode.set("server");js("fixtureMode='server';for(let i=0;i<12;i++)document.querySelector('.startup-error button').click()")
            waitFor("TrudomixSplash.inspect().error==='SERVER'&&!document.querySelector('.startup-error button').disabled")
            assertEquals(1,probes.get())
            mode.set("healthy");val before=System.currentTimeMillis()
            js("fixtureMode='healthy';document.querySelector('.startup-error button').click()")
            waitFor("!document.querySelector('#startup-splash')&&!!document.querySelector('.companies-page')")
            assertTrue("Retry must not replay 9.6 seconds",System.currentTimeMillis()-before<6000)
            js("window.dispatchEvent(new Event('offline'))")
            waitFor("!!document.querySelector('.connection-banner')")
            assertEquals("Runtime loss keeps the active route","true",js("!!document.querySelector('.companies-page')&&!document.querySelector('#startup-splash')"))
            js("document.querySelector('.connection-banner button').click()")
            waitFor("!document.querySelector('.connection-banner')")
            reduced.set(true)
            for((scenario,kind) in listOf("unavailable" to "UNAVAILABLE","timeout" to "TIMEOUT","server" to "SERVER")){
                mode.set(scenario);js("window.networkReload=true;location.reload()")
                waitFor("!window.networkReload&&!!document.querySelector('.startup-error')")
                assertEquals("$scenario classification","true",js("TrudomixSplash.inspect().reduced&&TrudomixSplash.inspect().elapsed===9.6&&TrudomixSplash.inspect().error==='$kind'"))
                assertEquals("Final logo stays visible","true",js("document.querySelector('.startup-logo').style.opacity==='1'&&document.querySelector('.startup-wordmark').style.opacity==='1'"))
            }
        }finally{instrumentation.runOnMainSync{web.get().destroy()}}
    }
}
