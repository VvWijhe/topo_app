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
import nl.topo.app.data.i18n.Txt
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
    /** Totaal verdiende punten. */
    val points: Int = 0,
) {
    val rank: Rank get() = Ranks.of(points)

    fun best(region: RegionId, topicId: String, level: Int): Int? = best[key(region, topicId, level)]

    fun mastered(region: Region): Int =
        region.topics.sumOf { t -> Levels.all.count { lv -> (best(region.id, t.id, lv.stars) ?: 0) >= Levels.MASTERED } }

    fun played(region: Region): Int =
        region.topics.sumOf { t -> Levels.all.count { lv -> best(region.id, t.id, lv.stars) != null } }

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
        val POINTS = intPreferencesKey("progress/points")
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
            points = p[Keys.POINTS] ?: 0,
        )
    }

    /**
     * Sla het resultaat van een ronde op en deel punten uit.
     *
     * @param correctFirstTry aantal vragen dat in één keer goed was
     * @param wholeLevel of alle vragen van het niveau voorbijkwamen; dan telt 100% ook voor de lagere niveaus
     */
    suspend fun recordRound(
        region: RegionId, topicId: String, level: Int, percent: Int,
        correctFirstTry: Int = 0, wholeLevel: Boolean = false,
    ): RoundOutcome {
        var outcome = RoundOutcome(false, emptyList(), 0, 0, emptyList())
        store.edit { p ->
            val awards = mutableListOf<Award>()
            val stars = level.coerceIn(1, Levels.all.size)
            val bestKey = intPreferencesKey(Keys.BEST_PREFIX + Progress.key(region, topicId, level))
            val old = p[bestKey]
            val record = old == null || percent > old
            if (record) p[bestKey] = percent

            // Basis: punten per goed antwoord, meer op hogere niveaus
            if (correctFirstTry > 0) awards += Award(Award.Kind.ANSWERS, correctFirstTry * Points.perAnswer(stars), correctFirstTry)
            if (percent == 100) awards += Award(Award.Kind.PERFECT, 50 * stars)
            when {
                percent >= Levels.MASTERED && (old == null || old < Levels.MASTERED) -> awards += Award(Award.Kind.MASTERED, 100 * stars)
                record && old != null -> awards += Award(Award.Kind.RECORD, 20 * stars)
            }

            // 100% over het hele niveau: de lagere niveaus van dit onderwerp tellen ook als gehaald
            val cascaded = mutableListOf<Int>()
            if (percent == 100 && wholeLevel) {
                for (lv in 1 until level) {
                    val k = intPreferencesKey(Keys.BEST_PREFIX + Progress.key(region, topicId, lv))
                    val o = p[k]
                    if (o == null || o < 100) {
                        p[k] = 100
                        cascaded += lv
                        if (o == null || o < Levels.MASTERED) awards += Award(Award.Kind.CASCADE, 50 * lv, lv)
                    }
                }
            }

            p[Keys.ROUNDS] = (p[Keys.ROUNDS] ?: 0) + 1
            p[Keys.LAST_PLAYED] = "${region.name}|$topicId|$level"
            val (before, after) = applyDayAndPoints(p, awards)
            outcome = RoundOutcome(record, awards, before, after, cascaded)
        }
        return outcome
    }

    /** Werkt de dagreeks bij, voegt de dagbonus toe en telt de punten op. */
    private fun applyDayAndPoints(p: androidx.datastore.preferences.core.MutablePreferences, awards: MutableList<Award>): Pair<Int, Int> {
        val today = LocalDate.now().toEpochDay()
        val lastDay = p[Keys.LAST_DAY]
        val streak = when {
            lastDay == today -> p[Keys.STREAK] ?: 1
            lastDay == today - 1 -> (p[Keys.STREAK] ?: 0) + 1
            else -> 1
        }
        // Eerste ronde van de dag: dagbonus, groter bij een langere reeks
        if (lastDay != today) awards += Award(Award.Kind.DAILY, 25 + minOf(50, 5 * (streak - 1)), streak)
        p[Keys.STREAK] = streak
        p[Keys.LAST_DAY] = today
        val before = p[Keys.POINTS] ?: 0
        val after = maxOf(0, before + awards.sumOf { it.points })
        p[Keys.POINTS] = after
        return before to after
    }

    // ---------------- dagelijkse uitdaging ----------------

    private fun dailyKey(day: Long, d: String, what: String) = "daily/$day/$d/$what"

    /** Stand van de uitdaging van [day] op [difficulty] ("EASY"/"HARD"). */
    fun daily(day: Long, difficulty: String): Flow<DailyState> = store.data.map { p ->
        DailyState(
            found = p[stringPreferencesKey(dailyKey(day, difficulty, "found"))]?.split(',')?.filter { it.isNotEmpty() }.orEmpty(),
            hints = p[intPreferencesKey(dailyKey(day, difficulty, "hints"))] ?: 0,
            finished = p[intPreferencesKey(dailyKey(day, difficulty, "points"))] != null,
            points = p[intPreferencesKey(dailyKey(day, difficulty, "points"))] ?: 0,
            gaveUp = p[intPreferencesKey(dailyKey(day, difficulty, "gaveUp"))] == 1,
            mistakes = p[intPreferencesKey(dailyKey(day, difficulty, "mistakes"))] ?: 0,
            lost = p[intPreferencesKey(dailyKey(day, difficulty, "lost"))] == 1,
        )
    }

    suspend fun saveDaily(day: Long, difficulty: String, found: List<String>, hints: Int, mistakes: Int = 0) {
        store.edit { p ->
            p[stringPreferencesKey(dailyKey(day, difficulty, "found"))] = found.joinToString(",")
            p[intPreferencesKey(dailyKey(day, difficulty, "hints"))] = hints
            p[intPreferencesKey(dailyKey(day, difficulty, "mistakes"))] = mistakes
        }
    }

    /**
     * Rond de uitdaging af en deel punten uit: per gevonden land (makkelijk 10, moeilijk 25),
     * een bonus als alles gevonden is (50 / 200), 5 punten aftrek per hint en 5 / 10 per fout.
     * [lost]: te veel fouten; dan geen bonus.
     */
    suspend fun finishDaily(
        day: Long, difficulty: String, hard: Boolean, found: Int, total: Int, hints: Int, gaveUp: Boolean,
        mistakes: Int = 0, lost: Boolean = false,
    ): RoundOutcome {
        var outcome = RoundOutcome(false, emptyList(), 0, 0, emptyList())
        store.edit { p ->
            val pointsKey = intPreferencesKey(dailyKey(day, difficulty, "points"))
            if (p[pointsKey] != null) return@edit // al afgerond
            val awards = mutableListOf<Award>()
            if (found > 0) awards += Award(Award.Kind.CHALLENGE_FOUND, found * if (hard) 25 else 10, found)
            if (found == total && total > 0) awards += Award(Award.Kind.CHALLENGE_DONE, if (hard) 200 else 50)
            if (hints > 0) awards += Award(Award.Kind.HINTS, -5 * hints, hints)
            if (mistakes > 0) awards += Award(Award.Kind.MISTAKES, -mistakes * DailyRules.penalty(hard), mistakes)
            val (before, after) = applyDayAndPoints(p, awards)
            p[pointsKey] = after - before
            if (gaveUp) p[intPreferencesKey(dailyKey(day, difficulty, "gaveUp"))] = 1
            if (lost) p[intPreferencesKey(dailyKey(day, difficulty, "lost"))] = 1
            p[intPreferencesKey(dailyKey(day, difficulty, "mistakes"))] = mistakes
            outcome = RoundOutcome(false, awards, before, after, emptyList())
        }
        return outcome
    }

    /**
     * Eenmalig bij de overstap naar vijf niveaus: de oude ★★★★ (alles typen) heet nu ★★★★★.
     * Die score gaat naar ★★★★★ en telt ook voor de nieuwe ★★★★ (alles aantikken/kiezen).
     */
    suspend fun migrateToFiveLevels() {
        store.edit { p ->
            val flag = intPreferencesKey("progress/levels5")
            if (p[flag] != null) return@edit
            val old4 = p.asMap().entries.filter { it.key.name.startsWith(Keys.BEST_PREFIX) && it.key.name.endsWith("/4") }
            for ((k, v) in old4) {
                val score = v as? Int ?: continue
                val five = intPreferencesKey(k.name.removeSuffix("/4") + "/5")
                if (p[five] == null) p[five] = score
            }
            p[flag] = 1
        }
    }

    suspend fun reset() {
        store.edit { p ->
            p.asMap().keys
                .filter { it.name.startsWith(Keys.BEST_PREFIX) || it.name.startsWith("progress/") || it.name.startsWith("daily/") }
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


/** Eén bonus na een ronde. [n]: aantal antwoorden, niveau of dagen, afhankelijk van [kind]. */
data class Award(val kind: Kind, val points: Int, val n: Int = 0) {
    enum class Kind { ANSWERS, PERFECT, MASTERED, RECORD, CASCADE, DAILY, CHALLENGE_FOUND, CHALLENGE_DONE, HINTS, MISTAKES }
}

data class RoundOutcome(
    val record: Boolean,
    val awards: List<Award>,
    val pointsBefore: Int,
    val pointsAfter: Int,
    /** Lagere niveaus die door deze ronde ook als gehaald tellen. */
    val cascadedLevels: List<Int>,
) {
    val gained: Int get() = pointsAfter - pointsBefore
    val rankUp: Boolean get() = Ranks.of(pointsAfter) != Ranks.of(pointsBefore)
}

object Points {
    /** Punten per goed antwoord: ★ 10, ★★ 20, ★★★ 30, ★★★★ 40, ★★★★★ 60. */
    fun perAnswer(stars: Int) = when (stars) { 1 -> 10; 2 -> 20; 3 -> 30; 4 -> 40; else -> 60 }
}

/** Rangen op basis van je totaal aantal punten. */
data class Rank(val index: Int, val min: Int, val name: Txt)

object Ranks {
    val all = listOf(
        Rank(0, 0, Txt("Beginner", "Beginner")),
        Rank(1, 500, Txt("Explorer", "Verkenner")),
        Rank(2, 1_500, Txt("Navigator", "Navigator")),
        Rank(3, 4_000, Txt("Cartographer", "Cartograaf")),
        Rank(4, 10_000, Txt("Globetrotter", "Globetrotter")),
        Rank(5, 25_000, Txt("Atlas master", "Atlasmeester")),
        Rank(6, 60_000, Txt("Legend", "Legende")),
    )
    fun of(points: Int): Rank = all.last { points >= it.min }
    fun next(points: Int): Rank? = all.firstOrNull { it.min > points }

    /** 0..1: hoe ver je bent naar de volgende rang. */
    fun fraction(points: Int): Float {
        val cur = of(points); val nxt = next(points) ?: return 1f
        return (points - cur.min).toFloat() / (nxt.min - cur.min)
    }
}

/** Stand van een dagelijkse uitdaging. */
data class DailyState(
    val found: List<String> = emptyList(),
    val hints: Int = 0,
    val finished: Boolean = false,
    val points: Int = 0,
    val gaveUp: Boolean = false,
    val mistakes: Int = 0,
    val lost: Boolean = false,
)

/** Regels voor fouten in de dagelijkse uitdaging. */
object DailyRules {
    /** Zoveel fouten mag je maken; bij de volgende is het voorbij. */
    fun maxMistakes(hard: Boolean) = if (hard) 3 else 5
    /** Puntenaftrek per fout. */
    fun penalty(hard: Boolean) = if (hard) 10 else 5
}
