package nl.topo.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.topo.app.data.content.Levels
import nl.topo.app.data.content.Region
import nl.topo.app.data.content.RegionId
import java.time.LocalDate

data class LastPlayed(val region: RegionId, val topicId: String, val level: Int)

data class Progress(
    /** "REGIO/onderwerp/niveau" -> beste percentage */
    val best: Map<String, Int> = emptyMap(),
    val rounds: Int = 0,
    val streak: Int = 0,
    val lastPlayed: LastPlayed? = null,
) {
    fun best(region: RegionId, topicId: String, level: Int): Int? = best[key(region, topicId, level)]

    fun mastered(region: Region): Int =
        region.topics.sumOf { t -> (1..4).count { lv -> (best(region.id, t.id, lv) ?: 0) >= Levels.MASTERED } }

    fun played(region: Region): Int =
        region.topics.sumOf { t -> (1..4).count { lv -> best(region.id, t.id, lv) != null } }

    fun masteredAtLevel(region: Region, level: Int): Int =
        region.topics.count { t -> (best(region.id, t.id, level) ?: 0) >= Levels.MASTERED }

    /** 0..1: aandeel gekende vakjes in deze regio. */
    fun fraction(region: Region): Float =
        if (region.cellCount == 0) 0f else mastered(region).toFloat() / region.cellCount

    companion object {
        fun key(region: RegionId, topicId: String, level: Int) = "${region.name}/$topicId/$level"
    }
}

class ProgressRepository(private val store: DataStore<Preferences>) {
    private object Keys {
        const val BEST_PREFIX = "best/"
        val ROUNDS = intPreferencesKey("progress/rounds")
        val STREAK = intPreferencesKey("progress/streak")
        val LAST_DAY = longPreferencesKey("progress/lastDay")
        val LAST_PLAYED = stringPreferencesKey("progress/lastPlayed")
    }

    val progress: Flow<Progress> = store.data.map { p ->
        val best = p.asMap().entries
            .filter { it.key.name.startsWith(Keys.BEST_PREFIX) }
            .mapNotNull { e -> (e.value as? Int)?.let { e.key.name.removePrefix(Keys.BEST_PREFIX) to it } }
            .toMap()
        val today = LocalDate.now().toEpochDay()
        val lastDay = p[Keys.LAST_DAY]
        // Een reeks telt alleen als je vandaag of gisteren nog speelde.
        val streak = if (lastDay != null && today - lastDay <= 1) p[Keys.STREAK] ?: 0 else 0
        Progress(
            best = best,
            rounds = p[Keys.ROUNDS] ?: 0,
            streak = streak,
            lastPlayed = p[Keys.LAST_PLAYED]?.let(::parseLast),
        )
    }

    /** Sla het resultaat van een ronde op. Geeft true terug bij een nieuw record. */
    suspend fun recordRound(region: RegionId, topicId: String, level: Int, percent: Int): Boolean {
        var record = false
        store.edit { p ->
            val bestKey = intPreferencesKey(Keys.BEST_PREFIX + Progress.key(region, topicId, level))
            val old = p[bestKey]
            if (old == null || percent > old) { p[bestKey] = percent; record = true }
            p[Keys.ROUNDS] = (p[Keys.ROUNDS] ?: 0) + 1
            val today = LocalDate.now().toEpochDay()
            val lastDay = p[Keys.LAST_DAY]
            p[Keys.STREAK] = when {
                lastDay == today -> p[Keys.STREAK] ?: 1
                lastDay == today - 1 -> (p[Keys.STREAK] ?: 0) + 1
                else -> 1
            }
            p[Keys.LAST_DAY] = today
            p[Keys.LAST_PLAYED] = "${region.name}|$topicId|$level"
        }
        return record
    }

    suspend fun reset() {
        store.edit { p ->
            p.asMap().keys
                .filter { it.name.startsWith(Keys.BEST_PREFIX) || it.name.startsWith("progress/") }
                .forEach { p.remove(it) }
        }
    }

    private fun parseLast(s: String): LastPlayed? {
        val parts = s.split("|")
        if (parts.size != 3) return null
        val region = runCatching { RegionId.valueOf(parts[0]) }.getOrNull() ?: return null
        return LastPlayed(region, parts[1], parts[2].toIntOrNull() ?: return null)
    }
}
