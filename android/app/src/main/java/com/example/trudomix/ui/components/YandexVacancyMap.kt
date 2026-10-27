package com.example.trudomix.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.*
import androidx.core.view.doOnLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.AndroidView
import com.example.trudomix.BuildConfig
import com.example.trudomix.data.models.Vacancy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YandexVacancyMap(items: List<Vacancy>, onSelect: (Vacancy) -> Unit, onError: (String) -> Unit, modifier: Modifier = Modifier, controller: VacancyMapController? = null) {
    val context = LocalContext.current
    val select by rememberUpdatedState(onSelect)
    val error by rememberUpdatedState(onError)
    val vacancies by rememberUpdatedState(items)
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == "trudomix" && uri.host == "vacancy") {
                        vacancies.firstOrNull { it.id == uri.lastPathSegment?.toIntOrNull() }?.let(select)
                    } else if (uri.scheme == "trudomix" && uri.host == "map-error") {
                        error("Не удалось загрузить Яндекс.Карты. Проверьте интернет и настройки ключа карты.")
                    } else if (request.hasGesture() && uri.scheme == "https") {
                        runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
                    }
                    // Keep navigation inside the application; map resources load as subresources.
                    return true
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                    if (request.isForMainFrame || request.url.host == "api-maps.yandex.ru") {
                        error("Нет соединения с Яндекс.Картами. Проверьте интернет и повторите попытку.")
                    }
                }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame || request.url.host == "api-maps.yandex.ru") {
                        error("Яндекс.Карты: HTTP ${response.statusCode}. Проверьте доступность сервиса и ключ карты.")
                    }
                }
            }
        }
    }
    LaunchedEffect(webView, items) {
        if (BuildConfig.YANDEX_MAPS_API_KEY.isBlank()) error("Не настроен ключ Яндекс.Карт")
        else webView.doOnLayout {
            webView.loadDataWithBaseURL("https://appassets.androidplatform.net/", mapHtml(items, dark), "text/html", "UTF-8", null)
        }
    }
    SideEffect { webView.evaluateJavascript("window.pendingDark=$dark;if(window.jobTheme)window.jobTheme($dark);",null) }
    DisposableEffect(webView,controller) { controller?.view=webView; onDispose { controller?.view=null; webView.stopLoading(); webView.destroy() } }
    AndroidView(factory = { webView }, modifier = modifier)
}

private fun mapHtml(items: List<Vacancy>, dark: Boolean): String {
    val points = buildJsonArray {
        items.forEach { vacancy -> add(buildJsonObject {
            put("id", vacancy.id)
            put("lat", vacancy.latitude)
            put("lon", vacancy.longitude)
            put("title", android.text.TextUtils.htmlEncode(vacancy.title))
        }) }
    }.toString().replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026")
    val key = Uri.encode(BuildConfig.YANDEX_MAPS_API_KEY)
    return """
        <!DOCTYPE html><html lang="ru"><head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>html,body{margin:0;width:100%;height:100%;background:#F8F7FC}#map{position:fixed;inset:0;width:100vw;height:100vh}
        .job-pin{width:38px;height:38px;background:var(--pin-color,#8257E6);color:var(--pin-text,white);border:3px solid white;border-radius:14px 14px 14px 3px;box-shadow:0 4px 12px #19143155;transform:rotate(-45deg);display:flex;align-items:center;justify-content:center}.job-pin svg{transform:rotate(45deg);width:21px;height:21px}</style>
        <script>function mapError(){location.href='trudomix://map-error';}window.addEventListener('error',mapError);
        var ready=false;setTimeout(function(){if(!ready)mapError();},25000);</script>
        <script src="https://api-maps.yandex.ru/2.1/?apikey=$key&lang=ru_RU" onerror="mapError()"></script></head>
        <body><div id="map"></div><script>
        if(typeof ymaps==='undefined'){mapError();}else ymaps.ready(function(){try{
            var points=$points;
            var element=document.getElementById('map');
            element.style.height=window.innerHeight+'px';
            var map=new ymaps.Map('map',{center:points.length?[points[0].lat,points[0].lon]:[55.03,82.92],zoom:points.length?11:4,controls:[]});
            window.jobMap=map;
            // JS 2.1 has no native theme. Tint only the documented ground pane;
            // markers, attribution and provider controls retain their original colors.
            window.jobTheme=function(dark){document.documentElement.style.setProperty('--pin-color',dark?'#9474ED':'#8257E6');document.documentElement.style.setProperty('--pin-text',dark?'#191431':'white');map.panes.get('ground').getElement().style.filter=dark?'invert(.9) hue-rotate(180deg) brightness(1.15) saturate(.7)':'none';document.body.style.background=dark?'#080816':'#F8F7FC';};
            window.jobTheme(typeof window.pendingDark==='boolean'?window.pendingDark:$dark);
            var userMarker=null;
            window.jobLocate=function(lat,lon,accuracy){
                if(userMarker)map.geoObjects.remove(userMarker);
                userMarker=new ymaps.Placemark([lat,lon],{hintContent:'Вы здесь · точность около '+Math.round(accuracy)+' м'},{preset:'islands#blueCircleDotIcon'});
                map.geoObjects.add(userMarker);map.setCenter([lat,lon],13,{duration:300});
            };
            var pin=ymaps.templateLayoutFactory.createClass('<div class="job-pin"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="7" width="18" height="14" rx="3"/><path d="M8 7V4h8v3M3 12h18M10 12v3h4v-3"/></svg></div>');
            var cluster=new ymaps.Clusterer({preset:'islands#blueClusterIcons',groupByCoordinates:false});
            points.forEach(function(p){
                var marker=new ymaps.Placemark([p.lat,p.lon],{hintContent:p.title},{iconLayout:pin,iconShape:{type:'Rectangle',coordinates:[[0,0],[44,44]]},iconOffset:[-22,-42]});
                marker.events.add('click',function(){location.href='trudomix://vacancy/'+p.id;});
                cluster.add(marker);
            });
            map.geoObjects.add(cluster);
            window.jobFit=function(){if(points.length>1)map.setBounds(cluster.getBounds(),{checkZoomRange:true,zoomMargin:[160,40,160,40]});else if(points.length)map.setCenter([points[0].lat,points[0].lon],12);};
            window.jobFit();
            setTimeout(function(){map.container.fitToViewport();},100);
            window.addEventListener('resize',function(){element.style.height=window.innerHeight+'px';map.container.fitToViewport();});
            ready=true;
        }catch(e){mapError();}});
        </script></body></html>
    """.trimIndent()
}

class VacancyMapController {
    internal var view: WebView? = null
    fun zoom(delta:Int) { view?.evaluateJavascript("if(window.jobMap)window.jobMap.setZoom(Math.max(2,Math.min(19,window.jobMap.getZoom()+($delta))),{duration:200});",null) }
    fun fit() { view?.evaluateJavascript("if(window.jobFit)window.jobFit();",null) }
    fun locate(latitude:Double,longitude:Double,accuracy:Float) {
        if(!latitude.isFinite()||!longitude.isFinite()||!accuracy.isFinite())return
        view?.evaluateJavascript("(function(){var tries=0;function locate(){if(window.jobLocate)window.jobLocate($latitude,$longitude,$accuracy);else if(++tries<60)setTimeout(locate,500);}locate();})()",null)
    }
}
