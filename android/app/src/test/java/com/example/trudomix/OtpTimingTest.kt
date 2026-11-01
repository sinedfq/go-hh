package com.example.trudomix

import com.example.trudomix.ui.screens.auth.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import org.junit.Assert.*
import org.junit.Test

class OtpTimingTest{
    @Test fun cooldownAlwaysEmitsZeroIncludingRateLimitOnlyClock()=runBlocking{
        for(deadline in listOf(1L,999L,1000L,1001L,60000L)){
            var time=0L
            val ticks=otpClockTicks({deadline},{time}){time+=it}.toList()
            assertEquals(0,otpSecondsRemaining(deadline,ticks.last()))
            assertEquals(deadline,ticks.last())
            assertTrue(ticks.zipWithNext().all{(a,b)->b>a})
        }
    }
    @Test fun aDelayedFramePastDeadlineCannotLeaveOneSecond()=runBlocking{
        var time=0L
        val values=otpClockTicks({1000L},{time}){time+=1500}.toList().map{otpSecondsRemaining(1000,it)}
        assertEquals(listOf(1,0),values)
    }
}
