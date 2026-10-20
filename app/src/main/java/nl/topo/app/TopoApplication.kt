package nl.topo.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import nl.topo.app.data.geo.GeoRepository
import nl.topo.app.data.prefs.ProgressRepository
import nl.topo.app.data.prefs.SettingsRepository

private val Context.topoStore: DataStore<Preferences> by preferencesDataStore(name = "topo")

/** Eenvoudige afhankelijkheden-container. Later eventueel te vervangen door Hilt. */
class AppContainer(context: Context) {
    private val store = context.topoStore
    val settings = SettingsRepository(store)
    val progress = ProgressRepository(store)
    val geo = GeoRepository(context.assets)
}

class TopoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
