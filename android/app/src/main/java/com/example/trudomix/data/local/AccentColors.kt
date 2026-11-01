package com.example.trudomix.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
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
    private val customColor=MutableStateFlow<Color?>(null)
    val custom=customColor.asStateFlow()
    fun initialize(context:Context){val prefs=context.getSharedPreferences("theme_cache",Context.MODE_PRIVATE);current.value=runCatching{AccentColor.valueOf(prefs.getString("accent","PURPLE")!!)}.getOrDefault(AccentColor.PURPLE);customColor.value=parseAccentHex(prefs.getString("custom_accent",null))}
    fun select(context:Context,color:AccentColor){context.getSharedPreferences("theme_cache",Context.MODE_PRIVATE).edit().putString("accent",color.name).remove("custom_accent").apply();customColor.value=null;current.value=color}
    fun selectCustom(context:Context,color:Color){val opaque=color.copy(alpha=1f);context.getSharedPreferences("theme_cache",Context.MODE_PRIVATE).edit().putString("custom_accent",accentHex(opaque)).apply();customColor.value=opaque}
}

fun parseAccentHex(value:String?):Color?=value?.trim()?.removePrefix("#")?.takeIf{it.length==6&&it.all{c->c.digitToIntOrNull(16)!=null}}?.toLongOrNull(16)?.let{Color((it or 0xFF000000L).toInt())}
fun accentHex(color:Color)="#"+(color.toArgb() and 0xFFFFFF).toString(16).padStart(6,'0').uppercase()
private fun contrast(first:Color,second:Color):Float{val a=first.luminance();val b=second.luminance();return (maxOf(a,b)+.05f)/(minOf(a,b)+.05f)}
fun readableAccent(color:Color,dark:Boolean):Color{
    val background=if(dark)Color(0xFF080816)else Color(0xFFF8F7FC)
    val end=if(dark)Color.White else Color.Black
    for(step in 0..20){val candidate=lerp(color,end,step/20f);if(contrast(candidate,background)>=4.5f)return candidate}
    return end
}
fun accentForeground(color:Color):Color=if(contrast(color,Color.Black)>=contrast(color,Color.White))Color.Black else Color.White
