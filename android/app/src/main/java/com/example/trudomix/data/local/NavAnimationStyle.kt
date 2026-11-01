package com.example.trudomix.data.local

enum class NavAnimationStyle(val id:String,val title:String){
    SMOOTH("smooth","Мягкая волна"),
    ELASTIC("elastic","Упругая волна"),
    CENTER_REVEAL("center_reveal","Раскрытие из центра"),
    NONE("none","Без анимации");
    companion object{fun fromId(id:String?)=entries.firstOrNull{it.id==id}?:SMOOTH}
}
