package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.data.local.NavAnimationStyle
import com.example.trudomix.ui.screens.settings.NavAnimationPicker
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class NavAnimationPickerTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun everyChoiceAppliesImmediatelyAndPreviewKeepsExistingTabs(){
        val selected=mutableStateOf(NavAnimationStyle.SMOOTH)
        compose.setContent{TrudomixTheme{NavAnimationPicker(selected.value,{selected.value=it})}}
        NavAnimationStyle.entries.forEach{style->
            compose.onNodeWithText(style.title).performClick()
            compose.runOnIdle{assertEquals(style,selected.value)}
        }
        compose.onNodeWithText(NavAnimationStyle.CENTER_REVEAL.title).performClick()
        for(label in listOf("Главная","Свайпы","Поиск","Отклики","Чаты"))compose.onNodeWithContentDescription(label).assertHasClickAction().performClick()
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null),"nav-animation-picker.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
}
