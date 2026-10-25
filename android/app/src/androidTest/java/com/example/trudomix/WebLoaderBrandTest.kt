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
    @Test fun startupAndRequestLoadersFollowSelectionAndTheme(){
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveWeb")=="true")
        val instrumentation=InstrumentationRegistry.getInstrumentation();val web=AtomicReference<WebView>()
        val modulesBlocked=AtomicBoolean(true);val companyReady=CountDownLatch(1)
        val url=InstrumentationRegistry.getArguments().getString("webUrl")?:"http://10.0.2.2:5174/"
        val user="""{"id":9999,"email":"preview@example.test","role":"candidate"}"""
        instrumentation.runOnMainSync{compose.activity.setContentView(WebView(compose.activity).apply{
            web.set(this);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.cacheMode=WebSettings.LOAD_NO_CACHE
            webViewClient=object:WebViewClient(){override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse?{
                val path=request.url.path.orEmpty()
                if(modulesBlocked.get()&&path.startsWith("/assets/")&&path.endsWith(".js"))return WebResourceResponse("application/javascript","UTF-8","".byteInputStream())
                if(!path.startsWith("/api/"))return null
                val body=when(path){
                    "/api/companies"->{companyReady.await(18,TimeUnit.SECONDS);"[]"}
                    "/api/me"->user
                    "/api/feed"->"""{"posts":[],"next_cursor":0}"""
                    "/api/vacancies/search"->"""{"vacancies":[],"total_count":0}"""
                    else->"[]"
                };return WebResourceResponse("application/json","UTF-8",body.byteInputStream())
            }};loadUrl(url)
        })}
        fun js(script:String):String{val result=AtomicReference<String>();val latch=CountDownLatch(1);instrumentation.runOnMainSync{web.get().evaluateJavascript(script){result.set(it);latch.countDown()}};assertTrue(latch.await(10,TimeUnit.SECONDS));return result.get()}
        fun waitFor(script:String){val deadline=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<deadline){if(js(script)=="true")return;Thread.sleep(100)};error("Loader condition failed: $script; ${js("document.body.innerText.slice(0,1000)")}")}
        fun screenshot(name:String){Thread.sleep(300);val bitmap=instrumentation.uiAutomation.takeScreenshot();File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
        try{
            waitFor("!!document.querySelector('#root .trudomix-loader')")
            js("window.previewReloading=true;localStorage.setItem('theme','dark');localStorage.setItem('trudomix-brand',JSON.stringify({design:3,color:'auto'}));localStorage.setItem('trudomix-loader-settings',JSON.stringify({variant:'1',color:'mint'}));localStorage.setItem('user',JSON.stringify($user));localStorage.setItem('token','preview-token');sessionStorage.setItem('currentMode','companies');location.reload()")
            waitFor("!window.previewReloading&&document.querySelector('.trudomix-loader-logo').src.endsWith('icon_03_black.png')")
            assertEquals("Bootstrap respects the real theme","true",js("document.documentElement.dataset.theme==='dark'"))
            screenshot("web-loader-bootstrap.png")
            modulesBlocked.set(false)
            js("window.previewReloading=true;location.reload()")
            waitFor("!window.previewReloading&&!!document.querySelector('.companies-page .trudomix-loader')")
            assertEquals("Request loader keeps selection","true",js("document.querySelector('.companies-page .trudomix-loader-logo').src.endsWith('icon_03_black.png')"))
            screenshot("web-loader-request.png")
            companyReady.countDown()
            waitFor("!document.querySelector('.trudomix-loader')&&document.body.innerText.includes('Компаний пока нет')")
            js("document.querySelector('.brand-picker-button').click()")
            waitFor("!!document.querySelector('#brand-design')")
            js("document.querySelector('#brand-design').click()")
            waitFor("!!document.querySelector('#brand-design-options')")
            js("[...document.querySelectorAll('#brand-design-options [role=option]')].find(node=>node.textContent.includes('Вариант 7')).click()")
            waitFor("document.querySelector('.brand-picker-preview').src.endsWith('icon_07_black.png')")
            js("document.querySelector('[aria-label=\"Закрыть выбор иконки\"]').click();document.querySelector('.theme-toggle').click()")
            waitFor("document.documentElement.dataset.theme==='light'&&document.querySelector('.brand-picker-button img').src.endsWith('icon_07_white.png')")
            assertEquals("Favicon follows the selected design and theme","true",js("document.querySelector('link[rel=icon]').href.endsWith('icon_07_white.png')"))
            js("window.previewReloading=true;location.reload()")
            waitFor("!window.previewReloading&&!!document.querySelector('.brand-picker-button img')&&document.querySelector('.brand-picker-button img').src.endsWith('icon_07_white.png')")
            assertEquals("Demo preferences were not overwritten","true",js("JSON.parse(localStorage.getItem('trudomix-loader-settings')).color==='mint'"))
            screenshot("web-brand-selection.png")
        }finally{companyReady.countDown();instrumentation.runOnMainSync{web.get().destroy()}}
    }
}
