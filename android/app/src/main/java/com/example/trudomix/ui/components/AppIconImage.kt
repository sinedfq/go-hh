package com.example.trudomix.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

@Composable
fun AppIconImage(resource: Int, description: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(resource) { ContextCompat.getDrawable(context, resource)!!.toBitmap(256,256).asImageBitmap() }
    Image(bitmap, description, modifier.clip(RoundedCornerShape(24)))
}
