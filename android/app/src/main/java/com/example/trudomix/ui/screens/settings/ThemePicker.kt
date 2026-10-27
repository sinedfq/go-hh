package com.example.trudomix.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.example.trudomix.data.local.AppThemeMode
import com.example.trudomix.ui.loading.StaticLoaderLogo
import com.example.trudomix.ui.theme.LocalThemeWave

@Composable fun ThemePicker(mode:AppThemeMode,onSelect:(AppThemeMode)->Unit){
    val reveal=LocalThemeWave.current
    val systemDark=android.content.res.Resources.getSystem().configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    val dark=MaterialTheme.colorScheme.background.luminance()<.5f
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Тема оформления",style=MaterialTheme.typography.titleMedium)
            Text("Выберите оформление и посмотрите пример",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(AppThemeMode.SYSTEM to "Системная",AppThemeMode.LIGHT to "Светлая",AppThemeMode.DARK to "Тёмная").forEach{(choice,title)->
                val previewDark=when(choice){AppThemeMode.SYSTEM->systemDark;AppThemeMode.DARK->true;else->false}
                var origin by remember{mutableStateOf(Offset.Zero)}
                OutlinedCard(onClick={if(mode!=choice){if(dark==previewDark)onSelect(choice)else reveal(origin){onSelect(choice)}}},modifier=Modifier.fillMaxWidth().onGloballyPositioned{origin=it.positionInWindow()+Offset(it.size.width/2f,it.size.height/2f)},colors=CardDefaults.outlinedCardColors(containerColor=if(choice==mode)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)){
                    Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        RadioButton(selected=choice==mode,onClick=null)
                        Column(Modifier.weight(1f)){
                            Text(title,style=MaterialTheme.typography.bodyLarge)
                            if(choice==AppThemeMode.SYSTEM)Text("Следует настройкам устройства",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ThemeThumbnail(previewDark)
                    }
                }
            }
        }
    }
}

@Composable private fun ThemeThumbnail(dark:Boolean){
    val background=if(dark)Color(0xFF080816)else Color(0xFFF8F7FC)
    val surface=if(dark)Color(0xFF1B1B30)else Color.White
    val text=if(dark)Color(0xFFFAF8FF)else Color(0xFF191431)
    Column(Modifier.size(104.dp,66.dp).clip(RoundedCornerShape(10.dp)).background(background).padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
            StaticLoaderLogo(dark,Modifier.size(18.dp,15.3.dp))
            Box(Modifier.width(42.dp).height(4.dp).background(text,RoundedCornerShape(2.dp)))
        }
        Column(Modifier.fillMaxWidth().weight(1f).background(surface,RoundedCornerShape(5.dp)).padding(5.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
            Box(Modifier.width(40.dp).height(3.dp).background(text.copy(alpha=.7f),RoundedCornerShape(2.dp)))
            Box(Modifier.width(56.dp).height(3.dp).background(Color(0xFF9474ED),RoundedCornerShape(2.dp)))
        }
    }
}
