package com.example.trudomix.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LoadingStyle(val id:String,val title:String,val description:String){
    IMPACT("impact","Контур + импульс","Прорисовка, фиксация и волна по экрану"),
    NEON("neon","Неоновая сборка","Светящиеся линии и электрический разряд"),
    SHUTTER("shutter","Световые створки","Две световые панели открывают знак");

    companion object{fun fromId(id:String?)=entries.firstOrNull{it.id==id}?:IMPACT}
}

/** Read before the first splash frame; independent from account/session settings. */
object LoadingStyles{
    private val selected=MutableStateFlow(LoadingStyle.IMPACT)
    val style=selected.asStateFlow()
    fun initialize(context:Context){selected.value=LoadingStyle.fromId(context.getSharedPreferences("loading_style",Context.MODE_PRIVATE).getString("selected",null))}
    fun select(context:Context,style:LoadingStyle){
        context.applicationContext.getSharedPreferences("loading_style",Context.MODE_PRIVATE).edit().putString("selected",style.id).apply()
        selected.value=style
    }
}
