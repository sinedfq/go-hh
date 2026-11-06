package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.data.models.*
import com.example.trudomix.ui.screens.home.RecommendationCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecommendationPresentationTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun explanationOpensAndDismissIsSeparateFromVacancyNavigation(){
        var opened=0;var dismissed=0
        val item=Recommendation(vacancy=Vacancy(id=7,title="Go Developer",company="Team"),insights=RecommendationInsights(matched_skills=listOf("Go"),missing_skills=listOf("Kotlin"),reasons=listOf("Совпадает город"),considerations=listOf("Опыт отличается")))
        compose.setContent{MaterialTheme{RecommendationCard(item,false,{opened++},{dismissed++})}}
        compose.onNodeWithText("Совпадают: Go").assertExists()
        compose.onNodeWithText("Почему эта вакансия").performClick()
        compose.onNodeWithText("Совпадает город",substring=true).assertExists()
        compose.onNodeWithText("Опыт отличается",substring=true).assertExists()
        compose.onNodeWithText("Не интересно").performClick()
        compose.runOnIdle{assertEquals(0,opened);assertEquals(1,dismissed)}
        compose.onNodeWithText("Открыть").performClick()
        compose.runOnIdle{assertEquals(1,opened);assertEquals(1,dismissed)}
    }
}
