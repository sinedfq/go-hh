package com.example.trudomix.ui.screens.auth

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow

fun otpSecondsRemaining(deadline:Long,now:Long):Int=((deadline-now).coerceAtLeast(0)+999).div(1000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

fun otpClockTicks(deadline:()->Long,now:()->Long,wait:suspend(Long)->Unit={delay(it)})=flow{
    while(currentCoroutineContext().isActive){
        val time=now();emit(time)
        val remaining=deadline()-time
        if(remaining<=0)break
        wait(minOf(250L,remaining).coerceAtLeast(1))
    }
}
