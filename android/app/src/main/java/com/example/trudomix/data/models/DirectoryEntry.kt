package com.example.trudomix.data.models

import kotlinx.serialization.Serializable

@Serializable
data class DirectoryEntry(val name: String, val id: Int = 0)
