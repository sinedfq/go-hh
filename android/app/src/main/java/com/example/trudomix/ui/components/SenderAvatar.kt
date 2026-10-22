package com.example.trudomix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.trudomix.data.api.ApiClient

fun senderInitials(name: String): String = name.substringBefore('@').trim()
    .split(Regex("[\\s._-]+")) .filter { it.isNotBlank() }.take(2)
    .joinToString("") { it.take(1).uppercase() }.ifBlank { "?" }

@Composable
fun SenderAvatar(photo: String, name: String, modifier: Modifier = Modifier) {
    var failed by remember(photo) { mutableStateOf(false) }
    Surface(modifier.size(36.dp).semantics { contentDescription = "Аватар: $name" }, shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            Text(senderInitials(name), style = MaterialTheme.typography.labelMedium)
            if (photo.isNotBlank() && !failed) AsyncImage(ApiClient.mediaUrl(photo), contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop, onError = { failed = true })
        }
    }
}
