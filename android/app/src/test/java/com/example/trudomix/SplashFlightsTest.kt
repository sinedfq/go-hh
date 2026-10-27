package com.example.trudomix

import com.example.trudomix.ui.loading.SplashFlights
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SplashFlightsTest {
    @Test fun broadFlightsStayDirectedToOneCenterAndChangeWithSeed(){
        for((w,h) in listOf(360f to 800f,430f to 932f,768f to 1024f,1280f to 900f,1920f to 1080f)){
            for(index in 0..11){
                val curves=SplashFlights.curves(w,h,123,index,12+min(w,h)*.018f,4f)
                val a=curves.first().start;val end=curves.last().end
                assertEquals(w/2,end.x,0f);assertEquals(h/2,end.y,0f)
                assertTrue(a.x<0||a.x>w);assertTrue(a.y<0||a.y>h)
                val dx=end.x-a.x;val dy=end.y-a.y;val length=hypot(dx,dy)
                var forward=-Float.MAX_VALUE;var swing=0f
                for(curve in curves)for(point in listOf(curve.start,curve.first,curve.second,curve.end)){
                    val progress=((point.x-a.x)*dx+(point.y-a.y)*dy)/length
                    assertTrue(progress>=forward-.001f);forward=progress
                    swing=max(swing,abs((point.x-a.x)*dy-(point.y-a.y)*dx)/length)
                }
                assertTrue(swing>min(w,h)*.1f)
                assertEquals(curves,SplashFlights.curves(w,h,123,index,12+min(w,h)*.018f,4f))
                assertNotEquals(curves,SplashFlights.curves(w,h,321,index,12+min(w,h)*.018f,4f))
            }
        }
    }
}
