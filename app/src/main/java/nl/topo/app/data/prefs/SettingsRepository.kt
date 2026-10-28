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

/** Thema: Nachtkaart (donker), Dagkaart (licht) of zoals de telefoon staat. */
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** Hoe de kaart getekend wordt. Elke stijl heeft een lichte en een donkere versie. */
enum class MapStyle { CLASSIC, ATLAS, PARCHMENT, BLUEPRINT, NEON, CONTINENTS, DENSITY }

data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val roundLength: RoundLength = RoundLength.TEN,
    val mapLabels: Boolean = false,
    /** Strikte spelling bij typen (standaard uit: soepel). */
    val strictTyping: Boolean = false,
    /** Kaart met hoog contrast, voor buiten. */
    val highContrast: Boolean = false,
    /** Standaard Engels. */
    val language: Lang = Lang.EN,
    val theme: ThemeMode = ThemeMode.DARK,
    val mapStyle: MapStyle = MapStyle.CLASSIC,
    /** De introductie met de belangrijkste keuzes is doorlopen. */
    val onboarded: Boolean = false,
)

class SettingsRepository(private val store: DataStore<Preferences>) {
    private object Keys {
        val SOUND = booleanPreferencesKey("settings/sound")
        val HAPTICS = booleanPreferencesKey("settings/haptics")
        val ROUND = stringPreferencesKey("settings/round")
        val LABELS = booleanPreferencesKey("settings/labels")
        val LANGUAGE = stringPreferencesKey("settings/language")
        val CONTRAST = booleanPreferencesKey("settings/contrast")
        val STRICT = booleanPreferencesKey("settings/strict")
        val THEME = stringPreferencesKey("settings/theme")
        val MAP_STYLE = stringPreferencesKey("settings/mapStyle")
        val ONBOARDED = booleanPreferencesKey("settings/onboarded")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            sound = p[Keys.SOUND] ?: true,
            haptics = p[Keys.HAPTICS] ?: true,
            roundLength = p[Keys.ROUND]?.let { runCatching { RoundLength.valueOf(it) }.getOrNull() } ?: RoundLength.TEN,
            mapLabels = p[Keys.LABELS] ?: false,
            highContrast = p[Keys.CONTRAST] ?: false,
            strictTyping = p[Keys.STRICT] ?: false,
            language = p[Keys.LANGUAGE]?.let { runCatching { Lang.valueOf(it) }.getOrNull() } ?: Lang.EN,
            theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            mapStyle = p[Keys.MAP_STYLE]?.let { runCatching { MapStyle.valueOf(it) }.getOrNull() } ?: MapStyle.CLASSIC,
            onboarded = p[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun setSound(on: Boolean) { store.edit { it[Keys.SOUND] = on } }
    suspend fun setHaptics(on: Boolean) { store.edit { it[Keys.HAPTICS] = on } }
    suspend fun setRoundLength(length: RoundLength) { store.edit { it[Keys.ROUND] = length.name } }
    suspend fun setMapLabels(on: Boolean) { store.edit { it[Keys.LABELS] = on } }
    suspend fun setStrictTyping(on: Boolean) { store.edit { it[Keys.STRICT] = on } }
    suspend fun setHighContrast(on: Boolean) { store.edit { it[Keys.CONTRAST] = on } }
    suspend fun setLanguage(lang: Lang) { store.edit { it[Keys.LANGUAGE] = lang.name } }
    suspend fun setTheme(mode: ThemeMode) { store.edit { it[Keys.THEME] = mode.name } }
    suspend fun setMapStyle(style: MapStyle) { store.edit { it[Keys.MAP_STYLE] = style.name } }
    suspend fun setOnboarded(done: Boolean) { store.edit { it[Keys.ONBOARDED] = done } }
}
