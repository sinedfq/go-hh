package com.example.trudomix.data.local

import com.example.trudomix.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow

object SessionIdentity { val user=MutableStateFlow<User?>(null) }
