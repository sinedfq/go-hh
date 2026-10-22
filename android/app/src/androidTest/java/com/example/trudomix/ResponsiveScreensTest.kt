package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.ui.screens.auth.*
import com.example.trudomix.ui.screens.vacancies.*
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class ResponsiveScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun compactLoginWithLargeTextCanSubmit() {
        var submitted: Pair<String,String>? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                Box(Modifier.width(320.dp).fillMaxHeight()) {
                    TrudomixTheme(darkTheme = false) { LoginContent(AuthUiState.Idle, { email,password -> submitted=email to password }, {}) }
                }
            }
        }
        compose.onNode(hasSetTextAction() and hasText("Почта")).performScrollTo().performTextInput("design@example.test")
        compose.onNode(hasSetTextAction() and hasText("Пароль")).performScrollTo().performTextInput("test-password")
        compose.onNode(hasSetTextAction() and hasText("Пароль")).performImeAction()
        compose.runOnIdle { assertEquals("design@example.test" to "test-password", submitted) }
        compose.onNodeWithText("Войти").performScrollTo().assertIsDisplayed()
        screenshot("login-compact-large-text")
    }

    @Test fun loginFitsAvailableWindow() {
        compose.setContent { TrudomixTheme(darkTheme = true) { LoginContent(AuthUiState.Idle, { _,_ -> }, {}) } }
        compose.onNodeWithText("С возвращением").assertIsDisplayed()
        compose.onNodeWithText("Войти").performScrollTo().assertIsDisplayed()
        screenshot("login-window-dark")
    }

    @Test fun vacancyHeaderRemainsVisibleAfterThemeChange() {
        val dark = mutableStateOf(false)
        val vacancy = Vacancy(id=101, title="Разработчик мобильных приложений Android", company="Команда разработки цифровых продуктов", location="Новосибирск, Академгородок", experience="От трёх до шести лет", remote=true, skills=listOf("Kotlin","Jetpack Compose"))
        var opened = 0
        compose.setContent {
            TrudomixTheme(darkTheme = dark.value) {
                VacancyFeed(VacancyListUiState.Success(listOf(vacancy, vacancy.copy(id=102,title="Продуктовый дизайнер"), vacancy.copy(id=103,title="Разработчик Go"))), "", false, 3, false, null, {}, {}, {}, {}, {}, { opened=it }, { _,_ -> })
            }
        }
        compose.onNodeWithText("Найди свою команду").assertIsDisplayed()
        screenshot("vacancies-light")
        compose.runOnIdle { dark.value=true }
        compose.onNodeWithText("Найди свою команду").assertIsDisplayed()
        screenshot("vacancies-dark")
        compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToNode(hasText(vacancy.title))
        compose.onNodeWithText(vacancy.title).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(101, opened) }
    }

    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(250)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
}
