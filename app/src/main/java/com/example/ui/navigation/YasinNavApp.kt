package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.registry.Routes
import com.example.di.AppContainer
import com.example.ui.screens.ai.AiAgentScreen
import com.example.ui.screens.ai.AiAgentViewModel
import com.example.ui.screens.docs.DocsMcpScreen
import com.example.ui.screens.docs.DocsMcpViewModel
import com.example.ui.screens.hub.HubScreen
import com.example.ui.screens.hub.HubViewModel
import com.example.ui.screens.press.PressScreen
import com.example.ui.screens.press.PressViewModel
import com.example.ui.screens.relay.RelaySettingsScreen
import com.example.ui.screens.relay.RelaySettingsViewModel

private data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val primaryTabs = listOf(
    BottomNavItem(
        route = Routes.HUB,
        labelRes = R.string.nav_hub,
        selectedIcon = Icons.Filled.Hub,
        unselectedIcon = Icons.Outlined.Hub
    ),
    BottomNavItem(
        route = Routes.AI_AGENT,
        labelRes = R.string.nav_ai,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    BottomNavItem(
        route = Routes.PRESS,
        labelRes = R.string.nav_press,
        selectedIcon = Icons.Filled.Newspaper,
        unselectedIcon = Icons.Outlined.Newspaper
    ),
    BottomNavItem(
        route = Routes.DOCS_MCP,
        labelRes = R.string.nav_docs,
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YasinNavApp(
    appContainer: AppContainer
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HUB
    var pendingAiPrompt by rememberSaveable { mutableStateOf<String?>(null) }

    fun navigateTo(route: String) {
        if (route == Routes.RELAY_SETTINGS) {
            navController.navigate(Routes.RELAY_SETTINGS) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val topTitle = when (currentRoute) {
        Routes.HUB -> stringResource(R.string.app_name)
        Routes.AI_AGENT -> stringResource(R.string.nav_ai)
        Routes.PRESS -> stringResource(R.string.nav_press)
        Routes.DOCS_MCP -> stringResource(R.string.nav_docs)
        Routes.RELAY_SETTINGS -> stringResource(R.string.relay_title)
        else -> stringResource(R.string.app_name)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp
        val isMainTab = primaryTabs.any { it.route == currentRoute }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = topTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        if (currentRoute == Routes.RELAY_SETTINGS) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("topbar_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت"
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute != Routes.RELAY_SETTINGS) {
                            IconButton(
                                onClick = { navigateTo(Routes.RELAY_SETTINGS) },
                                modifier = Modifier.testTag("topbar_relay_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.settings_title)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useRail && isMainTab) {
                    NavigationBar {
                        primaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("bottom_nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && isMainTab) {
                    NavigationRail {
                        primaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_nav_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HUB
                    ) {
                        composable(Routes.HUB) {
                            val hubVm: HubViewModel = viewModel(
                                factory = HubViewModel.factory(
                                    pressRepository = appContainer.pressRepository,
                                    relayConfigRepository = appContainer.relayConfigRepository
                                )
                            )
                            HubScreen(
                                viewModel = hubVm,
                                onNavigateToRoute = ::navigateTo
                            )
                        }

                        composable(Routes.AI_AGENT) {
                            val aiVm: AiAgentViewModel = viewModel(
                                factory = AiAgentViewModel.factory(
                                    aiGateway = appContainer.aiGateway,
                                    dao = appContainer.yasinDao,
                                    pressRepository = appContainer.pressRepository,
                                    inspectorRepository = appContainer.inspectorRepository,
                                    relayConfigRepository = appContainer.relayConfigRepository,
                                    logRepository = appContainer.logRepository
                                )
                            )
                            AiAgentScreen(
                                viewModel = aiVm,
                                onNavigateToRoute = ::navigateTo,
                                initialPrompt = pendingAiPrompt,
                                onConsumedInitialPrompt = { pendingAiPrompt = null }
                            )
                        }

                        composable(Routes.PRESS) {
                            val pressVm: PressViewModel = viewModel(
                                factory = PressViewModel.factory(
                                    pressRepository = appContainer.pressRepository,
                                    aiGateway = appContainer.aiGateway,
                                    eitaaRepository = appContainer.eitaaRepository,
                                    relayConfigRepository = appContainer.relayConfigRepository,
                                    logRepository = appContainer.logRepository,
                                    telegramChannelRepository = appContainer.telegramChannelRepository,
                                    pressPipeline = appContainer.pressPipeline
                                )
                            )
                            PressScreen(
                                viewModel = pressVm,
                                onOpenEitaayarSettings = { navigateTo(Routes.RELAY_SETTINGS) }
                            )
                        }

                        composable(Routes.DOCS_MCP) {
                            val docsVm: DocsMcpViewModel = viewModel(
                                factory = DocsMcpViewModel.factory(
                                    inspectorRepository = appContainer.inspectorRepository,
                                    relayConfigRepository = appContainer.relayConfigRepository
                                )
                            )
                            DocsMcpScreen(
                                viewModel = docsVm,
                                onAnalyzeWithAi = { prompt ->
                                    pendingAiPrompt = prompt
                                    navigateTo(Routes.AI_AGENT)
                                }
                            )
                        }

                        composable(Routes.RELAY_SETTINGS) {
                            val relayVm: RelaySettingsViewModel = viewModel(
                                factory = RelaySettingsViewModel.factory(
                                    relayConfigRepository = appContainer.relayConfigRepository,
                                    eitaaRepository = appContainer.eitaaRepository,
                                    logRepository = appContainer.logRepository
                                )
                            )
                            RelaySettingsScreen(viewModel = relayVm)
                        }
                    }
                }
            }
        }
    }
}
