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
import java.util.concurrent.atomic.AtomicReference

/** Real Chromium rendering of the local web build, with isolated API fixtures (no writes). */
class WebFrontendPreviewTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Test fun localFrontendRendersAuthSearchProfileAndChat() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveWeb")=="true")
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val web=AtomicReference<WebView>()
        val url=InstrumentationRegistry.getArguments().getString("webUrl")?:"http://10.0.2.2:5174/"
        val user="""{"id":9999,"email":"preview@example.test","role":"candidate","photo_url":"/brand-dark-waves.png","created_at":"2026-01-01T00:00:00Z"}"""
        val companyLogo=AtomicReference("")
        fun company()="""{"id":99,"name":"Команда Trudomix","it_accredited":true,"description":"Создаём удобные продукты","city":"Новосибирск","logo_url":"${companyLogo.get()}","team_story":"Небольшая команда инженеров, дизайнеров и исследователей.","benefits":["Обучение и конференции","Гибкий график"],"hiring_process":"Знакомство с HR → разговор с командой → предложение","photos":["/brand-dark-waves.png"]}"""
        val vacancies=(1..16).joinToString(",",prefix="[",postfix="]"){"""{"id":$it,"title":"Android-разработчик $it","company":"Команда Trudomix","salary_from":120000,"salary_to":180000,"currency":"RUB","employment":"full_time","schedule":"flexible","location":"Новосибирск","experience":"Middle","remote":true,"skills":["Kotlin","Compose"],"description":"Создавай удобные продукты вместе с командой. Гибкий график и возможность работать удалённо.","latitude":55.03,"longitude":82.92}"""}
        val resume="""{"id":99,"user_id":9999,"full_name":"Александр Иванов","desired_position":"Android-разработчик","city":"Новосибирск","experience":"Middle","skills":["Kotlin","React","Go"],"about":"Создаю удобные продукты и люблю работать с сильными командами.","photo_url":"/brand-dark-waves.png","remote":true}"""
        val employerVacancies=org.json.JSONArray(vacancies).apply { getJSONObject(length()-1).put("is_archived",true) }.toString()
        val detailVacancy=org.json.JSONArray(vacancies).getJSONObject(0).apply {put("company_it_accredited",true);put("description","## Наша команда\n\n**Создаём** полезные продукты.\n\n- Поддержка\n- Обучение\n\n![Команда](/brand-dark-waves.png)\n\n<script>window.markdownInjected=true</script>")}.toString()
        val conversations="""[{"conversation_id":7,"id":7,"application_id":8,"vacancy_id":1,"vacancy_title":"Android-разработчик","company_name":"Команда Trudomix","candidate_id":9999,"candidate_name":"Александр Иванов","last_message":"Давайте обсудим проект","last_message_at":"2026-10-21T10:00:00Z","unread_count":1,"cover_letter":"Здравствуйте! Хочу присоединиться к вашей команде."}]"""
        instrumentation.runOnMainSync { compose.activity.setContentView(WebView(compose.activity).apply {
            web.set(this);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.cacheMode=WebSettings.LOAD_NO_CACHE
            webViewClient=object:WebViewClient(){override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse? {
                val path=request.url.path.orEmpty()
                if(!path.startsWith("/api/")&&request.url.host=="10.0.2.2")return null
                val json=when {
                    path=="/api/vacancies/search"->"""{"vacancies":$vacancies,"total_count":16}"""
                    path=="/api/vacancies/1"->detailVacancy
                    path=="/api/vacancies/1/application"->"""{"applied":false}"""
                    path=="/api/vacancies"&&request.url.getQueryParameter("id")!=null->detailVacancy
                    path=="/api/vacancies"->vacancies
                    path=="/api/me"->user
                    path=="/api/my-company/logo"->{companyLogo.set("/brand-dark-waves.png?uploaded");company()}
                    path=="/api/my-company"->company()
                    path=="/api/companies/99"->company()
                    path=="/api/companies/99/vacancies"->vacancies
                    path=="/api/companies"->"[${company()}]"
                    path=="/api/saved-searches"->if(request.method=="POST")"""{"id":1}""" else """[{"id":1,"name":"Android в Новосибирске","filters":{"query":"Android","location":"Новосибирск","remote":true,"skills":["Kotlin"],"salary_min":100000,"currency":"RUB"}}]"""
                    path=="/api/applications/8/history"->"""[{"id":1,"status":"new","feedback":"","created_at":"2026-10-20T10:00:00Z"},{"id":2,"status":"accepted","feedback":"Приглашаем познакомиться с командой","interview_at":"2026-11-01T10:00:00+07:00","created_at":"2026-10-21T10:00:00Z"}]"""
                    path=="/api/my-vacancies"->"""{"company":${company()},"vacancies":$employerVacancies}"""
                    path=="/api/employer/applications"->"""[{"id":8,"vacancy_id":1,"candidate_user_id":9999,"candidate_email":"preview@example.test","candidate_photo":"/brand-dark-waves.png","resume_id":99,"resume_full_name":"Александр Иванов","resume_position":"Android-разработчик","resume_experience":"Middle","vacancy_title":"Android-разработчик","company_name":"Команда Trudomix","status":"accepted","feedback":"Приглашаем познакомиться с командой","interview_at":"2026-11-01T10:00:00+07:00","created_at":"2026-10-20T10:00:00Z"}]"""
                    path=="/api/my-resume"->resume
                    path=="/api/my-resume/stats"->"""{"total_views":28}"""
                    path=="/api/my-applications"->"""[{"id":8,"vacancy_id":1,"vacancy_title":"Android-разработчик","company_name":"Команда Trudomix","status":"accepted","created_at":"2026-10-20T10:00:00Z"}]"""
                    path=="/api/conversations"->conversations
                    path.endsWith("/messages")->"""[{"id":1,"sender_id":88,"content":"Здравствуйте! Давайте обсудим проект и ваш опыт.","sender_name":"Анна, HR команды","sender_photo":"/brand-dark-waves.png","created_at":"2026-10-21T10:00:00Z"},{"id":2,"sender_id":9999,"content":"Добрый день! Буду рад познакомиться с командой.","sender_name":"Александр","sender_photo":"/brand-dark-waves.png","created_at":"2026-10-21T10:01:00Z","is_read":true}]"""
                    path.endsWith("/typing")->"""{"typing_users":[]}"""
                    else->"[]"
                }
                return WebResourceResponse("application/json","UTF-8",json.byteInputStream())
            }}
            layoutParams=android.view.ViewGroup.LayoutParams(-1,-1)
            loadUrl(url)
        }) }
        fun evaluate(script:String):String {
            val result=AtomicReference<String>();val latch=CountDownLatch(1)
            instrumentation.runOnMainSync{web.get().evaluateJavascript(script){result.set(it);latch.countDown()}}
            assertTrue(latch.await(10,TimeUnit.SECONDS));return result.get()
        }
        fun waitFor(script:String) {val deadline=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<deadline){if(evaluate(script)=="true")return;Thread.sleep(200)};error("Frontend condition failed: $script; ${evaluate("document.body.innerText.slice(0,1500)")}")}
        fun capture(name:String) {
            val painted=CountDownLatch(1)
            instrumentation.runOnMainSync{web.get().postVisualStateCallback(System.nanoTime(),object:WebView.VisualStateCallback(){override fun onComplete(requestId:Long){painted.countDown()}})}
            assertTrue(painted.await(10,TimeUnit.SECONDS));Thread.sleep(700)
            val bitmap=instrumentation.uiAutomation.takeScreenshot();File(instrumentation.targetContext.getExternalFilesDir(null),"web-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            File(instrumentation.targetContext.getExternalFilesDir(null),"web-$name-state.json").writeText(evaluate("JSON.stringify({height:innerHeight,width:innerWidth,app:document.querySelector('.app').getBoundingClientRect().toJSON(),main:document.querySelector('.main-area').getBoundingClientRect().toJSON(),styles:getComputedStyle(document.querySelector('.main-area')).cssText,body:document.body.innerText.slice(0,1000)})"))
            assertEquals("Horizontal overflow on $name","true",evaluate("document.documentElement.scrollWidth<=innerWidth+1"))
            if(name.startsWith("search")||name=="profile")assertEquals("Collapsed app on $name","true",evaluate("innerHeight>100&&document.querySelector('.main-area').getBoundingClientRect().height>innerHeight*.8"))
        }
        try {
        waitFor("!!document.querySelector('.top-header')")
        evaluate("window.previewReloading=true;localStorage.removeItem('user');localStorage.removeItem('token');localStorage.removeItem('theme');sessionStorage.setItem('currentMode','search');location.reload()")
        waitFor("!window.previewReloading&&!!document.querySelector('.header-login-btn')")
        evaluate("document.querySelector('.header-login-btn')?.click()")
        waitFor("!!document.querySelector('.auth-dialog')")
        capture("login")
        evaluate("document.querySelector('.auth-switch').click()")
        waitFor("!!document.querySelector('.auth-roles')")
        capture("register")
        evaluate("window.previewReloading=true;document.querySelector('.auth-close').click();localStorage.setItem('user',JSON.stringify($user));localStorage.setItem('token','preview-token');sessionStorage.setItem('currentMode','search');location.reload()")
        waitFor("!window.previewReloading&&!!document.querySelector('.user-profile-btn')&&document.querySelectorAll('.search-result-card').length===16")
        capture("search-light")
        evaluate("document.querySelector('.filter-toggle-button').click()")
        waitFor("!!document.querySelector('.vacancy-filters')")
        assertEquals("All mobile filters available","true",evaluate("['Компания','Грейд','Требуемый опыт в годах','Навыки','Только с адресом на карте','Сортировка'].every(text=>document.querySelector('.vacancy-filters').innerText.includes(text))"))
        evaluate("[...document.querySelectorAll('.vf-chips button')].find(b=>b.textContent==='В офисе').click()")
        evaluate("[...document.querySelectorAll('.vf-chips button')].find(b=>b.textContent==='Junior').click()")
        evaluate("[...document.querySelectorAll('.vf-chips button')].find(b=>b.textContent==='1-3 года').click()")
        capture("filters-light")
        assertEquals("Draft is not applied yet","true",evaluate("!location.search.includes('experience')"))
        evaluate("document.querySelector('.vf-actions button[type=submit]').click()")
        waitFor("new URLSearchParams(location.search).get('remote')==='false'&&new URLSearchParams(location.search).get('experience')==='Junior|1-3 года'")
        evaluate("document.querySelector('.filter-toggle-button').click()")
        evaluate("document.querySelector('.theme-toggle').click()")
        waitFor("document.documentElement.dataset.theme==='dark'")
        capture("filters-dark")
        evaluate("document.querySelector('.filter-toggle-button').click()")
        capture("search-dark")
        evaluate("document.querySelector('.search-result-card').click()")
        waitFor("!!document.querySelector('.markdown-content h2')")
        assertEquals("Markdown headings and lists","true",evaluate("document.querySelector('.markdown-content h2').textContent==='Наша команда'&&document.querySelectorAll('.markdown-content li').length===2"))
        assertEquals("Raw HTML is not executed","true",evaluate("!window.markdownInjected&&!document.querySelector('.markdown-content script')"))
        evaluate("document.querySelector('.vacancy-company-row .it-accreditation-badge').click()")
        waitFor("!!document.querySelector('.it-accreditation-note')")
        assertEquals("IT comment stays on screen","true",evaluate("(()=>{const r=document.querySelector('.it-accreditation-note').getBoundingClientRect();return r.left>=0&&r.right<=innerWidth})()"))
        capture("it-comment-dark")
        evaluate("document.querySelector('.vacancy-company-row .it-accreditation-badge').click()")
        capture("markdown-vacancy-dark")
        evaluate("document.querySelector('.vacancy-page>.back-btn').click()")
        waitFor("document.querySelectorAll('.search-result-card').length===16")
        evaluate("document.querySelector('.user-profile-btn').click()")
        waitFor("!!document.querySelector('.profile-header')&&document.body.innerText.includes('Александр Иванов')")
        capture("profile")
        assertEquals("Export is available", "true",evaluate("[...document.querySelectorAll('button')].some(b=>b.textContent==='Скачать PDF')"))
        evaluate("document.querySelector('.resume-card.clickable').click()")
        waitFor("!!document.querySelector('.details-modal')")
        assertEquals("Dark resume surface","rgb(22, 27, 34)",evaluate("getComputedStyle(document.querySelector('.details-modal')).backgroundColor").trim('"'))
        capture("resume-dark")
        evaluate("document.querySelector('.details-modal-close').click()")
        evaluate("document.querySelector('.chat-fab').click()")
        waitFor("!!document.querySelector('.chat-app-sidebar-item')")
        evaluate("document.querySelector('.chat-app-sidebar-item').click()")
        waitFor("document.querySelectorAll('.chat-message').length===2")
        evaluate("document.querySelector('.chat-menu-wrapper button[title=\"Меню\"]').click()")
        evaluate("[...document.querySelectorAll('.chat-dropdown-item')].find(b=>b.textContent.includes('Участники')).click()")
        waitFor("!!document.querySelector('.chat-participants-modal')")
        assertEquals("Participants are above the chat","true",evaluate("(()=>{const modal=document.querySelector('.chat-participants-content'),r=modal.getBoundingClientRect();return modal.contains(document.elementFromPoint(r.left+r.width/2,r.top+30))})()"))
        capture("chat-participants-front")
        evaluate("document.querySelector('.chat-participants-close').click()")
        capture("chat")
        evaluate("window.previewReloading=true;sessionStorage.setItem('currentMode','home');location.href=location.pathname")
        waitFor("!window.previewReloading&&!!document.querySelector('.home-hero')&&document.body.innerText.includes('Александр')")
        capture("home-dark")
        evaluate("document.querySelector('.theme-toggle').click()")
        waitFor("document.documentElement.dataset.theme==='light'")
        capture("home-light")
        evaluate("[...document.querySelectorAll('.nav-title')].find(node=>node.textContent==='Поиск').closest('.nav-item').click()")
        waitFor("!!document.querySelector('.results-count .btn')")
        evaluate("document.querySelector('.results-count .btn').click()")
        waitFor("!!document.querySelector('.save-search-form')")
        evaluate("document.querySelector('.save-search-form button').click()")
        waitFor("!document.querySelector('.save-search-form')")
        evaluate("window.previewReloading=true;sessionStorage.setItem('currentMode','saved-searches');location.href=location.pathname")
        waitFor("!window.previewReloading&&document.body.innerText.includes('Android в Новосибирске')")
        capture("saved-searches")
        evaluate("[...document.querySelectorAll('.nav-title')].find(node=>node.textContent==='Мои отклики').closest('.nav-item').click()")
        waitFor("!!document.querySelector('.application-history')")
        evaluate("document.querySelector('.application-history button').click()")
        waitFor("document.querySelectorAll('.application-history li').length===2")
        capture("application-history")
        assertEquals("Application dashboard has three metrics","3",evaluate("document.querySelectorAll('.applications-workspace .workspace-metric').length"))
        evaluate("(()=>{const input=document.querySelector('input[aria-label=\"Поиск по откликам\"]');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,'нет такой компании');input.dispatchEvent(new Event('input',{bubbles:true}));})()")
        waitFor("document.querySelector('.applications-workspace').textContent.includes('Ничего не найдено')")
        evaluate("[...document.querySelectorAll('.workspace-state button')].find(node=>node.textContent.includes('Показать все')).click()")
        waitFor("document.querySelectorAll('.my-app-card').length===1")
        capture("applications-light")
        evaluate("window.previewReloading=true;localStorage.setItem('user',JSON.stringify({...$user,role:'employer'}));sessionStorage.setItem('currentMode','my-company');location.href=location.pathname")
        waitFor("!window.previewReloading&&!!document.querySelector('.company-avatar-editor')")
        assertEquals("Favicon configured","true",evaluate("!!document.querySelector('link[rel=icon][href=\"/brand-dark-waves.png\"]')"))
        evaluate("(()=>{const file=new File([Uint8Array.from(atob('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII='),c=>c.charCodeAt(0))],'company.png',{type:'image/png'});const transfer=new DataTransfer();transfer.items.add(file);const input=document.querySelector('.company-avatar-editor input[type=file]');input.files=transfer.files;input.dispatchEvent(new Event('change',{bubbles:true}));})()")
        waitFor("!!document.querySelector('.company-avatar-editor img[src*=uploaded]')")
        evaluate("document.documentElement.dataset.theme='dark';localStorage.setItem('theme','dark')")
        assertEquals("Company IT badge", "true",evaluate("!!document.querySelector('.company-avatar-editor .it-accreditation-badge')"))
        assertEquals("Dark company surface","rgb(22, 27, 34)",evaluate("getComputedStyle(document.querySelector('.company-info-card')).backgroundColor").trim('"'))
        capture("company-logo")
        evaluate("[...document.querySelectorAll('.company-info-card button')].find(node=>node.textContent.includes('Редактировать страницу')).click()")
        waitFor("!!document.querySelector('#company-team')")
        assertEquals("IT status is editable","true",evaluate("document.querySelector('.company-accreditation-option input').checked"))
        capture("company-editor")
        assertEquals("Company editor uses unified size selector","true",evaluate("!!document.querySelector('#company-size[role=combobox]')&&!document.querySelector('.company-editor-form select')"))
        evaluate("window.previewReloading=true;sessionStorage.setItem('currentMode','profile');location.href=location.pathname")
        waitFor("!window.previewReloading&&!!document.querySelector('.employer-profile-workspace')&&document.querySelectorAll('.employer-profile-shortcuts button').length===4")
        waitFor("document.querySelector('.employer-profile-workspace .workspace-metric strong').textContent==='15'")
        capture("employer-profile-dark")
        evaluate("document.querySelector('.theme-toggle').click()")
        waitFor("document.documentElement.dataset.theme==='light'")
        capture("employer-profile-light")
        evaluate("document.querySelector('.theme-toggle').click()")
        evaluate("[...document.querySelectorAll('.employer-profile-shortcuts button')].find(b=>b.textContent.includes('Моя компания')).click()")
        waitFor("!!document.querySelector('.company-management-hero')")
        evaluate("window.previewReloading=true;sessionStorage.setItem('currentMode','applications');location.href=location.pathname")
        waitFor("!window.previewReloading&&document.querySelectorAll('.recruitment-card').length===1")
        assertEquals("Employer dashboard metrics","3",evaluate("document.querySelectorAll('.employer-applications-workspace .workspace-metric').length"))
        evaluate("document.querySelector('.application-history button').click()")
        waitFor("document.querySelectorAll('.application-history li').length===2&&!!document.querySelector('.application-history form [role=combobox]')")
        evaluate("document.querySelector('.application-stage-form').scrollIntoView({block:'center'})")
        capture("employer-applications-dark")
        evaluate("document.querySelector('.application-stage-form .date-picker-trigger').click()")
        waitFor("!!document.querySelector('.date-picker-popup')")
        assertEquals("Dark calendar surface","rgb(22, 27, 34)",evaluate("getComputedStyle(document.querySelector('.date-picker-popup')).backgroundColor").trim('"'))
        assertEquals("Calendar fits viewport","true",evaluate("(()=>{const r=document.querySelector('.date-picker-popup').getBoundingClientRect();return r.left>=0&&r.right<=innerWidth+1&&r.top>=0&&r.bottom<=innerHeight+1})()"))
        capture("datepicker-dark")
        evaluate("window.calendarHeight=document.querySelector('.date-picker-popup').getBoundingClientRect().height;document.querySelector('.date-picker-popup [aria-label=\"Год\"]').click()")
        waitFor("!!document.querySelector('.form-select-floating [role=option]')")
        assertEquals("Year list does not stretch calendar","true",evaluate("Math.abs(document.querySelector('.date-picker-popup').getBoundingClientRect().height-window.calendarHeight)<2&&document.querySelector('.form-select-floating .form-select-menu').getBoundingClientRect().height<=202"))
        capture("datepicker-year-floating")
        evaluate("[...document.querySelectorAll('.form-select-floating [role=option]')].find(b=>b.textContent==='2026').click()")
        evaluate("document.querySelector('.date-picker-popup button[aria-label=\"Закрыть календарь\"]').click()")
        evaluate("document.querySelector('.theme-toggle').click()")
        waitFor("document.documentElement.dataset.theme==='light'")
        capture("employer-applications-light")
        assertEquals("Theme changes preserve the expanded editor","true",evaluate("!!document.querySelector('.application-stage-form textarea')"))
        evaluate("document.querySelector('.application-stage-form .date-picker-trigger').click()")
        waitFor("!!document.querySelector('.date-picker-popup')")
        capture("datepicker-light")
        evaluate("document.querySelector('[data-day=\"2026-11-01\"]').dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowRight',bubbles:true}))")
        waitFor("document.activeElement?.dataset.day==='2026-11-02'")
        evaluate("document.querySelector('[data-day=\"2026-11-02\"]').click();document.querySelector('.date-picker-popup [aria-label=\"Часы\"]').click()")
        waitFor("!!document.querySelector('.form-select-floating [role=option]')")
        evaluate("[...document.querySelectorAll('.form-select-floating [role=option]')].find(b=>b.textContent==='14').click()")
        evaluate("document.querySelector('.date-picker-popup [aria-label=\"Минуты\"]').click()")
        waitFor("!!document.querySelector('.form-select-floating [role=option]')")
        evaluate("[...document.querySelectorAll('.form-select-floating [role=option]')].find(b=>b.textContent==='30').click()")
        evaluate("document.querySelector('.date-picker-apply').click()")
        waitFor("!document.querySelector('.date-picker-popup')&&document.querySelector('.date-picker-trigger').textContent.includes('14:30')")
        evaluate("(()=>{const original=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.send=function(body){if(typeof body==='string'&&body.includes('interview_at'))window.previewStagePayload=JSON.parse(body);return original.call(this,body)};const input=document.querySelector('.application-stage-form textarea');Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set.call(input,'Встреча с руководителем');input.dispatchEvent(new Event('input',{bubbles:true}));})()")
        evaluate("document.querySelector('.application-stage-form button[type=submit],.application-stage-form>.btn').click()")
        waitFor("window.previewStagePayload?.feedback==='Встреча с руководителем'&&window.previewStagePayload?.interview_at===new Date('2026-11-02T14:30').toISOString()")
        evaluate("document.querySelector('.theme-toggle').click()")
        evaluate("window.previewReloading=true;sessionStorage.setItem('currentMode','my-vacancies');location.href=location.pathname")
        waitFor("!window.previewReloading&&document.querySelectorAll('.workspace-job-card').length===15")
        capture("my-vacancies-dark")
        evaluate("[...document.querySelectorAll('.workspace-tabs button')].find(node=>node.textContent.includes('Архив')).click()")
        waitFor("document.querySelectorAll('.workspace-job-card').length===1")
        assertEquals("Archived job is separate","true",evaluate("document.querySelector('.workspace-job-title').textContent.includes('16')"))
        capture("my-vacancies-archive")
        evaluate("[...document.querySelectorAll('.workspace-header button')].find(node=>node.textContent.includes('Новая вакансия')).click()")
        waitFor("!!document.querySelector('.vacancy-editor-screen')")
        assertEquals("Vacancy form is split into uniform sections","4",evaluate("document.querySelectorAll('.vacancy-form>.editor-section').length"))
        assertEquals("Salary section has the same heading","true",evaluate("document.querySelector('.terms-fields .editor-section-heading>span').textContent==='02'&&!document.querySelector('.terms-fields legend')"))
        evaluate("(()=>{const input=document.querySelector('.position-input-wrapper input');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,'Go-разработчик');input.dispatchEvent(new Event('input',{bubbles:true}));})()")
        waitFor("document.querySelector('.vacancy-editor-preview-card h2').textContent==='Go-разработчик'")
        evaluate("(()=>{const input=document.querySelector('.city-picker-input');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,'Новосибирск');input.dispatchEvent(new Event('input',{bubbles:true}));})()")
        waitFor("document.querySelector('.vacancy-editor-preview-card .workspace-job-meta').textContent.includes('Новосибирск')")
        evaluate("document.querySelector('#vacancy-grade').click()")
        waitFor("!!document.querySelector('#vacancy-grade-options')")
        waitFor("document.querySelectorAll('.form-select-menu').length===1")
        evaluate("document.querySelector('#vacancy-grade').scrollIntoView({block:'center'})")
        val menuColor=evaluate("getComputedStyle(document.querySelector('.form-select-menu')).backgroundColor")
        capture("vacancy-grade-menu")
        evaluate("[...document.querySelectorAll('#vacancy-grade-options [role=option]')].find(b=>b.textContent.includes('Middle')).click()")
        evaluate("document.querySelector('#vacancy-currency').dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowDown',bubbles:true}))")
        waitFor("!!document.querySelector('#vacancy-currency-options')")
        assertEquals("Unified menu surface",menuColor,evaluate("getComputedStyle(document.querySelector('.form-select-menu')).backgroundColor"))
        evaluate("document.querySelector('#vacancy-currency').dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowDown',bubbles:true}))")
        evaluate("document.querySelector('#vacancy-currency').dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}))")
        waitFor("document.querySelector('#vacancy-currency').textContent.includes('USD')")
        assertEquals("No mixed native selects","0",evaluate("document.querySelectorAll('.vacancy-form select').length"))
        evaluate("(()=>{const input=document.querySelector('#vacancy-skill');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,'Kotlin');input.dispatchEvent(new Event('input',{bubbles:true}));})()")
        waitFor("!!document.querySelector('#vacancy-skill-options [role=option]')")
        evaluate("document.querySelector('#vacancy-skill').dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}))")
        waitFor("document.querySelector('.skills-list').textContent.includes('Kotlin')")
        capture("create-vacancy-dark")
        evaluate("document.querySelector('.theme-toggle').click()")
        waitFor("document.documentElement.dataset.theme==='light'")
        capture("create-vacancy-light")
        evaluate("document.querySelector('.theme-toggle').click()")
        waitFor("document.documentElement.dataset.theme==='dark'")
        evaluate("window.previewReloading=true;sessionStorage.setItem('currentMode','companies');location.href=location.pathname")
        waitFor("!window.previewReloading&&!!document.querySelector('.company-card')")
        assertEquals("Dark public company card","rgb(22, 27, 34)",evaluate("getComputedStyle(document.querySelector('.company-card')).backgroundColor").trim('"'))
        evaluate("document.querySelector('.company-card').click()")
        waitFor("!!document.querySelector('.company-profile-hero')")
        assertEquals("Dark company surface","rgb(22, 27, 34)",evaluate("getComputedStyle(document.querySelector('.company-profile-logo')).backgroundColor").trim('"'))
        assertEquals("IT badge in company profile","true",evaluate("!!document.querySelector('.company-profile-hero .it-accreditation-badge')"))
        assertEquals("Company vacancies include salary","true",evaluate("document.querySelector('.company-profile-job .vacancy-terms').textContent.includes('120')"))
        assertEquals("Job cards support keyboard","BUTTON",evaluate("document.querySelector('.company-profile-job').tagName").trim('"'))
        capture("company-profile-dark")
        evaluate("document.querySelector('.theme-toggle').click()")
        waitFor("document.documentElement.dataset.theme==='light'")
        assertEquals("Light company surface","rgb(255, 255, 255)",evaluate("getComputedStyle(document.querySelector('.company-profile-logo')).backgroundColor").trim('"'))
        capture("company-profile-light")
        } finally {
            evaluate("localStorage.removeItem('user');localStorage.removeItem('token');sessionStorage.removeItem('currentMode');localStorage.removeItem('theme')")
            instrumentation.runOnMainSync{web.get().destroy()}
        }
    }
}
