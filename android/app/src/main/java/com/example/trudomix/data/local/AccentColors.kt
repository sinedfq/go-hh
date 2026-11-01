package com.example.trudomix.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AccentColor(val title:String,val light:Color,val dark:Color){
    PURPLE("Фиолетовый",Color(0xFF8257E6),Color(0xFFAD8CFF)),
    BLUE("Синий",Color(0xFF245FCC),Color(0xFF91B7FF)),
    TEAL("Бирюзовый",Color(0xFF006F70),Color(0xFF64D6CF)),
    GREEN("Зелёный",Color(0xFF2D703A),Color(0xFF8BDA99)),
    ROSE("Розовый",Color(0xFFB02E64),Color(0xFFFFA2C5)),
    ORANGE("Оранжевый",Color(0xFF9A4F00),Color(0xFFFFB76C));
}
object AccentColors{
    private val current=MutableStateFlow(AccentColor.PURPLE)
    val selected=current.asStateFlow()
    fun initialize(context:Context){current.value=runCatching{AccentColor.valueOf(context.getSharedPreferences("theme_cache",Context.MODE_PRIVATE).getString("accent","PURPLE")!!)}.getOrDefault(AccentColor.PURPLE)}
    fun select(context:Context,color:AccentColor){context.getSharedPreferences("theme_cache",Context.MODE_PRIVATE).edit().putString("accent",color.name).apply();current.value=color}
}
