package com.example.trudomix

import androidx.lifecycle.SavedStateHandle
import com.example.trudomix.ui.screens.home.MainTabsViewModel
import com.example.trudomix.ui.components.senderInitials
import com.example.trudomix.ui.screens.resume.formatResumeExperience
import org.junit.Assert.*
import org.junit.Test

class NavigationAndChatTest {
    @Test fun returningFromChildKeepsProfileAndChatTabs() {
        val tabs=MainTabsViewModel(SavedStateHandle())
        assertEquals(0,tabs.selectedTab(0).value)
        tabs.select(3)
        // Re-entry passes the original route argument (0); it must not select Search.
        assertEquals(3,tabs.selectedTab(0).value)
        tabs.select(2)
        assertEquals(2,tabs.selectedTab(0).value)
        tabs.select(1)
        assertEquals(1,tabs.selectedTab(0).value)
    }
    @Test fun restoredTabWinsOverInitialRouteArgument() {
        val tabs=MainTabsViewModel(SavedStateHandle(mapOf("selectedTab" to 3)))
        assertEquals(3,tabs.selectedTab(0).value)
        assertEquals(1,MainTabsViewModel(SavedStateHandle()).selectedTab(1).value)
    }
    @Test fun avatarFallbackAndExperienceLabelsAreReadable() {
        assertEquals("ДИ",senderInitials("Денис Иванов"))
        assertEquals("D",senderInitials("denis@example.test"))
        assertEquals("?",senderInitials("  "))
        assertEquals("Junior",formatResumeExperience("Junior"))
        assertEquals("1-3 года",formatResumeExperience("1-3 года"))
        assertEquals("5 лет",formatResumeExperience("5"))
    }
}
