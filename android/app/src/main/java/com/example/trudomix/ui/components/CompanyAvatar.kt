package com.example.trudomix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.trudomix.data.api.ApiClient

@Composable
fun CompanyAvatar(logo: String?, name: String, size: Dp = 48.dp) {
    val url=logo?.let(ApiClient::mediaUrl)
    var failed by remember(url) { mutableStateOf(false) }
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center) {
        if(url==null || failed) Icon(Icons.Default.Business,contentDescription="Компания: $name",tint=MaterialTheme.colorScheme.onPrimaryContainer,modifier=Modifier.size(size/2))
        else AsyncImage(url,contentDescription="Логотип: $name",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit,alignment=Alignment.Center,onError={failed=true})
    }
}
