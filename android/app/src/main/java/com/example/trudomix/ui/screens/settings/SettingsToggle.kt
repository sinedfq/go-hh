package com.example.trudomix.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics

@Composable internal fun SettingsToggle(checked:Boolean){
    Switch(checked=checked,onCheckedChange=null,modifier=Modifier.clearAndSetSemantics{},
        thumbContent=if(checked){{Icon(Icons.Default.Check,null,tint=MaterialTheme.colorScheme.primary)}}else null,
        colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=MaterialTheme.colorScheme.primary,checkedBorderColor=MaterialTheme.colorScheme.primary,uncheckedThumbColor=MaterialTheme.colorScheme.onSurfaceVariant,uncheckedTrackColor=MaterialTheme.colorScheme.surfaceVariant,uncheckedBorderColor=MaterialTheme.colorScheme.outline))
}
