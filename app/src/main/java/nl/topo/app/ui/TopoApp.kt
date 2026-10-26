package nl.topo.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import nl.topo.app.data.daily.Difficulty
import nl.topo.app.data.puzzle.PuzzleKind
import nl.topo.app.ui.puzzle.PuzzleScreen
import nl.topo.app.ui.daily.DailyPlayScreen
import nl.topo.app.ui.daily.DailyScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.topo.app.AppContainer
import nl.topo.app.data.content.RegionId
import nl.topo.app.data.i18n.Txt
import nl.topo.app.data.i18n.text
import nl.topo.app.ui.components.NightSky
import nl.topo.app.ui.home.HomeScreen
import nl.topo.app.ui.progress.ProgressScreen
import nl.topo.app.ui.quiz.QuizScreen
import nl.topo.app.ui.region.RegionScreen
import nl.topo.app.ui.settings.SettingsScreen
import nl.topo.app.ui.theme.Night

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("Geen AppContainer") }

/** Alle schermen van de app. Nieuwe schermen voeg je hier toe. */
object Routes {
    const val HOME = "home"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val REGION = "region/{region}"
    const val QUIZ = "quiz/{region}/{topic}/{level}"
    const val DAILY = "daily"
    const val DAILY_PLAY = "daily/{difficulty}"
    const val PUZZLE = "puzzle/{kind}/{mode}"
    fun puzzle(kind: PuzzleKind, daily: Boolean) = "puzzle/${kind.name}/${if (daily) "daily" else "practice"}"
    fun dailyPlay(d: Difficulty) = "daily/${d.name}"
    fun region(id: RegionId) = "region/${id.name}"
    fun quiz(id: RegionId, topic: String, level: Int) = "quiz/${id.name}/$topic/$level"
}

private data class Tab(val route: String, val label: Txt, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, Txt("Map", "Kaart"), Icons.Rounded.Explore),
    Tab(Routes.DAILY, Txt("Daily", "Dagelijks"), Icons.Rounded.Today),
    Tab(Routes.PROGRESS, Txt("Progress", "Voortgang"), Icons.Rounded.Insights),
    Tab(Routes.SETTINGS, Txt("Settings", "Instellingen"), Icons.Rounded.Settings),
)

@Composable
fun TopoApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showTabs = tabs.any { it.route == route }

    NightSky {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { if (showTabs) BottomTabs(nav, route) },
        ) { inner ->
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                // Alleen onderaan opvullen: het startscherm loopt door tot onder de statusbalk.
                modifier = Modifier.padding(bottom = inner.calculateBottomPadding()),
                enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenRegion = { nav.navigate(Routes.region(it)) },
                        onPlay = { r, t, l -> nav.navigate(Routes.quiz(r, t, l)) },
                    )
                }
                composable(Routes.PROGRESS) {
                    Box(Modifier.statusBarsPadding()) {
                        ProgressScreen(
                            onOpenRegion = { nav.navigate(Routes.region(it)) },
                            onGoToMap = { nav.switchTab(Routes.HOME) },
                        )
                    }
                }
                composable(Routes.DAILY) {
                    Box(Modifier.statusBarsPadding()) { DailyScreen(onPlay = { nav.navigate(Routes.dailyPlay(it)) }, onPuzzle = { k, d -> nav.navigate(Routes.puzzle(k, d)) }) }
                }
                composable(Routes.DAILY_PLAY, arguments = listOf(navArgument("difficulty") { type = NavType.StringType })) { e ->
                    val d = runCatching { Difficulty.valueOf(e.arguments?.getString("difficulty") ?: "EASY") }.getOrDefault(Difficulty.EASY)
                    Box(Modifier.statusBarsPadding()) { DailyPlayScreen(d, onBack = { nav.popBackStack() }) }
                }
                composable(
                    Routes.PUZZLE,
                    arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("mode") { type = NavType.StringType }),
                ) { e ->
                    val k = runCatching { PuzzleKind.valueOf(e.arguments?.getString("kind") ?: "ROUTE") }.getOrDefault(PuzzleKind.ROUTE)
                    val daily = e.arguments?.getString("mode") == "daily"
                    Box(Modifier.statusBarsPadding()) { PuzzleScreen(k, daily, onBack = { nav.popBackStack() }) }
                }
                composable(Routes.SETTINGS) { Box(Modifier.statusBarsPadding()) { SettingsScreen() } }
                composable(Routes.REGION, arguments = listOf(navArgument("region") { type = NavType.StringType })) { e ->
                    val id = RegionId.valueOf(e.arguments?.getString("region") ?: RegionId.EUROPA.name)
                    Box(Modifier.statusBarsPadding()) {
                        RegionScreen(
                            regionId = id,
                            onBack = { nav.popBackStack() },
                            onPlay = { topic, level -> nav.navigate(Routes.quiz(id, topic, level)) },
                        )
                    }
                }
                composable(
                    Routes.QUIZ,
                    arguments = listOf(
                        navArgument("region") { type = NavType.StringType },
                        navArgument("topic") { type = NavType.StringType },
                        navArgument("level") { type = NavType.IntType },
                    ),
                ) { e ->
                    val a = e.arguments
                    Box(Modifier.statusBarsPadding()) {
                        QuizScreen(
                            regionId = RegionId.valueOf(a?.getString("region") ?: RegionId.EUROPA.name),
                            topicId = a?.getString("topic") ?: "",
                            level = a?.getInt("level") ?: 1,
                            onBack = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun BottomTabs(nav: NavHostController, current: String?) {
    NavigationBar(containerColor = Night.surface.copy(alpha = 0.94f), tonalElevation = 0.dp) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { if (current != tab.route) nav.switchTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label.text) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Night.glow,
                    selectedTextColor = Night.glow,
                    indicatorColor = Night.glow.copy(alpha = 0.14f),
                    unselectedIconColor = Night.muted,
                    unselectedTextColor = Night.muted,
                ),
            )
        }
    }
}
