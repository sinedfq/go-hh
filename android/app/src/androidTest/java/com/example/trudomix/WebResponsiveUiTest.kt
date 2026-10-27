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
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Real layout and interaction checks against the production build; all API calls use fixtures. */
class WebResponsiveUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Test fun compactStoriesToolbarAndGlobalChatHeaderAcrossViewports(){
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveWeb")=="true")
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val web=AtomicReference<WebView>()
        val user="""{"id":9999,"email":"preview@example.test","role":"candidate","photo_url":"/brand/wave-night.png"}"""
        val company="""{"id":99,"name":"Команда Trudomix","logo_url":"/brand/wave-night.png","description":"Продукты для людей","photos":[],"videos":[]}"""
        val expiry=Instant.now().plusSeconds(86400).toString()
        val requests=AtomicInteger();val block=AtomicBoolean(false);val release=CountDownLatch(1)
        instrumentation.runOnMainSync{compose.activity.setContentView(WebView(compose.activity).apply{
            web.set(this);setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.useWideViewPort=false;settings.cacheMode=WebSettings.LOAD_NO_CACHE
            webViewClient=object:WebViewClient(){override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse?{
                val path=request.url.path.orEmpty()
                if(!path.startsWith("/api/"))return null
                val body=when(path){
                    "/api/me"->user
                    "/api/stories"->{requests.incrementAndGet();if(block.get())release.await(10,TimeUnit.SECONDS);"""[{"id":1,"company_id":99,"company_name":"Команда Trudomix","company_logo":"/brand/wave-night.png","media_url":"/brand/wave-night.png","media_type":"image","can_delete":true,"expires_at":"$expiry"}]"""}
                    "/api/my-company"->company
                    "/api/my-vacancies"->"""{"company":$company,"vacancies":[]}"""
                    "/api/feed"->"""{"posts":[],"next_cursor":0}"""
                    "/api/vacancies/search"->"""{"vacancies":[],"total_count":0}"""
                    "/api/my-resume"->"""{"id":99,"full_name":"Александр","desired_position":"Разработчик"}"""
                    "/api/my-resume/stats"->"""{"total_views":2}"""
                    "/api/conversations"->"""[{"conversation_id":7,"application_id":8,"vacancy_id":1,"company_name":"Команда Trudomix","vacancy_title":"Разработчик","unread_count":0}]"""
                    else->"[]"
                }
                return WebResourceResponse("application/json","UTF-8",body.byteInputStream())
            }}
            loadUrl(InstrumentationRegistry.getArguments().getString("webUrl")?:"http://10.0.2.2:5174/")
        })}
        fun js(script:String):String{val result=AtomicReference<String>();val latch=CountDownLatch(1);instrumentation.runOnMainSync{web.get().evaluateJavascript(script){result.set(it);latch.countDown()}};if(!latch.await(2,TimeUnit.SECONDS))return "null";return result.get()}
        fun waitFor(script:String){val deadline=System.currentTimeMillis()+25000;while(System.currentTimeMillis()<deadline){if(js(script)=="true")return;Thread.sleep(100)};error("Responsive condition: $script; ${js("document.body.innerText.slice(0,800)")}")}
        fun check(label:String,script:String){val result=js(script);val diagnostic=if(result!="true")js("JSON.stringify({viewport:innerWidth,doc:document.documentElement.scrollWidth,roots:[document.documentElement,document.body,document.querySelector('#root'),document.querySelector('.app'),document.querySelector('.sidebar'),document.querySelector('.main-area'),document.querySelector('.content')].map(e=>[e.className||e.tagName,e.getBoundingClientRect().width,getComputedStyle(e).display,getComputedStyle(e).flex,getComputedStyle(e).position]),home:[document.querySelector('.home-page')?.clientWidth,document.querySelector('.home-page')?.scrollWidth],overflow:[...document.querySelectorAll('.home-page *')].filter(e=>e.scrollWidth>e.clientWidth+1&&e.clientWidth>0).slice(0,10).map(e=>[e.className,e.clientWidth,e.scrollWidth])})")else "";assertEquals("$label: $diagnostic","true",result)}
        fun navigate(label:String){js("[...document.querySelectorAll('.nav-title')].find(n=>n.textContent==='$label').closest('.nav-item').click()")}
        fun capture(name:String){if(name.startsWith("ui-home")){js("document.querySelector('.home-page').getAnimations({subtree:true}).filter(a=>a.effect.getTiming().iterations!==Infinity).forEach(a=>a.finish())");waitFor("Number(getComputedStyle(document.querySelector('.home-page')).opacity)>.99");check("Primary colors after transitions","(()=>{const style=getComputedStyle(document.querySelector('.home-actions .btn-primary'));return ['rgb(130, 87, 230)','rgb(148, 116, 237)'].includes(style.backgroundColor)})()");};val latch=CountDownLatch(1);instrumentation.runOnMainSync{web.get().postVisualStateCallback(1,object:WebView.VisualStateCallback(){override fun onComplete(id:Long){latch.countDown()}})};assertTrue(latch.await(30,TimeUnit.SECONDS));instrumentation.runOnMainSync{val v=web.get();val bitmap=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap));File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}}
        try{
            waitFor("!!document.querySelector('.top-header')")
            js("window.uiReload=true;localStorage.removeItem('trudomix-brand');localStorage.setItem('user',JSON.stringify($user));localStorage.setItem('token','preview-token');sessionStorage.setItem('currentMode','home');location.reload()")
            waitFor("!window.uiReload&&!document.getElementById('startup-splash')&&!!document.querySelector('.social-story-ring')")
            // Burst clicks must create exactly one request, even before React can commit disabled=true.
            val before=requests.get();block.set(true)
            js("for(let i=0;i<12;i++)document.querySelector('.social-stories .compact-refresh-button').click()")
            waitFor("document.querySelector('.social-stories .compact-refresh-button').disabled")
            Thread.sleep(250);assertEquals(before+1,requests.get());release.countDown();block.set(false)
            waitFor("!document.querySelector('.social-stories .compact-refresh-button').disabled")
            js("window.waveCalls=[];window.waveErrors=[];if(document.startViewTransition){const nativeTransition=document.startViewTransition.bind(document);document.startViewTransition=update=>{const t=nativeTransition(update);t.ready.catch(e=>waveErrors.push(String(e)));return t}};const nativeAnimate=Element.prototype.animate;Element.prototype.animate=function(frames,options){if(options?.pseudoElement==='::view-transition-new(root)')window.waveCalls.push(frames);return nativeAnimate.call(this,frames,options)}")
            val selectedWidth=InstrumentationRegistry.getArguments().getString("onlyWidth")?.toIntOrNull()
            for(theme in listOf("light","dark"))for(width in selectedWidth?.let{listOf(it)}?:listOf(360,430,768,1024,1280,1920)){
                val brandFile=if(theme=="dark")"wave-night.png"else "wave-light.png"
                instrumentation.runOnMainSync{val v=web.get();v.layoutParams=v.layoutParams.apply{this.width=kotlin.math.round(width*v.resources.displayMetrics.density).toInt()}}
                js("if(document.documentElement.dataset.theme!=='$theme')document.querySelector('.theme-toggle').click()")
                waitFor("!document.documentElement.classList.contains('theme-wave-active')&&document.documentElement.dataset.theme==='$theme'&&[...document.querySelectorAll('img.trudomix-brand-image')].every(img=>img.src.includes('$brandFile')&&img.complete)")
                waitFor("Math.abs(innerWidth-$width)<=1")
                check("Theme wave uses the new document snapshot or falls back cleanly","!document.startViewTransition||matchMedia('(prefers-reduced-motion: reduce)').matches||waveCalls.some(f=>f.clipPath?.[0].startsWith('circle(0px at '))||waveErrors.length>0||document.documentElement.dataset.theme==='light'")
                waitFor("document.querySelector('.main-area').getBoundingClientRect().width>innerWidth*.5")
                check("Compact round stories $theme/$width","(()=>{const s=document.querySelector('.social-stories'),r=s.querySelector('.social-story-cover').getBoundingClientRect();return r.width>=64&&r.width<=76&&Math.abs(r.width-r.height)<1&&getComputedStyle(s).backgroundColor==='rgba(0, 0, 0, 0)'&&s.getBoundingClientRect().height<190&&document.querySelector('.home-page').firstElementChild===s})()")
                check("Home overflow $theme/$width","document.documentElement.scrollWidth<=innerWidth+1&&document.querySelector('.home-page').scrollWidth<=document.querySelector('.home-page').clientWidth+1")
                check("Icon picker keeps its own bounded styles","document.querySelector('.brand-picker-button').getBoundingClientRect().width<=44&&document.querySelector('.brand-picker-button img').getBoundingClientRect().width<=32&&document.querySelector('.top-header').getBoundingClientRect().height<140")
                if(width==360||width==1280)capture("ui-home-$theme-$width.png")
                js("document.querySelector('.theme-toggle').click()")
            waitFor("!document.documentElement.classList.contains('theme-wave-active')")
            check("Supported browsers animate the new root snapshot "+js("JSON.stringify(waveErrors)"),"!document.startViewTransition||matchMedia('(prefers-reduced-motion: reduce)').matches||waveCalls.length>0||waveErrors.length>0")
            val beforeBurst=js("document.documentElement.dataset.theme")
            js("for(let i=0;i<4;i++)document.querySelector('.theme-toggle').click()")
            waitFor("!document.documentElement.classList.contains('theme-wave-active')")
            assertEquals("Four rapid toggles preserve the final theme",beforeBurst,js("document.documentElement.dataset.theme"))
            js("window.normalMedia=matchMedia.bind(window);window.matchMedia=q=>q==='(prefers-reduced-motion: reduce)'?{matches:true}:normalMedia(q);window.beforeReduced=waveCalls.length;document.querySelector('.theme-toggle').click()")
            check("Reduced motion commits immediately without wave","!document.documentElement.classList.contains('theme-wave-active')&&waveCalls.length===beforeReduced")
            js("window.matchMedia=window.normalMedia")
            navigate("Поиск");waitFor("!!document.querySelector('.profile-toggle-button')")
                check("Matching toolbar $theme/$width","(()=>{const b=document.querySelector('.search-bar-wrapper'),f=b.querySelector('.filter-toggle-button'),p=b.querySelector('.profile-toggle-button'),a=f.getBoundingClientRect(),c=p.getBoundingClientRect();return Math.abs(a.top-c.top)<1&&a.width===c.width&&a.height===c.height&&getComputedStyle(f).borderRadius===getComputedStyle(p).borderRadius&&c.right<=innerWidth&&b.scrollWidth<=b.clientWidth+1&&!document.querySelector('.user-profile-btn')&&!document.querySelector('.header-profile-btn')})()")
                js("document.querySelector('.profile-toggle-button').click()")
                waitFor("!!document.querySelector('.profile-header')")
                js("document.querySelector('.chat-fab').click()")
                waitFor("!!document.querySelector('.chat-app-sidebar-item')")
                js("document.querySelector('.chat-app-sidebar-item').click()")
                waitFor("!!document.querySelector('.chat-app-main-header')")
                check("Global close $theme/$width","(()=>{const w=document.querySelector('.chat-app-container'),h=w.querySelector('.chat-app-window-header'),b=h.querySelector('[aria-label=\"Закрыть чат\"]'),r=b.getBoundingClientRect(),box=w.getBoundingClientRect();return h.parentElement===w&&r.width>=40&&r.right<=box.right&&box.right-r.right<=25&&r.top>=box.top&&h.getBoundingClientRect().width>=box.width-2&&b.contains(document.elementFromPoint(r.left+r.width/2,r.top+r.height/2))})()")
                if(width==360||width==1280)capture("ui-chat-$theme-$width.png")
                js("document.querySelector('[aria-label=\"Закрыть чат\"]').click()")
                waitFor("!document.querySelector('.chat-app-container')")
                navigate("Главная");waitFor("!!document.querySelector('.social-story-ring')")
            }
            js("window.uiReload=true;localStorage.setItem('user',JSON.stringify({...$user,role:'employer'}));sessionStorage.setItem('currentMode','my-company');location.href=location.pathname")
            waitFor("!window.uiReload&&!document.getElementById('startup-splash')&&!!document.querySelector('.social-story-ring')&&!!document.querySelector('.company-management-hero')")
            check("Employer reuses compact stories and retains creator","!!document.querySelector('.social-composer')&&document.querySelector('.social-story-cover').getBoundingClientRect().width<=76&&getComputedStyle(document.querySelector('.social-stories')).backgroundColor==='rgba(0, 0, 0, 0)'")
            js("document.querySelector('.social-story-ring').click()")
            waitFor("!!document.querySelector('.media-viewer')")
        }finally{release.countDown();instrumentation.runOnMainSync{web.get().destroy()}}
    }
}
