package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.ui.screens.auth.CountryPhoneField
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CountryPhoneTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun wheelChangesPrefixAndPastedInternationalNumberIsNotDuplicated(){
        val phone=mutableStateOf("+7")
        compose.setContent{TrudomixTheme{Column(Modifier.fillMaxSize()){CountryPhoneField(phone.value,{phone.value=it},true)}}}
        compose.onNodeWithText("🇷🇺 +7 ▾").performClick()
        compose.onNodeWithContentDescription("Коды стран").performScrollToNode(hasText("Великобритания"))
        compose.onNodeWithText("Великобритания").performClick()
        compose.onNodeWithText("🇬🇧 +44 ▾").assertExists()
        compose.onNodeWithText("Номер телефона").performTextReplacement("+442071234567")
        assertEquals("+442071234567",phone.value)
    }
}
