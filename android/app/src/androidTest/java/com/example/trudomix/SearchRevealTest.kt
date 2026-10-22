package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.data.models.Vacancy
import com.example.trudomix.ui.screens.vacancies.*
import com.example.trudomix.ui.theme.TrudomixTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SearchRevealTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun upwardScrollRevealsSearchWithIconFilter() {
        var filters=0
        val vacancies=(1..40).map{Vacancy(id=it,title="Вакансия $it",company="Команда",location="Новосибирск")}
        compose.setContent {TrudomixTheme(darkTheme=false){VacancyFeed(VacancyListUiState.Success(vacancies),"",false,40,false,null,{}, {},{filters++},{},{},{},{_,_->})}}
        val list=compose.onNode(hasScrollToIndexAction())
        list.performTouchInput{swipeUp()}
        list.performTouchInput{swipeUp()}
        compose.onNodeWithTag("floating-vacancy-search").assertDoesNotExist()
        list.performTouchInput{swipeDown()}
        compose.onNodeWithTag("floating-vacancy-search").assertIsDisplayed()
        compose.onNodeWithTag("floating-vacancy-search").onChildren().onFirst().assertExists()
        compose.onNode(hasContentDescription("Фильтры") and hasAnyAncestor(hasTestTag("floating-vacancy-search"))).performClick()
        compose.runOnIdle{assertEquals(1,filters)}
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("floating-vacancy-search"))).performClick()
        list.performTouchInput{swipeUp()}
        compose.onNodeWithTag("floating-vacancy-search").assertDoesNotExist()
    }
}
