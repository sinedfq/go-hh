package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.local.LoadingStyles
import com.example.trudomix.data.local.LoadingStyle
import com.example.trudomix.ui.loading.LoadingStylePreview

@Composable fun LoadingStylePicker(){
    val context=LocalContext.current
    val selected by LoadingStyles.style.collectAsState()
    var replay by remember{mutableIntStateOf(0)}
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Стиль загрузки",style=MaterialTheme.typography.titleMedium)
            Text("Применяется при следующем запуске приложения",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            LoadingStyle.entries.forEach{style->
                OutlinedCard(onClick={LoadingStyles.select(context,style);replay++},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=if(style==selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)){
                    Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                        RadioButton(selected=selected==style,onClick=null)
                        Column(Modifier.weight(1f)){
                            Text(style.title,style=MaterialTheme.typography.bodyLarge)
                            Text(style.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            LoadingStylePreview(selected,replay,Modifier.fillMaxWidth().height(220.dp))
            TextButton(onClick={replay++},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Повторить анимацию")}
        }
    }
}
