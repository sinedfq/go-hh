package com.example.trudomix

import com.example.trudomix.data.local.LoadingStyle
import com.example.trudomix.ui.loading.LoadingStyleMotion
import org.junit.Assert.*
import org.junit.Test

class LoadingStyleMotionTest{
    @Test fun unknownSavedStyleFallsBackAndAllStylesFinishWithoutRepeating(){
        assertEquals(LoadingStyle.IMPACT,LoadingStyle.fromId("removed-style"))
        for(style in LoadingStyle.entries){
            assertEquals(style,LoadingStyle.fromId(style.id))
            val final=LoadingStyleMotion.frame(LoadingStyleMotion.duration,style)
            assertEquals(1f,final.fill,0f)
            assertEquals(0f,final.glow,0f)
            assertEquals(0f,final.ringAlpha,0f)
            assertEquals(final,LoadingStyleMotion.frame(20f,style))
        }
    }
    @Test fun stylesHaveDistinctMotionWithoutAppearanceDelay(){
        assertTrue(LoadingStyleMotion.frame(.1f,LoadingStyle.IMPACT).trace>0f)
        assertEquals(1f,LoadingStyleMotion.frame(0f,LoadingStyle.SHUTTER).fill,0f)
        val impact=LoadingStyleMotion.frame(2.6f,LoadingStyle.IMPACT)
        val neon=LoadingStyleMotion.frame(2.6f,LoadingStyle.NEON)
        assertTrue(impact.ringAlpha>0);assertEquals(0f,neon.ringAlpha,0f)
        assertEquals(0f,LoadingStyleMotion.frame(.5f,LoadingStyle.SHUTTER).shutter,.0001f)
        assertEquals(1f,LoadingStyleMotion.frame(2.1f,LoadingStyle.SHUTTER).shutter,.0001f)
        assertTrue(LoadingStyleMotion.frame(1.95f,LoadingStyle.NEON).fill<1f)
    }
}
