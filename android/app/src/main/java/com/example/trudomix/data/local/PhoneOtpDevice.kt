package com.example.trudomix.data.local

import android.content.Context
import java.security.SecureRandom

object PhoneOtpDevice{
    @Synchronized fun key(context:Context):String{
        val prefs=context.applicationContext.getSharedPreferences("phone_otp_device",Context.MODE_PRIVATE)
        prefs.getString("key",null)?.takeIf{it.matches(Regex("[0-9a-f]{64}"))}?.let{return it}
        val bytes=ByteArray(32).also{SecureRandom().nextBytes(it)}
        val key=bytes.joinToString(""){"%02x".format(it.toInt() and 255)}
        prefs.edit().putString("key",key).apply();return key
    }
}
