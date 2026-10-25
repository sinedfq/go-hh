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
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class WebSocialExperienceTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Test fun webSubscriptionsPublishingStoriesLikesAndComments(){
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveWeb")=="true")
        val instrumentation=InstrumentationRegistry.getInstrumentation();val web=AtomicReference<WebView>()
        val url=InstrumentationRegistry.getArguments().getString("webUrl")?:"http://10.0.2.2:5174/"
        val following=AtomicBoolean(false);val posted=AtomicBoolean(false);val liked=AtomicBoolean(false);val commented=AtomicBoolean(false)
        val user="""{"id":9999,"email":"preview@example.test","role":"candidate"}"""
        val company="""{"id":99,"name":"Команда Trudomix","description":"Создаём продукты","logo_url":"/brand-dark-waves.png"}"""
        val expiry=Instant.now().plusSeconds(86400).toString()
        fun post(id:Int,own:Boolean)="""{"id":$id,"author_name":"${if(own)"Александр"else "Команда Trudomix"}","company_id":${if(own)"null"else "99"},"body":"${if(own)"Мой новый пост"else "Новости команды"}","can_delete":$own,"liked":${liked.get()},"likes":${if(liked.get())1 else 0},"comments":${if(commented.get())1 else 0},"created_at":"2026-10-25T10:00:00Z"}"""
        instrumentation.runOnMainSync{compose.activity.setContentView(WebView(compose.activity).apply{
            web.set(this);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.cacheMode=WebSettings.LOAD_NO_CACHE
            webViewClient=object:WebViewClient(){override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse?{
                val path=request.url.path.orEmpty();if(!path.startsWith("/api/"))return null
                val body=when{
                    path=="/api/me"->user
                    path=="/api/companies"->"[$company]"
                    path=="/api/companies/99"->company
                    path.endsWith("/subscription")->{if(request.method=="PUT")following.set(true);if(request.method=="DELETE")following.set(false);"""{"following":${following.get()},"followers":${if(following.get())1 else 0}}"""}
                    path=="/api/stories"->if(following.get())"""[{"id":1,"company_id":99,"company_name":"Команда Trudomix","company_logo":"/brand-dark-waves.png","media_url":"/brand-dark-waves.png","media_type":"image","caption":"День команды","expires_at":"$expiry"}]"""else "[]"
                    path=="/api/posts"->{posted.set(true);"""{"id":2}"""}
                    path=="/api/posts/2"&&request.method=="DELETE"->{posted.set(false);"{}"}
                    path.endsWith("/like")->{liked.set(request.method=="PUT");"""{"likes":${if(liked.get())1 else 0},"liked":${liked.get()}}"""}
                    path.endsWith("/comments")->{if(request.method=="POST"){commented.set(true);"""{"id":7}"""}else """{"comments":${if(commented.get())"""[{"id":7,"author_name":"Александр","body":"Спасибо команде","can_delete":true,"created_at":"2026-10-25T10:00:00Z"}]"""else "[]"},"next_cursor":0}"""}
                    path=="/api/comments/7"->{commented.set(false);"{}"}
                    path=="/api/feed"->"""{"posts":[${if(posted.get())post(2,true)+","else ""}${post(1,false)}],"next_cursor":0}"""
                    path=="/api/vacancies/search"->"""{"vacancies":[],"total_count":0}"""
                    path=="/api/my-resume"->"""{"id":99,"full_name":"Александр"}"""
                    path=="/api/my-resume/stats"->"""{"total_views":0}"""
                    else->"[]"
                };return WebResourceResponse("application/json","UTF-8",body.byteInputStream())
            }};loadUrl(url)
        })}
        fun js(script:String):String{val result=AtomicReference<String>();val latch=CountDownLatch(1);instrumentation.runOnMainSync{web.get().evaluateJavascript(script){result.set(it);latch.countDown()}};assertTrue(latch.await(10,TimeUnit.SECONDS));return result.get()}
        fun waitFor(script:String){val deadline=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<deadline){if(js(script)=="true")return;Thread.sleep(150)};error("Web social condition: $script; ${js("document.body.innerText.slice(0,2000)")}")}
        fun input(selector:String,value:String){js("(()=>{const field=document.querySelector('$selector');Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set.call(field,'$value');field.dispatchEvent(new Event('input',{bubbles:true}));})()")}
        fun screenshot(name:String){Thread.sleep(400);val bitmap=instrumentation.uiAutomation.takeScreenshot();File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle();assertEquals("No horizontal overflow","true",js("document.documentElement.scrollWidth<=innerWidth+1"))}
        try{
            waitFor("!!document.querySelector('.top-header')")
            js("window.loadingPreview=true;localStorage.setItem('theme','light');localStorage.setItem('user',JSON.stringify($user));localStorage.setItem('token','preview-token');sessionStorage.setItem('currentMode','companies');location.reload()")
            waitFor("!window.loadingPreview&&!!document.querySelector('.company-card')")
            js("document.querySelector('.company-card').click()")
            waitFor("!!document.querySelector('.company-follow button:not(:disabled)')")
            js("document.querySelector('.company-follow button').click()")
            waitFor("document.querySelector('.company-follow button').textContent==='Вы подписаны'")
            assertTrue(following.get())
            js("window.loadingPreview=true;sessionStorage.setItem('currentMode','home');location.href=location.pathname")
            waitFor("!window.loadingPreview&&document.querySelectorAll('.social-post').length===1&&document.querySelectorAll('.social-story-ring').length===1")
            js("document.querySelector('.social-story-ring').click()")
            waitFor("!!document.querySelector('.media-viewer')&&document.querySelector('.media-story-caption').textContent==='День команды'")
            js("document.querySelector('[aria-label=\"Закрыть просмотр\"]').click()")
            input(".social-composer textarea","Мой новый пост")
            js("document.querySelector('.social-composer .btn-primary').click()")
            waitFor("document.querySelectorAll('.social-post').length===2")
            js("document.querySelector('.social-post-reactions button').click()")
            waitFor("document.querySelector('.social-post-reactions button').getAttribute('aria-pressed')==='true'")
            js("document.querySelector('.social-post-reactions button[aria-expanded]').click()")
            waitFor("!!document.querySelector('.social-comments textarea')&&!document.querySelector('.social-comments [role=status]')")
            input(".social-comments textarea","Спасибо команде")
            js("document.querySelector('.social-comments form button').click()")
            waitFor("document.querySelectorAll('.social-comment').length===1")
            js("document.querySelector('.social-feed').scrollIntoView()")
            screenshot("web-social-light.png")
            js("document.querySelector('.social-comment button').click()")
            waitFor("document.querySelectorAll('.social-comment').length===0")
            js("document.querySelector('.theme-toggle').click();document.querySelector('.social-feed').scrollIntoView()")
            waitFor("document.documentElement.dataset.theme==='dark'")
            screenshot("web-social-dark.png")
            js("document.querySelector('.social-post>header>button').click()")
            waitFor("!!document.querySelector('.social-delete-confirm')")
            js("document.querySelector('.social-delete-confirm .btn-primary').click()")
            waitFor("document.querySelectorAll('.social-post').length===1")
            assertTrue(liked.get());assertFalse(commented.get());assertFalse(posted.get())
        }finally{instrumentation.runOnMainSync{web.get().destroy()}}
    }
}
