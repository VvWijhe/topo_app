package nl.topo.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class RoundLength(val label: String) { TEN("10 vragen"), ALL("Alles") }

data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val roundLength: RoundLength = RoundLength.TEN,
    val mapLabels: Boolean = false,
)

class SettingsRepository(private val store: DataStore<Preferences>) {
    private object Keys {
        val SOUND = booleanPreferencesKey("settings/sound")
        val HAPTICS = booleanPreferencesKey("settings/haptics")
        val ROUND = stringPreferencesKey("settings/round")
        val LABELS = booleanPreferencesKey("settings/labels")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            sound = p[Keys.SOUND] ?: true,
            haptics = p[Keys.HAPTICS] ?: true,
            roundLength = p[Keys.ROUND]?.let { runCatching { RoundLength.valueOf(it) }.getOrNull() } ?: RoundLength.TEN,
            mapLabels = p[Keys.LABELS] ?: false,
        )
    }

    suspend fun setSound(on: Boolean) { store.edit { it[Keys.SOUND] = on } }
    suspend fun setHaptics(on: Boolean) { store.edit { it[Keys.HAPTICS] = on } }
    suspend fun setRoundLength(length: RoundLength) { store.edit { it[Keys.ROUND] = length.name } }
    suspend fun setMapLabels(on: Boolean) { store.edit { it[Keys.LABELS] = on } }
}
