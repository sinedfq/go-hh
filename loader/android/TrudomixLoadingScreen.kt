// Compose example: integrate with the existing navigation and settings repository.
// Use the selected logo resource ID and the current theme from DataStore.
// Replace R.drawable.trudomix_logo_default with your chosen drawable resources.
package com.example.trudomix.ui.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrudomixLoadingScreen(
    selectedLogoRes: Int,
    darkTheme: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (darkTheme) Color(0xFF101419) else Color(0xFFF6F8FC)
    val foreground = if (darkTheme) Color.White else Color(0xFF151B29)
    val accent = Color(0xFF8651F7)
    val transition = rememberInfiniteTransition(label = "logo")
    val pulse by transition.animateFloat(
        initialValue = .94f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = modifier.fillMaxSize().background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(selectedLogoRes),
            contentDescription = "Логотип Трудомикс",
            modifier = Modifier.size(116.dp).scale(pulse)
        )
        Spacer(Modifier.height(18.dp))
        Text("Трудомикс", fontSize = 26.sp, color = foreground)
        Spacer(Modifier.height(10.dp))
        Text("Подбираем возможности для вас", color = foreground.copy(alpha = .65f), fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        if (loading) LinearProgressIndicator(
            modifier = Modifier.width(190.dp).height(3.dp),
            color = accent,
            trackColor = foreground.copy(alpha = .12f)
        )
    }
}
// In app navigation, display while initial app state is Loading;
// navigate to Auth or Home once ready, rather than waiting a fixed duration.
