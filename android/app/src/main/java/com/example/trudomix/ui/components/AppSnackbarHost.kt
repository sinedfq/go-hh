package com.example.trudomix.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** A single themed, accessible style for short in-app messages. */
@Composable
fun AppSnackbarHost(state:SnackbarHostState,modifier:Modifier=Modifier) {
    SnackbarHost(state,modifier) { data ->
        Surface(Modifier.padding(horizontal=16.dp,vertical=12.dp).widthIn(max=560.dp).fillMaxWidth()
            .semantics { liveRegion=LiveRegionMode.Polite; dismiss {data.dismiss();true} },
            shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,
            contentColor=MaterialTheme.colorScheme.onSurface,shadowElevation=6.dp,
            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.padding(start=16.dp,end=4.dp,top=4.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Info,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(22.dp))
                Text(data.visuals.message,Modifier.weight(1f).padding(vertical=10.dp),style=MaterialTheme.typography.bodyMedium)
                data.visuals.actionLabel?.let {label->TextButton(onClick=data::performAction){Text(label)}}
                IconButton(onClick=data::dismiss){Icon(Icons.Default.Close,"Скрыть уведомление")}
            }
        }
    }
}
