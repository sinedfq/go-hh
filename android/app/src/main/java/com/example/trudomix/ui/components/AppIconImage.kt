package com.example.trudomix.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun AppIconImage(resource: Int, description: String?, modifier: Modifier = Modifier) {
    Image(painterResource(resource), description, modifier)
}
