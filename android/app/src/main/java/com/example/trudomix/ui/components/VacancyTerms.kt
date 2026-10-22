package com.example.trudomix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.example.trudomix.data.models.*

@Composable
fun ITAccreditationBadge() {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(Modifier.wrapContentSize()) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { open=true }.semantics { contentDescription="IT-аккредитация указана компанией" },contentAlignment=Alignment.Center) {
            Text("IT",fontSize=9.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer)
        }
        DropdownMenu(expanded=open,onDismissRequest={open=false}) {
            Column(Modifier.widthIn(max=270.dp).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("IT-аккредитация",style=MaterialTheme.typography.titleSmall)
                Text("Компания имеет статус аккредитованной IT-компании.",style=MaterialTheme.typography.bodyMedium)
                Text("Сведения указаны компанией.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VacancyTerms(vacancy:Vacancy) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(vacancySalary(vacancy),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(vacancy.location.isNotBlank()) VacancyMeta(Icons.Default.LocationOn,vacancy.location)
            if(vacancy.experience.isNotBlank()) VacancyMeta(Icons.Default.Work,vacancy.experience)
            VacancyMeta(Icons.Default.HomeWork,if(vacancy.remote) "Удалённо" else "В офисе")
        }
        vacancyConditions(vacancy).takeIf{it.isNotBlank()}?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun VacancyMeta(icon:ImageVector,label:String) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        Icon(icon,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
