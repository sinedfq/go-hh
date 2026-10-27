package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.ui.screens.auth.*
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Rule
import org.junit.Test

class AuthChoiceTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun segmentedChoiceOpensPhoneAndReturnsToEmail(){
        val auth=AuthViewModel()
        compose.setContent{TrudomixTheme{LoginScreen(auth,{}, {})}}
        compose.onNodeWithText("Пароль").assertExists()
        compose.onNodeWithText("Телефон").performScrollTo().performClick()
        compose.onNodeWithText("Номер телефона").assertExists()
        compose.onNodeWithText("Почта").performScrollTo().performClick()
        compose.onNodeWithText("Пароль").assertExists()
    }
}
