package com.example.trudomix.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import kotlinx.coroutines.*
import kotlin.coroutines.resume

fun hasLocationPermission(context:Context):Boolean = listOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)
    .any{ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED}

@android.annotation.SuppressLint("MissingPermission")
suspend fun currentDeviceLocation(context:Context):Location {
    check(hasLocationPermission(context)){"Разреши доступ к геопозиции в настройках приложения."}
    val manager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    check(LocationManagerCompat.isLocationEnabled(manager)){"Включи геолокацию в настройках телефона и попробуй снова."}
    val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
    val providers=listOf(LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER).filter { (fine||it!=LocationManager.GPS_PROVIDER)&&manager.isProviderEnabled(it) }
    check(providers.isNotEmpty()){ "Нет доступного источника геопозиции. Включи точное местоположение или определение по Wi-Fi." }
    var fallback:Location?=null
    val location=withTimeoutOrNull(20_000) {
        coroutineScope {
            val results=kotlinx.coroutines.channels.Channel<Pair<String,Location?>>(providers.size)
            providers.forEach { provider -> launch {
                val location=suspendCancellableCoroutine<Location?> { continuation ->
                    val signal=CancellationSignal()
                    continuation.invokeOnCancellation{signal.cancel()}
                    LocationManagerCompat.getCurrentLocation(manager,provider,signal,ContextCompat.getMainExecutor(context)){value->if(continuation.isActive)continuation.resume(value)}
                }
                results.send(provider to location)
            } }
            try {
                repeat(providers.size) {
                    val (provider,value)=results.receive()
                    if(value!=null) {
                        fallback=value
                        if(provider==LocationManager.GPS_PROVIDER||LocationManager.GPS_PROVIDER !in providers)return@coroutineScope value
                    }
                }
                fallback
            } finally {coroutineContext.cancelChildren();results.close()}
        }
    }
    return location ?: fallback ?: error("Не удалось определить местоположение. Попробуй у окна или на улице.")
}
