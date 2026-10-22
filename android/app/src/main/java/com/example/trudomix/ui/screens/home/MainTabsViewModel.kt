package com.example.trudomix.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class MainTabsViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    fun selectedTab(initialTab: Int): StateFlow<Int> = savedState.getStateFlow("selectedTab", initialTab.coerceIn(0, 6))
    fun select(tab: Int) { savedState["selectedTab"] = tab.coerceIn(0, 6) }
}
