package nl.topo.app

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.ui.theme.LocalHighContrast
import nl.topo.app.data.prefs.Settings
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.TopoApp
import nl.topo.app.ui.theme.TopoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Altijd donkere systeembalken: de app heeft één donker thema (Nachtkaart).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val container = (application as TopoApplication).container
        lifecycleScope.launch { container.progress.migrateToFiveLevels() }
        // Uitslagen die nog in de wachtrij staan (bijv. gespeeld zonder internet) alsnog versturen.
        lifecycleScope.launch { runCatching { container.leaderboard.flush() } }
        // Voortgang delen met vrienden: na elke wijziging (even wachten, zodat niet elke tik een verzoek wordt).
        if (container.leaderboard.isOnline) lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(container.progress.progress, container.profile.nickname) { pr, name ->
                val regions = nl.topo.app.data.content.Content.regions.filter { it.available }
                nl.topo.app.data.online.PlayerStats(
                    playerId = "", nickname = name, points = pr.points, streak = pr.streak, rounds = pr.rounds,
                    mastered = regions.sumOf { pr.mastered(it) },
                    regions = regions.associate { it.id.name to pr.mastered(it) },
                    updatedAt = 0L,
                )
            }.distinctUntilChanged().collectLatest { st ->
                kotlinx.coroutines.delay(3_000)
                container.leaderboard.publishStats(st.copy(updatedAt = System.currentTimeMillis()))
            }
        }
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(Settings())
            CompositionLocalProvider(LocalAppContainer provides container, LocalLang provides settings.language, LocalHighContrast provides settings.highContrast) {
                TopoTheme { TopoApp() }
            }
        }
    }
}
