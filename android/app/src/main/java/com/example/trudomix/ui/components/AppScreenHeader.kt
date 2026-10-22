package com.example.trudomix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

/** Shared typography, background and spacing, including the search screen. */
@Composable
fun AppScreenHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    subtitle: String? = null,
    applyInsets: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    containerColor: Color = MaterialTheme.colorScheme.background
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().background(containerColor).then(if (applyInsets) Modifier.statusBarsPadding() else Modifier), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 1200.dp).fillMaxWidth().padding(contentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                navigationIcon()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TRUDOMIX / КАРЬЕРА", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.primary)
                    CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
                        ProvideTextStyle(MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)) { title() }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                ProfileAction()
            }
            subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
        }
    }
}
