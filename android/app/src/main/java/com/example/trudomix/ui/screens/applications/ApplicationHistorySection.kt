package com.example.trudomix.ui.screens.applications

import com.example.trudomix.ui.loading.InlineLoadingState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun applicationEventDate(value: String?, zone: ZoneId = ZoneId.systemDefault()): String =
    if (value.isNullOrBlank()) "Дата не указана" else runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm",Locale.forLanguageTag("ru-RU")))
    }.getOrDefault(value.take(32))

@Composable
fun ApplicationDetailsSummary(feedback: String, interviewAt: String?) {
    if (feedback.isNotBlank()) {
        Text("Комментарий компании",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
        Text(feedback,style=MaterialTheme.typography.bodyMedium)
    }
    if (!interviewAt.isNullOrBlank()) {
        Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Event,null,tint=MaterialTheme.colorScheme.onSecondaryContainer)
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("Собеседование",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(applicationEventDate(interviewAt),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Время на твоём устройстве",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
fun ApplicationHistorySection(applicationId: Int, state: ApplicationHistoryState?, onLoad: () -> Unit) {
    var expanded by rememberSaveable(applicationId) { mutableStateOf(false) }
    LaunchedEffect(expanded,state == null) { if (expanded && state == null) onLoad() }
    Column {
        TextButton(onClick={expanded=!expanded}) {
            Icon(Icons.Default.History,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(if(expanded) "Скрыть историю" else "История отклика")
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                when(state) {
                    null, ApplicationHistoryState.Loading -> InlineLoadingState()
                    is ApplicationHistoryState.Error -> {
                        Text(state.message,color=MaterialTheme.colorScheme.error)
                        TextButton(onClick=onLoad){Text("Повторить загрузку истории")}
                    }
                    is ApplicationHistoryState.Success -> {
                        if(state.events.isEmpty()) Text("Изменений пока нет",style=MaterialTheme.typography.bodySmall)
                        state.events.asReversed().forEach { event ->
                            Surface(color=MaterialTheme.colorScheme.surfaceContainerLow,shape=RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text(applicationLabel(event.status),style=MaterialTheme.typography.titleSmall)
                                    Text(applicationEventDate(event.created_at),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    if(event.feedback.isNotBlank()) Text(event.feedback,style=MaterialTheme.typography.bodyMedium)
                                    if(!event.interview_at.isNullOrBlank()) Text("Собеседование: ${applicationEventDate(event.interview_at)}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
