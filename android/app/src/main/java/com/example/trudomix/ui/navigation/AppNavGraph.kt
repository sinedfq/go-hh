package com.example.trudomix.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trudomix.ui.screens.home.MainTabsViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.trudomix.push.PushNavigation
import com.example.trudomix.ui.screens.map.VacancyMapScreen
import com.example.trudomix.ui.screens.companies.CompanyScreen
import com.example.trudomix.ui.screens.employer.EmployerScreen
import androidx.navigation.navArgument
import com.example.trudomix.data.api.ApiClient
import com.example.trudomix.ui.screens.auth.AuthViewModel
import com.example.trudomix.ui.screens.auth.LoginScreen
import com.example.trudomix.ui.screens.auth.RegisterScreen
import com.example.trudomix.ui.screens.debug.LogViewerScreen
import com.example.trudomix.ui.screens.profile.ProfileViewModel
import com.example.trudomix.ui.screens.profile.ProfileScreen
import com.example.trudomix.ui.screens.resume.ResumeEditorScreen
import com.example.trudomix.ui.screens.resume.ResumeViewModel
import com.example.trudomix.ui.screens.resume.ResumeScreen
import com.example.trudomix.ui.screens.settings.SettingsScreen
import com.example.trudomix.ui.screens.vacancies.VacancyDetailScreen
import com.example.trudomix.ui.screens.vacancies.VacancyDetailViewModel
import com.example.trudomix.ui.screens.home.MainScreen // <--- Убедись, что этот файл существует
import com.example.trudomix.ui.screens.chat.ChatScreen // <--- Добавил импорт чата

object NavRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main?tab={tab}"
    const val VACANCY_DETAIL = "vacancy_detail/{vacancyId}"
    const val RESUME_EDITOR = "resume_editor"
    const val SETTINGS = "settings"
    const val LOGS = "logs"
    const val CHAT_DETAIL = "chat_detail/{conversationId}" // <--- Новый маршрут

    fun vacancyDetail(id: Int) = "vacancy_detail/$id"
    fun chatDetail(id: Int) = "chat_detail/$id"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val entry by navController.currentBackStackEntryAsState()
    val token by ApiClient.getTokenManager().tokenFlow.collectAsState(initial = "loading")
    val pushTarget by PushNavigation.target.collectAsState()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val identity by com.example.trudomix.data.local.SessionIdentity.user.collectAsState()
    LaunchedEffect(token) {
        if(token==null)com.example.trudomix.data.local.SessionIdentity.user.value=null
    }
    LaunchedEffect(token, entry?.destination?.route) {
        val route = entry?.destination?.route ?: return@LaunchedEffect
        if (token == null && route !in listOf(NavRoutes.SPLASH, NavRoutes.LOGIN, NavRoutes.REGISTER)) {
            navController.navigate(NavRoutes.LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }
    LaunchedEffect(pushTarget, token, entry?.destination?.route) {
        val target = pushTarget ?: return@LaunchedEffect
        val route = entry?.destination?.route ?: return@LaunchedEffect
        if (!token.isNullOrBlank() && token != "loading" && route !in listOf(NavRoutes.SPLASH, NavRoutes.LOGIN, NavRoutes.REGISTER)) {
            PushNavigation.target.value = null
            if(target=="applications" && route==NavRoutes.MAIN) {
                entry?.let{ViewModelProvider(it)[MainTabsViewModel::class.java].select(1)}
            } else navController.navigate(if (target == "applications") "main?tab=1" else target) { launchSingleTop = true }
        }
    }

    com.example.trudomix.ui.components.ProfileAccessHost(entry?.id,token!=null&&token!="loading"&&entry?.destination?.route !in listOf(NavRoutes.LOGIN,NavRoutes.REGISTER,NavRoutes.SPLASH,"profile"),
        onProfile={navController.navigate("profile"){launchSingleTop=true}},photo=identity?.photo_url.orEmpty()) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH,
        enterTransition = { fadeIn(tween(240)) + slideInHorizontally(tween(300)) { it / 12 } },
        exitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(240)) { -it / 20 } },
        popEnterTransition = { fadeIn(tween(240)) + slideInHorizontally(tween(300)) { -it / 12 } },
        popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(240)) { it / 12 } }
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                onAuthCheckComplete = { isAuthenticated ->
                    val targetRoute = if (isAuthenticated) "main" else NavRoutes.LOGIN
                    navController.navigate(targetRoute) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NavRoutes.REGISTER)
                }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.MAIN, arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 4 })) { backStackEntry ->
            LaunchedEffect(Unit) {
                val prompts = context.getSharedPreferences("notification_permission", android.content.Context.MODE_PRIVATE)
                if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty() && !prompts.getBoolean("asked", false) && android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    prompts.edit().putBoolean("asked", true).apply()
                    permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            MainScreen(
                initialTab = backStackEntry.arguments?.getInt("tab") ?: 4,
                onMap = { navController.navigate("map") },
                onEmployer = { navController.navigate("employer") },
                onLogout = {}, // Session flow owns the single logout navigation.
                onVacancyClick = { vacancyId ->
                    navController.navigate(NavRoutes.vacancyDetail(vacancyId))
                },
                onNavigateToResumeEditor = {
                    navController.navigate("resume")
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onNavigateToLogs = {
                    navController.navigate(NavRoutes.LOGS)
                },
                onChatClick = { conversationId ->
                    navController.navigate(NavRoutes.chatDetail(conversationId))
                }
            )
        }

        composable(
            route = NavRoutes.VACANCY_DETAIL,
            arguments = listOf(navArgument("vacancyId") { type = NavType.IntType })
        ) { backStackEntry ->
            val vacancyId = backStackEntry.arguments?.getInt("vacancyId") ?: 0
            val viewModel: VacancyDetailViewModel = viewModel()
            VacancyDetailScreen(
                vacancyId = vacancyId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onCompanyClick = { navController.navigate("company/$it") }
            )
        }

        // Новый маршрут для экрана чата
        composable(
            route = NavRoutes.CHAT_DETAIL,
            arguments = listOf(navArgument("conversationId") { type = NavType.IntType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getInt("conversationId") ?: 0
            ChatScreen(
                conversationId = conversationId,
                onBackClick = { navController.popBackStack() },
                onVacancyClick = { navController.navigate(NavRoutes.vacancyDetail(it)) }
            )
        }

        composable(NavRoutes.RESUME_EDITOR) {
            val resumeViewModel: ResumeViewModel = viewModel()
            ResumeEditorScreen(
                viewModel = resumeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("resume") {
            val model: ResumeViewModel = viewModel()
            ResumeScreen(model,onNavigateBack={navController.popBackStack()}) { navController.navigate(NavRoutes.RESUME_EDITOR) }
        }

        composable(NavRoutes.SETTINGS) {
            val profileViewModel: ProfileViewModel = viewModel()
            SettingsScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogs = { navController.navigate(NavRoutes.LOGS) }
            )
        }

        composable(NavRoutes.LOGS) {
            LogViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("map") { VacancyMapScreen({ navController.popBackStack() }, { navController.navigate(NavRoutes.vacancyDetail(it)) }) }
        composable("profile") {
            val profile:ProfileViewModel=viewModel()
            ProfileScreen(profile,{},onNavigateToSettings={navController.navigate(NavRoutes.SETTINGS)},onNavigateToLogs={navController.navigate(NavRoutes.LOGS)},onNavigateToResumeEditor={navController.navigate("resume")},onEmployer={navController.navigate("employer")},onApplications={navController.navigate("main?tab=1")},onFavorites={navController.navigate("main?tab=0")},onBack={navController.popBackStack()})
        }
        composable("employer") { EmployerScreen({ navController.popBackStack() }, { navController.navigate(NavRoutes.chatDetail(it)) }) }
        composable("company/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { backStackEntry ->
            CompanyScreen(backStackEntry.arguments?.getInt("id") ?: 0, { navController.popBackStack() }, { navController.navigate(NavRoutes.vacancyDetail(it)) })
        }
    }
    }
}

@Composable
fun SplashScreen(onAuthCheckComplete: (Boolean) -> Unit) {
    val tokenManager = remember { ApiClient.getTokenManager() }
    var attempt by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(attempt) {
        error = false
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) withContext(Dispatchers.Main.immediate){onAuthCheckComplete(false)}
        else try {
            val response = ApiClient.api.getMe()
            if (response.isSuccessful) {com.example.trudomix.data.local.SessionIdentity.user.value=response.body();withContext(Dispatchers.Main.immediate){onAuthCheckComplete(true)}}
            else if (response.code() == 401) { tokenManager.clearSession(); withContext(Dispatchers.Main.immediate){onAuthCheckComplete(false)} }
            else error = true
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (failure: Exception) { android.util.Log.e("TrudomixNavigation","Session startup failed",failure);error = true }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (error) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Не удалось подключиться к серверу")
            Button(onClick = { attempt++ }) { Text("Повторить") }
            com.example.trudomix.ui.screens.settings.ServerConnectionButton { attempt++ }
        } else com.example.trudomix.ui.loading.TrudomixLoadingScreen()
    }
}
