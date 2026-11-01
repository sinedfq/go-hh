package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.local.NavAnimationStyle
import com.example.trudomix.ui.components.GlassNavigation
import dev.chrisbanes.haze.HazeState

@Composable fun NavAnimationPicker(selected:NavAnimationStyle,onSelect:(NavAnimationStyle)->Unit){
    var previewTab by remember{mutableIntStateOf(4)}
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Анимация нижней навигации",style=MaterialTheme.typography.titleMedium)
            NavAnimationStyle.entries.forEach{style->
                OutlinedCard(onClick={onSelect(style)},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=if(style==selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)){
                    Row(Modifier.padding(8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                        RadioButton(selected==style,onClick=null)
                        Text(style.title,style=MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Text("Нажимайте вкладки в примере. Выбор применяется сразу и сохраняется.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            GlassNavigation(remember{HazeState()},previewTab,0,{previewTab=it},{},animationStyle=selected)
        }
    }
}
