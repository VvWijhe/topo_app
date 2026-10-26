package nl.topo.app.data.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * Resultaat van één dagelijkse uitdaging, zoals het naar de ranglijst gaat.
 * Alleen de eerste poging telt; daarna verandert het niet meer.
 */
@Serializable
data class ScoreSubmission(
    val playerId: String,
    val nickname: String,
    val day: Long,
    val difficulty: String,
    val points: Int,
    val found: Int,
    val total: Int,
    val mistakes: Int,
    val hints: Int,
    val timeMs: Long,
    val completed: Boolean,
    val submittedAt: Long,
)

data class LeaderboardEntry(
    val playerId: String,
    val nickname: String,
    val points: Int,
    val found: Int,
    val total: Int,
    val timeMs: Long,
    val isMe: Boolean,
)

enum class Period { DAY, WEEK }

/** Een vriend: zijn speler-ID, vriendcode en bijnaam (zoals bij het toevoegen). */
@Serializable
data class Friend(val playerId: String, val code: String, val nickname: String)

/** Je vriendenlijst, alleen op deze telefoon (volgen: je vriend hoeft jou niet terug toe te voegen). */
class FriendsStore(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("online/friends")
    private val json = Json { ignoreUnknownKeys = true }
    private val ser = ListSerializer(Friend.serializer())
    private fun decode(s: String?) = s?.let { runCatching { json.decodeFromString(ser, it) }.getOrNull() }.orEmpty()

    val friends: Flow<List<Friend>> = store.data.map { decode(it[key]) }

    suspend fun add(f: Friend) = store.edit { p -> p[key] = json.encodeToString(ser, decode(p[key]).filterNot { it.playerId == f.playerId } + f) }
    suspend fun remove(playerId: String) = store.edit { p -> p[key] = json.encodeToString(ser, decode(p[key]).filterNot { it.playerId == playerId }) }
}

/**
 * De ranglijst. Nu is er alleen [OfflineLeaderboard]; een online versie (Firebase)
 * implementeert dezelfde functies, zodat de schermen niet hoeven te veranderen.
 */
interface LeaderboardService {
    /** Verbonden met een online ranglijst? */
    val isOnline: Boolean

    /** Sla een resultaat op (offline: in de wachtrij; online: versturen). */
    suspend fun submit(s: ScoreSubmission)

    /**
     * Ranglijst: [Period.DAY] voor één dag en moeilijkheid, [Period.WEEK] = totaal van de week (beide moeilijkheden).
     * Met [only] alleen die spelers (plus jijzelf), bijv. je vrienden.
     */
    suspend fun ranking(day: Long, difficulty: String, period: Period, only: Set<String>? = null): List<LeaderboardEntry>

    /** Jouw vriendcode (online; maakt er een aan als je die nog niet hebt). Null als het niet lukt. */
    suspend fun myFriendCode(): String?

    /** Zoek een speler op zijn vriendcode. */
    suspend fun findFriend(code: String): Result<Friend>

    /** Bijnaam ook bij je vriendcode bijwerken. */
    suspend fun syncNickname() {}

    /** Nog niet verstuurde resultaten. */
    val pending: Flow<List<ScoreSubmission>>

    /** Probeer de wachtrij te versturen (online); geeft het aantal verstuurde terug. */
    suspend fun flush(): Int

    /** Verwijder al mijn online gegevens. */
    suspend fun deleteMyData()

    /** Staat er al een (lokaal bewaard) resultaat voor deze dag en moeilijkheid? */
    suspend fun hasResult(day: Long, difficulty: String): Boolean

    /** Laatste foutmelding bij versturen of ophalen (null = alles goed). */
    val problem: Flow<String?>

    companion object {
        /** Ranglijstvolgorde: meeste punten, dan snelste tijd. */
        val ORDER: Comparator<LeaderboardEntry> = compareByDescending<LeaderboardEntry> { it.points }.thenBy { it.timeMs }

        fun weekStart(day: Long): Long = LocalDate.ofEpochDay(day).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
    }
}

/** Speler: vaste anonieme ID en een zelfgekozen bijnaam. */
class PlayerProfile(private val store: DataStore<Preferences>) {
    private val idKey = stringPreferencesKey("online/playerId")
    private val nameKey = stringPreferencesKey("online/nickname")

    val nickname: Flow<String> = store.data.map { it[nameKey].orEmpty() }

