package com.example.trudomix.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Continuous decoration is opt-in; the default navigation does no idle frame work. */
object VisualPerformance {
    private val motion=MutableStateFlow(false)
    val searchMotion=motion.asStateFlow()
    fun initialize(context:Context){motion.value=context.getSharedPreferences("visual_performance",Context.MODE_PRIVATE).getBoolean("search_motion",false)}
    fun setSearchMotion(context:Context,enabled:Boolean){context.getSharedPreferences("visual_performance",Context.MODE_PRIVATE).edit().putBoolean("search_motion",enabled).apply();motion.value=enabled}
}
