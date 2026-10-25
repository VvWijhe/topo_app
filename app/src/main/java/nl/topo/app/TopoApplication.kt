package nl.topo.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import nl.topo.app.data.geo.GeoRepository
import nl.topo.app.data.prefs.ProgressRepository
import nl.topo.app.data.prefs.SettingsRepository
import nl.topo.app.data.online.LeaderboardService
import nl.topo.app.data.online.OfflineLeaderboard
import nl.topo.app.data.online.PlayerProfile

private val Context.topoStore: DataStore<Preferences> by preferencesDataStore(name = "topo")

/** Eenvoudige afhankelijkheden-container. Later eventueel te vervangen door Hilt. */
class AppContainer(context: Context) {
    private val store = context.topoStore
    val settings = SettingsRepository(store)
    val progress = ProgressRepository(store)
    val geo = GeoRepository(context.assets)
    val profile = PlayerProfile(store)
    /** Nu offline; later te vervangen door een Firebase-versie met dezelfde functies. */
    val leaderboard: LeaderboardService = OfflineLeaderboard(store, profile)
}

class TopoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
