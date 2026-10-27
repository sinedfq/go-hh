package com.example.trudomix.ui.loading

import kotlin.math.min

object SplashTiming {
    const val duration=LoadingStyleMotion.duration
    val lineColors=listOf(0xFF8257E6,0xFF5B43E1,0xFF6333F6,0xFF9474ED,0xFF9653F5,0xFF7965DD,0xFF5578DA,0xFFA879FF,0xFF7050E8,0xFFB57FC9,0xFF5C3DDD,0xFF8B6CE3)
    fun smooth(value:Float):Float {val x=value.coerceIn(0f,1f);return x*x*(3-2*x)}
    fun logo(t:Float)=smooth((t-3.7f)/1.8f)
    fun wordmark(t:Float)=smooth((t-2.6f)/1f)
    fun ringProgress(t:Float)=((t-7.22f)/1.9f).coerceIn(0f,1f)
    fun ringAlpha(t:Float):Float {val u=ringProgress(t);return if(u<=0f||u>=1f)0f else .78f*smooth((u-.12f)/.18f)*(1-smooth((u-.3f)/.7f))}
    fun logoSize(width:Float,height:Float)=(min(width,height)*.55f).coerceIn(100f,310f)
    fun dockScale(t:Float):Float{
        val u=((t-7.1f)/1.45f).coerceIn(0f,1f)
        return when{u<.18f->1-.065f*smooth(u/.18f);u<.38f->.935f;u<.56f->.935f+.1f*smooth((u-.38f)/.18f);else->1.035f-.035f*smooth((u-.56f)/.44f)}
    }
    fun stage(t:Float)=when{t>=duration->"ready";t>=2.8f->"wordmark";t>=2.2f->"wave";t>=1.3f->"orb";else->"outline"}
    data class Window(val head:Float,val tail:Float,val alpha:Float)
    fun lineWindow(t:Float,delay:Float):Window{
        val u=((t-delay)/2.5f).coerceIn(0f,1f);val head=smooth(u/.87f)
        return Window(head,(head-.72f*(1-smooth((u-.45f)/.55f))).coerceAtLeast(0f),smooth(u/.07f)*(1-smooth((u-.85f)/.15f)))
    }
}
