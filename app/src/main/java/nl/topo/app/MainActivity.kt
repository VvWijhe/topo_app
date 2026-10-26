package nl.topo.app

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
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
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(Settings())
            CompositionLocalProvider(LocalAppContainer provides container, LocalLang provides settings.language, LocalHighContrast provides settings.highContrast) {
                TopoTheme { TopoApp() }
            }
        }
    }
}
