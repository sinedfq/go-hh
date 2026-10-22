package com.example.trudomix.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ResumeEventBus {
    private val _resumeUpdatedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resumeUpdatedEvent: SharedFlow<Unit> = _resumeUpdatedEvent.asSharedFlow()

    fun notifyResumeUpdated() {
        _resumeUpdatedEvent.tryEmit(Unit)
    }
}
