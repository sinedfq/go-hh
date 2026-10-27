package com.example.trudomix.ui.loading

import com.example.trudomix.data.local.LoadingStyle

/** Timings from trudomix_three_loading_styles.html. */
object LoadingStyleMotion{
    const val duration=3.7f
    data class Frame(val trace:Float,val orbTrace:Float,val fill:Float,val glow:Float,val ring:Float,val ringAlpha:Float,val shutter:Float)
    private fun unit(v:Float)=v.coerceIn(0f,1f)
    private fun envelope(t:Float,start:Float,length:Float):Float{
        val u=unit((t-start)/length)
        return SplashTiming.smooth(u/.35f)*(1-SplashTiming.smooth((u-.35f)/.65f))
    }
    fun frame(time:Float,style:LoadingStyle):Frame{
        val t=time.coerceAtLeast(0f)
        return when(style){
            LoadingStyle.IMPACT->{
                val ring=unit((t-2f)/1.35f)
                Frame(SplashTiming.smooth(t/1.65f),SplashTiming.smooth((t-1.1f)/.8f),SplashTiming.smooth((t-1.5f)/.5f),envelope(t,2f,1.5f),ring,if(ring<=0f||ring>=1f)0f else .9f*SplashTiming.smooth(ring/.15f)*(1-SplashTiming.smooth((ring-.15f)/.75f)),1f)
            }
            LoadingStyle.NEON->{
                val u=unit((t-1.65f)/.85f)
                val flicker=when{u<=0f->0f;u<.25f->0f;u<.33f->1f;u<.5f->.15f;u<.63f->1f;u<1f->.3f;else->1f}
                Frame(unit(t/1.5f),unit((t-.25f)/1.3f),flicker,envelope(t,1.7f,2f),0f,0f,1f)
            }
            LoadingStyle.SHUTTER->Frame(0f,0f,1f,envelope(t,1.5f,1.5f),0f,0f,SplashTiming.smooth((t-.5f)/1.3f))
        }
    }
}
