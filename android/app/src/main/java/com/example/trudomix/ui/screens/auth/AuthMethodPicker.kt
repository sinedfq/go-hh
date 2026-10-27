package com.example.trudomix.ui.screens.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable fun AuthMethodPicker(phone:Boolean,onSelect:(Boolean)->Unit,enabled:Boolean=true){
    BoxWithConstraints(Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(14.dp)).padding(4.dp)){
        val half=maxWidth/2
        val position by animateDpAsState(if(phone)half else 0.dp,tween(240),label="auth-method-slider")
        Box(Modifier.offset(x=position).width(half).fillMaxHeight().background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(11.dp)))
        Row(Modifier.fillMaxSize().selectableGroup()){
            listOf(false to "Почта",true to "Телефон").forEach{(value,label)->
                Box(Modifier.weight(1f).fillMaxHeight().selectable(selected=phone==value,enabled=enabled,role=Role.Tab,onClick={onSelect(value)}),contentAlignment=Alignment.Center){Text(label,color=if(phone==value)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelLarge)}
            }
        }
    }
}
