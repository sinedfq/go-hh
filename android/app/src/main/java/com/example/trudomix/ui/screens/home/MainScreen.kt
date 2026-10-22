package com.example.trudomix.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trudomix.ui.components.GlassNavigation
import com.example.trudomix.ui.screens.applications.*
import com.example.trudomix.ui.screens.chats.*
import com.example.trudomix.ui.screens.profile.*
import com.example.trudomix.ui.screens.vacancies.*
import com.example.trudomix.data.repository.ChatRepository
import dev.chrisbanes.haze.*
import kotlinx.coroutines.*

@Composable
fun MainScreen(onLogout:()->Unit={},onVacancyClick:(Int)->Unit={},onNavigateToResumeEditor:()->Unit={},onNavigateToSettings:()->Unit={},onNavigateToLogs:()->Unit={},onChatClick:(Int)->Unit={},onMap:()->Unit={},initialTab:Int=4,onEmployer:()->Unit={}) {
    val tabs:MainTabsViewModel=viewModel()
    val selected by tabs.selectedTab(initialTab).collectAsState()
    var savedSearchReturnTab by rememberSaveable{mutableIntStateOf(4)}
    androidx.activity.compose.BackHandler(enabled=selected==5){tabs.select(savedSearchReturnTab)}
    val tabState=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val haze=remember{HazeState()}
    val owner=androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current!!
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var unread by remember{mutableIntStateOf(0)}
    LaunchedEffect(lifecycle,selected) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive) {
                try { unread=ChatRepository().unread();if(selected==2) androidx.lifecycle.ViewModelProvider(owner)[ChatViewModel::class.java].loadConversations(silent=true) }
                catch(e:CancellationException){throw e} catch(_:Exception){}
                delay(10000)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().haze(haze)) {
            AnimatedContent(selected, transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(100))) }, label = "main-tab") { tab ->
                tabState.SaveableStateProvider(tab) {
                when(tab) {
                    0->VacancyListScreen(viewModel(),onVacancyClick,{savedSearchReturnTab=0;tabs.select(5)})
                    1->ApplicationsScreen(viewModel(),onVacancyClick,onChatClick)
                    2->ChatListScreen(viewModel(),onChatClick)
                    3->ProfileScreen(viewModel(),onLogout,onNavigateToSettings,onNavigateToLogs,onNavigateToResumeEditor,onEmployer,{tabs.select(1)},{tabs.select(0)})
                    4->DashboardScreen({tabs.select(0)},{savedSearchReturnTab=4;tabs.select(5)},{tabs.select(1)},onNavigateToResumeEditor,onEmployer,onVacancyClick)
                    5->SavedSearchesScreen(viewModel(key="saved-searches"),{tabs.select(savedSearchReturnTab)},{filter->androidx.lifecycle.ViewModelProvider(owner)[VacanciesViewModel::class.java].restoreSearch(filter);tabs.select(0)})
                }
                }
            }
        }
        GlassNavigation(haze,selected,unread,tabs::select,onMap,Modifier.align(Alignment.BottomCenter))
    }
}
