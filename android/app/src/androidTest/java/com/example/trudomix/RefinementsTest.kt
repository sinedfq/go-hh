package com.example.trudomix

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trudomix.data.local.AppIcons
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.components.AppIconImage
import com.example.trudomix.ui.screens.applications.*
import com.example.trudomix.ui.screens.profile.*
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RefinementsTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun applicationsFilterLettersAndCancellationWork() {
        var cancelled=0
        val app=JobApplication(id=1,vacancy_id=10,vacancy_title="Android-разработчик",company_name="Команда",status="new",cover_letter="Моё письмо работодателю",created_at="2026-10-20T12:00:00Z")
        compose.setContent{TrudomixTheme(darkTheme=false){ApplicationsContent(ApplicationsUiState.Success(listOf(app,app.copy(id=2,vacancy_title="Дизайнер",status="accepted",cover_letter=""))),onCancel={cancelled++})}}
        compose.onNodeWithText("Моё сопроводительное письмо").performScrollTo().performClick()
        compose.onNodeWithText("Моё письмо работодателю").assertIsDisplayed()
        capture("applications-new")
        compose.onNodeWithContentDescription("Отозвать отклик").performScrollTo().performClick()
        compose.onNodeWithText("Оставить").performClick()
        compose.runOnIdle{assertEquals(0,cancelled)}
        compose.onNodeWithContentDescription("Отозвать отклик").performClick()
        compose.onNodeWithText("Отозвать",useUnmergedTree=true).performClick()
        compose.runOnIdle{assertEquals(1,cancelled)}
        compose.onNodeWithText("Приглашения").performScrollTo().performClick()
        compose.onNodeWithText("Дизайнер").assertIsDisplayed()
        compose.onNodeWithText("Android-разработчик").assertDoesNotExist()
    }
    @Test fun profileStatisticsLeadToTheirSections() {
        var resume=0;var applications=0;var favorites=0
        compose.setContent{TrudomixTheme(darkTheme=true){Column(Modifier.padding(20.dp)){
            ProfileDashboard(ProfileOverview(loading=false,views=15,applications=7,accepted=2,favorites=4,resume=Resume(full_name="Имя",desired_position="Android-разработчик",skills=listOf("Kotlin")),resumeKnown=true),true,{resume++},{applications++},{favorites++},{})
        }}}
        compose.onNodeWithText("15").assertIsDisplayed()
        compose.onNodeWithText("Просмотры резюме").performClick()
        compose.onNodeWithText("Отклики").performClick()
        compose.onNodeWithText("Избранное").performClick()
        compose.runOnIdle{assertEquals(1,resume);assertEquals(1,applications);assertEquals(1,favorites)}
        capture("profile-statistics")
    }
    @Test fun iconArtworkRendersWithoutOldOuterFrames() {
        compose.setContent{TrudomixTheme(darkTheme=false){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
            for(number in listOf(9,20,13,2)){val icon=AppIcons.choices.first{it.number==number};Row{AppIconImage(icon.resource,icon.title,Modifier.size(96.dp));androidx.compose.material3.Text(icon.title,Modifier.padding(16.dp))}}
        }}}
        compose.onNodeWithContentDescription("Тёмные волны T").assertIsDisplayed()
        capture("icon-artwork")
    }
    private fun capture(name:String){
        compose.mainClock.advanceTimeBy(500);compose.waitForIdle()
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null),"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
}
