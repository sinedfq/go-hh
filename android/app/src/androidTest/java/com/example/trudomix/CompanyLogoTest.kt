package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.ui.components.CompanyAvatar
import com.example.trudomix.ui.theme.TrudomixTheme
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Run with -e liveLogo true after placing the disposable PNG in the local API uploads. */
class CompanyLogoTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun relativeCompanyLogoLoadsAndBrokenLogoFallsBack() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveLogo")=="true")
        var logo by mutableStateOf("/uploads/codex_logo_preview.png")
        compose.setContent { TrudomixTheme(darkTheme=false) { Column { CompanyAvatar(logo,"Trudomix") } } }
        Thread.sleep(2500)
        compose.onNodeWithContentDescription("Логотип: Trudomix").assertIsDisplayed()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val screenshot=instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null),"company-logo-android.png").outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
        screenshot.recycle()
        compose.runOnIdle{logo="/uploads/codex_logo_missing.png"}
        compose.waitUntil(15000){compose.onAllNodesWithContentDescription("Компания: Trudomix").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("Компания: Trudomix").assertIsDisplayed()
    }
}
