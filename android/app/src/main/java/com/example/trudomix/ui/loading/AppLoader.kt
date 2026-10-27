package com.example.trudomix.ui.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.trudomix.ui.theme.AppSpacing

@Composable fun AppLoader(modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.primary,strokeWidth:Dp=1.5.dp){
    val transition=rememberInfiniteTransition(label="app-loader")
    val angle=transition.animateFloat(0f,360f,infiniteRepeatable(tween(1400,easing=LinearEasing)),label="loader-orbit")
    Box(modifier.size(40.dp).semantics{contentDescription="Загрузка"}.drawWithCache{
        val stroke=Stroke(strokeWidth.toPx())
        onDrawBehind{drawArc(color.copy(alpha=.2f),0f,360f,false,style=stroke);drawArc(color,angle.value,80f,false,style=stroke)}
    },contentAlignment=Alignment.Center){
        StaticLoaderLogo(MaterialTheme.colorScheme.background.luminance()<.5f,Modifier.fillMaxSize(.65f).aspectRatio(1000f/850f))
    }
}

@Composable fun PageLoadingState(modifier:Modifier=Modifier,label:String="Загружаем…",compact:Boolean=false){
    Column(modifier.fillMaxWidth().heightIn(min=if(compact)160.dp else 240.dp).padding(AppSpacing.lg).semantics{liveRegion=LiveRegionMode.Polite},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(AppSpacing.md,Alignment.CenterVertically)){
        AppLoader(Modifier.size(68.dp),color=MaterialTheme.colorScheme.onBackground)
        Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
    }
}

/** Compact request indicator for lists, sheets and inline sections. */
@Composable fun InlineLoadingState(modifier:Modifier=Modifier){
    Box(modifier.fillMaxWidth().height(32.dp),contentAlignment=Alignment.Center){
        AppLoader(Modifier.size(24.dp))
    }
}