    suspend fun playerId(): String {
        store.data.first()[idKey]?.let { return it }
        val id = UUID.randomUUID().toString()
        store.edit { if (it[idKey] == null) it[idKey] = id }
        return store.data.first()[idKey] ?: id
    }

    suspend fun currentNickname(): String = store.data.first()[nameKey].orEmpty()

    private val codeKey = stringPreferencesKey("online/friendCode")
    val friendCodeFlow: Flow<String?> = store.data.map { it[codeKey] }
    suspend fun friendCode(): String? = store.data.first()[codeKey]
    suspend fun setFriendCode(code: String?) { store.edit { if (code == null) it.remove(codeKey) else it[codeKey] = code } }

    suspend fun setNickname(name: String) {
        val clean = name.trim().replace(Regex("\\s+"), " ").take(MAX_NAME)
        store.edit { it[nameKey] = clean }
    }

    companion object { const val MAX_NAME = 20 }
}

/**
 * Ranglijst zonder internet: alle resultaten worden bewaard (ook als wachtrij voor later),
 * en de ranglijst toont alleen jouw eigen resultaten.
 */
class OfflineLeaderboard(private val store: DataStore<Preferences>, private val profile: PlayerProfile) : LeaderboardService {
    private val json = Json { ignoreUnknownKeys = true }
    private val ser = ListSerializer(ScoreSubmission.serializer())
    private val allKey = stringPreferencesKey("online/results")
    private val pendingKey = stringPreferencesKey("online/pending")

    override val isOnline: Boolean = false
    override val problem: Flow<String?> = kotlinx.coroutines.flow.flowOf(null)

    override suspend fun hasResult(day: Long, difficulty: String): Boolean =
        results().any { it.day == day && it.difficulty == difficulty }

    private fun decode(s: String?): List<ScoreSubmission> = s?.let { runCatching { json.decodeFromString(ser, it) }.getOrNull() }.orEmpty()

    override val pending: Flow<List<ScoreSubmission>> = store.data.map { decode(it[pendingKey]) }

    override suspend fun submit(s: ScoreSubmission) {
        store.edit { p ->
            val all = decode(p[allKey])
            // eerste poging telt: niet overschrijven
            if (all.any { it.day == s.day && it.difficulty == s.difficulty }) return@edit
            p[allKey] = json.encodeToString(ser, all + s)
            p[pendingKey] = json.encodeToString(ser, decode(p[pendingKey]) + s)
        }
    }

    override suspend fun myFriendCode(): String? = null
    override suspend fun findFriend(code: String): Result<Friend> = Result.failure(IllegalStateException("offline"))

    override suspend fun ranking(day: Long, difficulty: String, period: Period, only: Set<String>?): List<LeaderboardEntry> {
        val me = profile.playerId()
        val name = profile.currentNickname()
        val all = decode(store.data.first()[allKey])
        val mine = when (period) {
            Period.DAY -> all.filter { it.day == day && it.difficulty == difficulty }.map {
                LeaderboardEntry(me, name, it.points, it.found, it.total, it.timeMs, true)
            }
            Period.WEEK -> {
                val start = LeaderboardService.weekStart(day)
                val week = all.filter { it.day in start..day }
                if (week.isEmpty()) emptyList() else listOf(
                    LeaderboardEntry(me, name, week.sumOf { it.points }, week.sumOf { it.found }, week.sumOf { it.total }, week.sumOf { it.timeMs }, true),
                )
            }
        }
        return mine.sortedWith(LeaderboardService.ORDER)
    }

    override suspend fun flush(): Int = 0 // niets te versturen zonder online verbinding

    /** Alle eigen resultaten (ook al verstuurde). */
    suspend fun results(): List<ScoreSubmission> = decode(store.data.first()[allKey])

    suspend fun pendingNow(): List<ScoreSubmission> = decode(store.data.first()[pendingKey])

    /** Haal verstuurde resultaten uit de wachtrij. */
    suspend fun markSent(sent: List<ScoreSubmission>) {
        if (sent.isEmpty()) return
        val keys = sent.map { it.day to it.difficulty }.toSet()
        store.edit { p -> p[pendingKey] = json.encodeToString(ser, decode(p[pendingKey]).filterNot { (it.day to it.difficulty) in keys }) }
    }

    override suspend fun deleteMyData() {
        store.edit { it.remove(pendingKey) }
    }
}
