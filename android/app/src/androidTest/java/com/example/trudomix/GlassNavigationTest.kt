package com.example.trudomix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.trudomix.ui.components.GlassNavigation
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GlassNavigationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun overlayConsumesTapsAndMapRemainsClickable() {
        var behindClicks = 0
        var selected = -1
        var mapClicks = 0
        compose.setContent {
            MaterialTheme {
                val haze = remember { HazeState() }
                Box(Modifier.fillMaxSize().testTag("scene")) {
                    Box(Modifier.fillMaxSize().haze(haze).clickable { behindClicks++ })
                    GlassNavigation(haze, 0, 0, { selected = it }, { mapClicks++ }, Modifier.align(Alignment.BottomCenter).testTag("navigation"))
                }
            }
        }
        compose.onNodeWithContentDescription("Отклики").performClick()
        compose.runOnIdle { assertEquals(1, selected); assertEquals(0, behindClicks) }
        compose.onNodeWithContentDescription("Карта вакансий").performClick()
        compose.runOnIdle { assertEquals(1, mapClicks); assertEquals(0, behindClicks) }
        // Tap the background capsule between the label and the icon.
        compose.onNodeWithTag("navigation").performTouchInput { click(Offset(width * .35f, height * .5f)) }
        compose.runOnIdle { assertEquals(0, behindClicks) }
        compose.onNodeWithTag("scene").performTouchInput { click(Offset(10f, 10f)) }
        compose.runOnIdle { assertEquals(1, behindClicks) }
    }
}
