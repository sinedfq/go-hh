package com.example.trudomix.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationEvent(
    val id: Int,
    val status: String,
    val feedback: String = "",
    val interview_at: String? = null,
    val created_at: String? = null
)
