package com.example.trudomix.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.trudomix.data.local.NavAnimationStyle
import com.example.trudomix.data.local.ThemeManager

@Composable
private fun Modifier.glass(shape:Shape,highlight:NavHighlightState):Modifier {
    val colors=MaterialTheme.colorScheme
    // Translucent tint preserves the capsule without capturing/blurring the scrolling screen.
    return shadow(3.dp,shape,clip=false).navHighlight(highlight,colors.surface.copy(alpha=.94f),Color(0xFF70C9FF).copy(alpha=.22f),colors.outline)
}

@Composable
fun GlassNavigation(state:HazeState,selected:Int,unread:Int,onSelect:(Int)->Unit,onMap:()->Unit,modifier:Modifier=Modifier,employer:Boolean=false,animationStyle:NavAnimationStyle?=null) {
    val labels=if(employer)listOf("Главная","Компания","Вакансии","Отклики","Чаты")else listOf("Главная","Свайпы","Поиск","Отклики","Чаты")
    val destinations=if(employer)listOf(4,6,0,1,2)else listOf(4,7,0,1,2)
    val style=animationStyle?:run{
        val context=LocalContext.current
        val preferences=remember(context){ThemeManager(context.applicationContext)}
        val saved by preferences.navAnimationStyleFlow.collectAsStateWithLifecycle(initialValue=NavAnimationStyle.SMOOTH)
        saved
    }
    val target=if(selected==5)0 else destinations.indexOf(selected)
    val highlight=rememberNavHighlight(target,style)
    val showLabels = LocalConfiguration.current.screenWidthDp >= 360 && LocalDensity.current.fontScale <= 1.3f
    val icons=listOf(Icons.Default.Home,if(employer)Icons.Default.Business else Icons.Default.Swipe,if(employer)Icons.Default.WorkOutline else Icons.Default.Search,Icons.Default.Assignment,Icons.Default.ChatBubbleOutline)
    Row(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=12.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        // This hit target absorbs empty-space taps; children retain their own clicks.
        Row(Modifier.weight(1f).height(if(showLabels) 72.dp else 60.dp).glass(CircleShape,highlight).pointerInput(Unit){detectTapGestures{}},verticalAlignment=Alignment.CenterVertically) {
            labels.forEachIndexed { index,label ->
                if(index==2){
                    Column(Modifier.weight(1.15f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                        if(employer)FilledIconButton(onClick={onSelect(0)},modifier=Modifier.size(56.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color(0xFF3478F6),contentColor=Color.White)){Icon(icons[index],label,Modifier.size(30.dp))}
                        else DotSearchButton(onClick={onSelect(0)},label=label)
                    }
                }else{
                val color by animateColorAsState(if(selected==destinations[index]||(selected==5&&index==0)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,animationSpec=if(style==NavAnimationStyle.NONE)snap()else spring(),label="tab")
                Column(Modifier.weight(1f).fillMaxHeight().clickable(interactionSource=remember{MutableInteractionSource()},indication=if(style==NavAnimationStyle.CENTER_REVEAL||style==NavAnimationStyle.NONE)null else LocalIndication.current) { onSelect(destinations[index]) },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Box(Modifier.size(40.dp),contentAlignment=Alignment.Center) {
                        BadgedBox(badge={if(index==4 && unread>0) Badge{Text(if(unread>99) "99+" else unread.toString())}}){Icon(icons[index],label,tint=color)}
                    }
                    if(showLabels) Text(label,style=MaterialTheme.typography.labelSmall,color=color,maxLines=1)
                }
                }
            }
        }
    }
}
