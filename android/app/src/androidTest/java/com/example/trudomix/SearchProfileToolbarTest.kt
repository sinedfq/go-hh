package com.example.trudomix

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trudomix.ui.components.LocalProfileAccess
import com.example.trudomix.ui.components.ProfileAccess
import com.example.trudomix.ui.screens.vacancies.VacancyFeed
import com.example.trudomix.ui.screens.vacancies.VacancyListUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SearchProfileToolbarTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun profileSharesSearchRowAndFilterDimensions(){
        var opened=false
        compose.setContent{
            MaterialTheme{
                CompositionLocalProvider(LocalProfileAccess provides ProfileAccess({opened=true},"",{_,_->})){
                    VacancyFeed(state=VacancyListUiState.Success(emptyList()),query="",favorites=false,total=0,loading=false,error=null,
                        onQuery={},onFavorites={},onFilters={},onRefresh={},onMore={},onVacancy={},onFavorite={_,_->})
                }
            }
        }
        compose.onAllNodesWithContentDescription("Профиль").assertCountEquals(1)
        val filter=compose.onNodeWithContentDescription("Фильтры").fetchSemanticsNode().boundsInRoot
        val profile=compose.onNodeWithContentDescription("Профиль").fetchSemanticsNode().boundsInRoot
        assertEquals(filter.center.y,profile.center.y,1f)
        assertEquals(filter.width,profile.width,1f)
        assertEquals(filter.height,profile.height,1f)
        assertTrue(profile.left>filter.right)
        compose.onNodeWithContentDescription("Профиль").performClick()
        compose.runOnIdle{assertTrue(opened)}
    }
}
