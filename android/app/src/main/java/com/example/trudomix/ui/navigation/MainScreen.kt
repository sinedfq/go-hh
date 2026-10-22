package com.example.trudomix.ui.navigation

// Compatibility entry point; the only implementation lives in ui.screens.home.
import androidx.compose.runtime.Composable
@Composable
fun HomeScreen(onLogout:()->Unit,onVacancyClick:(Int)->Unit={},onNavigateToResumeEditor:()->Unit={},onNavigateToSettings:()->Unit={},onNavigateToLogs:()->Unit={}) {
    com.example.trudomix.ui.screens.home.MainScreen(onLogout,onVacancyClick,onNavigateToResumeEditor,onNavigateToSettings,onNavigateToLogs)
}
