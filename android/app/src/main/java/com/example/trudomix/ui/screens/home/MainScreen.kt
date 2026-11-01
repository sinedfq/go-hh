package com.example.trudomix.ui.screens.home

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val selected by tabs.selectedTab(initialTab).collectAsStateWithLifecycle()
    val identity by com.example.trudomix.data.local.SessionIdentity.user.collectAsStateWithLifecycle()
    val employer=identity?.role in listOf("employer","admin")
    LaunchedEffect(employer){if(!employer&&selected==6||employer&&selected in listOf(5,7))tabs.select(4)}
    var savedSearchReturnTab by rememberSaveable{mutableIntStateOf(4)}
    androidx.activity.compose.BackHandler(enabled=selected==5){tabs.select(savedSearchReturnTab)}
    val tabState=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val owner=androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current!!
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(selected, transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(100))) }, label = "main-tab") { tab ->
                tabState.SaveableStateProvider(tab) {
                when(tab) {
                    0->if(employer)com.example.trudomix.ui.screens.employer.EmployerScreen({tabs.select(4)},onChatClick,0,true)else VacancyListScreen(viewModel(),onVacancyClick,{savedSearchReturnTab=0;tabs.select(5)},onMap)
                    1->if(employer)com.example.trudomix.ui.screens.employer.EmployerScreen({tabs.select(4)},onChatClick,1,true)else ApplicationsScreen(viewModel(),onVacancyClick,onChatClick)
                    2->ChatListScreen(viewModel(),onChatClick)
                    3->ProfileScreen(viewModel(),onLogout,onNavigateToSettings,onNavigateToLogs,onNavigateToResumeEditor,onEmployer,{tabs.select(1)},{tabs.select(0)})
                    4->DashboardScreen({tabs.select(0)},{savedSearchReturnTab=4;tabs.select(5)},{tabs.select(1)},onNavigateToResumeEditor,onEmployer,onVacancyClick)
                    5->SavedSearchesScreen(viewModel(key="saved-searches"),{tabs.select(savedSearchReturnTab)},{filter->androidx.lifecycle.ViewModelProvider(owner)[VacanciesViewModel::class.java].restoreSearch(filter);tabs.select(0)})
                    6->{val model:com.example.trudomix.ui.screens.employer.EmployerViewModel=viewModel();val companyState by model.state.collectAsStateWithLifecycle();if(companyState.company.id>0)com.example.trudomix.ui.screens.companies.CompanyScreen(companyState.company.id,{tabs.select(4)},onVacancyClick,148.dp,true)else com.example.trudomix.ui.screens.employer.EmployerScreen({tabs.select(4)},onChatClick,0,true)}
                    7->SwipeScreen(onVacancyClick)
                }
                }
            }
        }
        PollingNavigation(selected,tabs::select,onMap,Modifier.align(Alignment.BottomCenter),employer)
    }
}

/** Badge updates invalidate only navigation; changing tabs does not restart its request loop. */
@Composable private fun PollingNavigation(selected:Int,onSelect:(Int)->Unit,onMap:()->Unit,modifier:Modifier,employer:Boolean){
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val repository=remember{ChatRepository()}
    var unread by remember{mutableIntStateOf(0)}
    LaunchedEffect(lifecycle,repository){
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
            while(isActive){
                try{unread=repository.unread()}catch(e:CancellationException){throw e}catch(_:Exception){}
                delay(10000)
            }
        }
    }
    GlassNavigation(remember{HazeState()},selected,unread,onSelect,onMap,modifier,employer)
}
