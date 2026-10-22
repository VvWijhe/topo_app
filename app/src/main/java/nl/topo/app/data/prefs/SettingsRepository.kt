package nl.topo.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.topo.app.data.i18n.Lang

enum class RoundLength { TEN, ALL }

data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val roundLength: RoundLength = RoundLength.TEN,
    val mapLabels: Boolean = false,
    /** Kaart met hoog contrast, voor buiten. */
    val highContrast: Boolean = false,
    /** Standaard Engels. */
    val language: Lang = Lang.EN,
)

class SettingsRepository(private val store: DataStore<Preferences>) {
    private object Keys {
        val SOUND = booleanPreferencesKey("settings/sound")
        val HAPTICS = booleanPreferencesKey("settings/haptics")
        val ROUND = stringPreferencesKey("settings/round")
        val LABELS = booleanPreferencesKey("settings/labels")
        val LANGUAGE = stringPreferencesKey("settings/language")
        val CONTRAST = booleanPreferencesKey("settings/contrast")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            sound = p[Keys.SOUND] ?: true,
            haptics = p[Keys.HAPTICS] ?: true,
            roundLength = p[Keys.ROUND]?.let { runCatching { RoundLength.valueOf(it) }.getOrNull() } ?: RoundLength.TEN,
            mapLabels = p[Keys.LABELS] ?: false,
            highContrast = p[Keys.CONTRAST] ?: false,
            language = p[Keys.LANGUAGE]?.let { runCatching { Lang.valueOf(it) }.getOrNull() } ?: Lang.EN,
        )
    }

    suspend fun setSound(on: Boolean) { store.edit { it[Keys.SOUND] = on } }
    suspend fun setHaptics(on: Boolean) { store.edit { it[Keys.HAPTICS] = on } }
    suspend fun setRoundLength(length: RoundLength) { store.edit { it[Keys.ROUND] = length.name } }
    suspend fun setMapLabels(on: Boolean) { store.edit { it[Keys.LABELS] = on } }
    suspend fun setHighContrast(on: Boolean) { store.edit { it[Keys.CONTRAST] = on } }
    suspend fun setLanguage(lang: Lang) { store.edit { it[Keys.LANGUAGE] = lang.name } }
}
