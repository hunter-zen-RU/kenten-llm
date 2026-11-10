package com.pocketpal.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.pocketpal.PocketPalApplication
import com.pocketpal.data.repository.*
import com.pocketpal.data.server.LocalhostApiServer
import com.pocketpal.ui.screens.benchmark.BenchmarkScreen
import com.pocketpal.ui.screens.benchmark.BenchmarkViewModel
import com.pocketpal.ui.screens.chat.ChatScreen
import com.pocketpal.ui.screens.chat.ChatViewModel
import com.pocketpal.ui.screens.models.ModelsScreen
import com.pocketpal.ui.screens.models.ModelsViewModel
import com.pocketpal.ui.screens.pals.PalsScreen
import com.pocketpal.ui.screens.pals.PalsViewModel
import com.pocketpal.ui.screens.server.LocalhostServerScreen
import com.pocketpal.ui.screens.server.LocalhostServerViewModel
import com.pocketpal.ui.screens.settings.SettingsScreen
import com.pocketpal.ui.screens.settings.SettingsViewModel
import com.pocketpal.ui.screens.voice.HandsFreeVoiceScreen
import com.pocketpal.ui.screens.voice.HandsFreeVoiceViewModel
import com.pocketpal.ui.screens.voice.VoicePacksScreen
import com.pocketpal.ui.screens.voice.VoicePacksViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PocketPalApp() {
    val navController = rememberNavController()

    val db = PocketPalApplication.instance.database
    val chatRepo = remember { ChatRepository(db.chatDao()) }
    val palRepo = remember { PalRepository(db.palDao()) }
    val modelRepo = remember { ModelRepository() }
    val benchmarkRepo = remember { BenchmarkRepository(db.benchmarkDao()) }
    val voiceEngineRepo = remember { VoiceEngineRepository() }
    val aiEngine = remember { AiEngineService() }

    val localhostServer = remember {
        LocalhostApiServer().apply {
            startServer(8080)
        }
    }

    val chatViewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(chatRepo, palRepo, modelRepo, aiEngine) as T
            }
        }
    )

    val palsViewModel: PalsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PalsViewModel(palRepo) as T
            }
        }
    )

    val modelsViewModel: ModelsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ModelsViewModel(modelRepo) as T
            }
        }
    )

    val benchmarkViewModel: BenchmarkViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BenchmarkViewModel(benchmarkRepo) as T
            }
        }
    )

    val handsFreeVoiceViewModel: HandsFreeVoiceViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HandsFreeVoiceViewModel(palRepo, aiEngine) as T
            }
        }
    )

    val localhostServerViewModel: LocalhostServerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LocalhostServerViewModel(localhostServer) as T
            }
        }
    )

    val voicePacksViewModel: VoicePacksViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VoicePacksViewModel(voiceEngineRepo) as T
            }
        }
    )

    val settingsViewModel: SettingsViewModel = viewModel()

    // 5 primary action-oriented bottom bar destinations
    val mainBottomScreens = listOf(
        Screen.Chat,
        Screen.HandsFreeVoice,
        Screen.Pals,
        Screen.Models,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isImeVisible = WindowInsets.isImeVisible

    Scaffold(
        bottomBar = {
            if (!isImeVisible && currentRoute != Screen.HandsFreeVoice.route) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(28.dp)),
                    color = Color(0xFF0F172A),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        mainBottomScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            IconButton(
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    )
                                    .testTag("nav_icon_${screen.route}")
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Chat.route) {
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToPals = { navController.navigate(Screen.Pals.route) },
                    onNavigateToModels = { navController.navigate(Screen.Models.route) },
                    onNavigateToBenchmark = { navController.navigate(Screen.Benchmark.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToHandsFreeVoice = { navController.navigate(Screen.HandsFreeVoice.route) },
                    onNavigateToServer = { navController.navigate(Screen.Server.route) },
                    onNavigateToVoicePacks = { navController.navigate(Screen.VoicePacks.route) }
                )
            }

            composable(Screen.HandsFreeVoice.route) {
                HandsFreeVoiceScreen(
                    viewModel = handsFreeVoiceViewModel,
                    onClose = { navController.popBackStack() }
                )
            }

            composable(Screen.Pals.route) {
                PalsScreen(
                    viewModel = palsViewModel,
                    onStartChatWithPal = { pal ->
                        chatViewModel.createNewSession(title = "Chat with ${pal.name}", pal = pal)
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                        }
                    }
                )
            }

            composable(Screen.Models.route) {
                ModelsScreen(viewModel = modelsViewModel)
            }

            composable(Screen.Server.route) {
                LocalhostServerScreen(viewModel = localhostServerViewModel)
            }

            composable(Screen.VoicePacks.route) {
                VoicePacksScreen(viewModel = voicePacksViewModel)
            }

            composable(Screen.Benchmark.route) {
                BenchmarkScreen(viewModel = benchmarkViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToServer = { navController.navigate(Screen.Server.route) },
                    onNavigateToVoicePacks = { navController.navigate(Screen.VoicePacks.route) }
                )
            }
        }
    }
}
