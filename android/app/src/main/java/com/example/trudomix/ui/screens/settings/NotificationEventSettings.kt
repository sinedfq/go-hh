package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.trudomix.push.*

@Composable fun NotificationEventSettings(masterEnabled:Boolean){
    val context=LocalContext.current
    val values by NotificationPreferences.events.collectAsState()
    Text("Какие события показывать",style=MaterialTheme.typography.titleSmall)
    NotificationEvent.entries.forEach{event->
        val checked=values[event]?:NotificationPreferences.eventEnabled(context,event)
        Row(Modifier.fillMaxWidth().heightIn(min=76.dp).toggleable(checked,role=Role.Switch,onValueChange={NotificationPreferences.setEvent(context,event,it)}).semantics{contentDescription="Уведомления: ${event.title}"},verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f).padding(end=12.dp)){
                Text(event.title,style=MaterialTheme.typography.bodyLarge)
                Text(event.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SettingsToggle(checked)
        }
    }
    Text(if(masterEnabled)"Выбор сохраняется на этом устройстве. Код регистрации или входа приходит отдельно, только по вашему запросу."else "Можно настроить события сейчас. Чтобы они приходили, включите «Получать уведомления» выше.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
