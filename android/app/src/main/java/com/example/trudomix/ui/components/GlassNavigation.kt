package com.example.trudomix.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.*

@Composable
private fun Modifier.glass(state:HazeState,shape:Shape,blue:Boolean=false):Modifier {
    val colors=MaterialTheme.colorScheme
    val tint=if(blue) colors.primary.copy(alpha=.22f) else colors.surface.copy(alpha=.27f)
    return shadow(12.dp,shape).clip(shape).hazeChild(state) {
        backgroundColor=colors.surface
        tints=listOf(HazeTint(tint))
        blurRadius=14.dp
        noiseFactor=.008f
        fallbackTint=HazeTint(colors.surface.copy(alpha=.96f))
    }.background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.08f),Color.Transparent)))
        .border(1.dp,Brush.verticalGradient(listOf(Color.White.copy(alpha=.36f),colors.outline.copy(alpha=.45f))),shape)
}

@Composable
fun GlassNavigation(state:HazeState,selected:Int,unread:Int,onSelect:(Int)->Unit,onMap:()->Unit,modifier:Modifier=Modifier,employer:Boolean=false) {
    val labels=listOf("Главная",if(employer)"Вакансии"else "Поиск","Отклики","Чаты")
    val destinations=listOf(4,0,1,2)
    val showLabels = LocalConfiguration.current.screenWidthDp >= 360 && LocalDensity.current.fontScale <= 1.3f
    val icons=listOf(Icons.Default.Home,if(employer)Icons.Default.WorkOutline else Icons.Default.Search,Icons.Default.Assignment,Icons.Default.ChatBubbleOutline)
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=12.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        // This hit target absorbs empty-space taps; children retain their own clicks.
        Row(Modifier.weight(1f).height(if(showLabels) 72.dp else 60.dp).glass(state,CircleShape).pointerInput(Unit){detectTapGestures{}},verticalAlignment=Alignment.CenterVertically) {
            labels.forEachIndexed { index,label ->
                val color by animateColorAsState(if(selected==destinations[index]||(selected==5&&index==0)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,label="tab")
                Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(destinations[index]) },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Box(Modifier.size(40.dp),contentAlignment=Alignment.Center) {
                        BadgedBox(badge={if(index==3 && unread>0) Badge{Text(if(unread>99) "99+" else unread.toString())}}){Icon(icons[index],label,tint=color)}
                    }
                    if(showLabels) Text(label,style=MaterialTheme.typography.labelSmall,color=color,maxLines=1)
                }
            }
        }
        IconButton(onClick={if(employer)onSelect(6)else onMap()},modifier=Modifier.size(60.dp).glass(state,CircleShape,true)) {
            Icon(if(employer)Icons.Default.Business else Icons.Default.Map,if(employer)"Моя компания"else "Карта вакансий",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(28.dp))
        }
    }
}
