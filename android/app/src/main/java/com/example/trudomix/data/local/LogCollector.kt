package com.example.trudomix.data.local

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogCollector {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(tag: String, message: String, isError: Boolean = false) {
        val timestamp = dateFormat.format(Date())
        val prefix = if (isError) "[ERROR]" else "[INFO]"
        val entry = "$timestamp $prefix [$tag] $message"

        if (isError) {
            Log.e(tag, message)
        } else {
            Log.d(tag, message)
        }

        _logs.value = _logs.value + entry
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
