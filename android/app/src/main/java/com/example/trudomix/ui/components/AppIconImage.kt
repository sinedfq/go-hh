package com.example.trudomix.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage

@Composable
fun AppIconImage(resource: Int, description: String?, modifier: Modifier = Modifier) {
    AsyncImage(model=resource, contentDescription=description, modifier=modifier)
}
