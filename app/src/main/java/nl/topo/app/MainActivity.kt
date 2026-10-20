package nl.topo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
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
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                TopoTheme { TopoApp() }
            }
        }
    }
}
