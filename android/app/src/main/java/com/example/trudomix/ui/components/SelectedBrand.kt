package com.example.trudomix.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import com.example.trudomix.data.local.AppIcon
import com.example.trudomix.data.local.AppIcons

@Composable
fun selectedBrand():AppIcon {
    val appearance by AppIcons.appearance.collectAsState()
    return AppIcons.forTheme(appearance.icon,MaterialTheme.colorScheme.background.luminance()<.5f,appearance.followTheme)
}
@Composable
fun SelectedBrandImage(description:String?,modifier:Modifier=Modifier){AppIconImage(selectedBrand().resource,description,modifier)}
