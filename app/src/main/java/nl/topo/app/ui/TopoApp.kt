package nl.topo.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
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
    fun region(id: RegionId) = "region/${id.name}"
    fun quiz(id: RegionId, topic: String, level: Int) = "quiz/${id.name}/$topic/$level"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Kaart", Icons.Rounded.Explore),
    Tab(Routes.PROGRESS, "Voortgang", Icons.Rounded.Insights),
    Tab(Routes.SETTINGS, "Instellingen", Icons.Rounded.Settings),
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
                modifier = Modifier.padding(inner),
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
                    ProgressScreen(
                        onOpenRegion = { nav.navigate(Routes.region(it)) },
                        onGoToMap = { nav.switchTab(Routes.HOME) },
                    )
                }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.REGION, arguments = listOf(navArgument("region") { type = NavType.StringType })) { e ->
                    val id = RegionId.valueOf(e.arguments?.getString("region") ?: RegionId.EUROPA.name)
                    RegionScreen(
                        regionId = id,
                        onBack = { nav.popBackStack() },
                        onPlay = { topic, level -> nav.navigate(Routes.quiz(id, topic, level)) },
                    )
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
                label = { Text(tab.label) },
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
