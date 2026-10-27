package com.example.trudomix.push

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.trudomix.MainActivity
import com.example.trudomix.R
import com.example.trudomix.data.local.AppIcons

/** User-requested development codes are separate from optional business-event notifications. */
object OtpNotifications{
    fun expect(context:Context,id:String){context.getSharedPreferences("otp_delivery",Context.MODE_PRIVATE).edit().putString("request",id).putLong("expires",System.currentTimeMillis()+300000).commit()}
    fun receive(context:Context,data:Map<String,String>){
        val preferences=context.getSharedPreferences("otp_delivery",Context.MODE_PRIVATE)
        val expires=data["expires_at"]?.toLongOrNull()?:return
        if(data["request_id"]!=preferences.getString("request",null)||System.currentTimeMillis()>=preferences.getLong("expires",0)||expires*1000<=System.currentTimeMillis())return
        show(context,data["phone"].orEmpty(),data["code"].orEmpty(),((expires*1000-System.currentTimeMillis())/1000).toInt().coerceIn(1,300))
    }
    fun receiveWeb(context:Context,data:Map<String,String>){
        val expires=data["expires_at"]?.toLongOrNull()?:return
        val remaining=expires-System.currentTimeMillis()/1000
        if(remaining !in 1..300)return
        show(context,data["phone"].orEmpty(),data["code"].orEmpty(),remaining.toInt(),web=true)
    }
    const val CHANNEL="trudomix_auth_codes"
    const val ID=0x54524F54
    fun createChannel(context:Context){context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Коды подтверждения",NotificationManager.IMPORTANCE_HIGH).apply{description="Коды регистрации и входа, запрошенные вами"})}
    fun allowed(context:Context):Boolean{
        createChannel(context)
        return NotificationManagerCompat.from(context).areNotificationsEnabled()&&(android.os.Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)&&context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL).importance!=NotificationManager.IMPORTANCE_NONE
    }
    fun show(context:Context,phone:String,code:String,expiresIn:Int,web:Boolean=false):Boolean{
        if(!allowed(context)||!code.matches(Regex("[0-9]{6}")))return false
        val open=PendingIntent.getActivity(context,ID,Intent(context,MainActivity::class.java).setAction("com.example.trudomix.OTP").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(ContextCompat.getDrawable(context,AppIcons.current(context).resource)?.toBitmap(192,192))
            .setContentTitle(if(web)"Код входа на сайт Trudomix"else "Код Trudomix").setContentText("Код: $code · номер …${phone.takeLast(4)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(if(web)"Код входа на сайт: $code\nВведите его только на сайте Trudomix, если вы запросили вход. Никому не сообщайте код."else "Код: $code\nДля номера …${phone.takeLast(4)}. Действует ${expiresIn/60} мин. Это временная доставка через приложение, SMS не отправляется."))
            .setContentIntent(open).setAutoCancel(true).setLocalOnly(true).setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_STATUS).setPriority(NotificationCompat.PRIORITY_HIGH).setTimeoutAfter(expiresIn*1000L).build()
        return try{context.getSystemService(NotificationManager::class.java).notify("trudomix-otp",ID,notification);true}catch(_:SecurityException){false}
    }
    fun clear(context:Context){context.getSharedPreferences("otp_delivery",Context.MODE_PRIVATE).edit().clear().apply();context.getSystemService(NotificationManager::class.java).cancel("trudomix-otp",ID)}
}
