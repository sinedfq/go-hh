package com.example.trudomix

import com.example.trudomix.ui.loading.SplashTiming
import org.junit.Assert.*
import org.junit.Test

class SplashTimingTest{
    @Test fun lightThreadTailFollowsHeadAndVanishesAtEndOfLineStage(){
        for(corner in 0..3)for(line in 0..2){
            val delay=line*.08f+corner*.035f
            var head=0f;var tail=0f
            for(step in 0..330){
                val segment=SplashTiming.lineWindow(step/100f,delay)
                assertTrue(segment.head>=head&&segment.tail>=tail&&segment.tail<=segment.head)
                head=segment.head;tail=segment.tail
            }
            val longThread=SplashTiming.lineWindow(delay+1.3f,delay)
            assertTrue(longThread.head-longThread.tail>.55f)
            assertEquals(0f,SplashTiming.lineWindow(delay+.1f,delay).tail,0f)
            assertEquals(1f,SplashTiming.lineWindow(2.8f,delay).head,0f)
            assertEquals(1f,SplashTiming.lineWindow(2.8f,delay).tail,0f)
            assertEquals(0f,SplashTiming.lineWindow(2.8f,delay).alpha,0f)
        }
    }
    @Test fun referenceSequenceWaitsBeforeAppearanceAndEndsOnce(){
        assertEquals(0f,SplashTiming.logo(0f),0f)
        assertEquals(0f,SplashTiming.logo(3.7f),0f)
        assertEquals(1f,SplashTiming.logo(5.5f),.0001f)
        assertEquals(0f,SplashTiming.wordmark(2.6f),0f)
        assertEquals(1f,SplashTiming.wordmark(3.9f),.0001f)
        assertTrue(SplashTiming.ringAlpha(8f)>0)
        assertEquals(0f,SplashTiming.ringAlpha(9.8f),0f)
        assertEquals(1f,SplashTiming.dockScale(20f),.0001f)
        assertEquals("ready",SplashTiming.stage(SplashTiming.duration))
        assertEquals("ready",SplashTiming.stage(20f))
    }
    @Test fun responsiveLogoFitsShortEdge(){
        for((w,h) in listOf(390f to 844f,1600f to 900f,844f to 390f)){
            val sign=SplashTiming.logoSize(w,h)
            assertTrue(sign<=minOf(w,h)*.8f)
            assertTrue(sign in 100f..310f)
        }
    }
}
