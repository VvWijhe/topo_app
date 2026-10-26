package nl.topo.app.data.online

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import nl.topo.app.BuildConfig

/**
 * Online ranglijst met Firebase: anoniem inloggen + Firestore.
 *
 * Elk resultaat is één document `scores/{dag}_{moeilijkheid}_{uid}`; de Firestore-regels
 * staan alleen een eerste, eigen inzending toe. Resultaten gaan eerst in de lokale wachtrij
 * (via [local]), zodat er zonder internet niets verloren gaat.
 */
class FirebaseLeaderboard(
    private val local: OfflineLeaderboard,
    private val profile: PlayerProfile,
    app: FirebaseApp,
) : LeaderboardService {
    private val auth = FirebaseAuth.getInstance(app)
    private val db = FirebaseFirestore.getInstance(app)
    private val scores get() = db.collection("scores")
    private val lock = Mutex()

    override val isOnline: Boolean = true
    override val pending: Flow<List<ScoreSubmission>> = local.pending
    private val _problem = MutableStateFlow<String?>(null)
    override val problem: Flow<String?> = _problem

    override suspend fun hasResult(day: Long, difficulty: String) = local.hasResult(day, difficulty)

    private fun describe(e: Throwable): String = when (e) {
        is FirebaseFirestoreException -> "Firestore: ${e.code}"
        is kotlinx.coroutines.TimeoutCancellationException -> "Timeout (no internet?)"
        else -> (e::class.simpleName ?: "Error") + (e.message?.let { ": " + it.take(120) } ?: "")
    }

    private suspend fun uid(): String =
        auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: error("Inloggen mislukt")

    override suspend fun submit(s: ScoreSubmission) {
        local.submit(s)
        flush()
    }

    override suspend fun flush(): Int = lock.withLock {
        val queue = local.pendingNow()
        if (queue.isEmpty()) return 0
        val sent = mutableListOf<ScoreSubmission>()
        runCatching {
            withTimeout(15_000) {
                val me = uid()
                val name = profile.currentNickname()
                for (s in queue) {
                    val ref = scores.document("${s.day}_${s.difficulty}_$me")
                    val data = mapOf(
                        "playerId" to me,
                        "nickname" to (s.nickname.ifBlank { name }).take(PlayerProfile.MAX_NAME),
                        "day" to s.day,
                        "difficulty" to s.difficulty,
                        "points" to s.points.coerceAtLeast(0),
                        "found" to s.found,
                        "total" to s.total,
                        "mistakes" to s.mistakes,
                        "hints" to s.hints,
                        "timeMs" to s.timeMs,
                        "completed" to s.completed,
                        "submittedAt" to s.submittedAt,
                    )
                    try {
                        ref.set(data).await()
                        sent += s
                    } catch (e: FirebaseFirestoreException) {
                        // Al eerder verstuurd (overschrijven mag niet): dan is hij er dus al.
                        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && ref.get().await().exists()) sent += s
                        else throw e
                    }
                }
            }
        }.onSuccess { _problem.value = null }
            .onFailure { Log.w(TAG, "Versturen mislukt; later opnieuw", it); _problem.value = describe(it) }
        local.markSent(sent)
        sent.size
    }

    override suspend fun ranking(day: Long, difficulty: String, period: Period, only: Set<String>?): List<LeaderboardEntry> {
        flush()
        return runCatching {
            withTimeout(10_000) {
                val me = uid()
                when (period) {
                    // Eén veld per query: zo zijn er geen extra Firestore-indexen nodig.
                    Period.DAY -> scores.whereEqualTo("day", day).get().await().documents
                        .filter { it.getString("difficulty") == difficulty }
                        .map { d ->
                            LeaderboardEntry(
                                playerId = d.getString("playerId").orEmpty(),
                                nickname = d.getString("nickname").orEmpty(),
                                points = d.getLong("points")?.toInt() ?: 0,
                                found = d.getLong("found")?.toInt() ?: 0,
                                total = d.getLong("total")?.toInt() ?: 0,
                                timeMs = d.getLong("timeMs") ?: 0L,
                                isMe = d.getString("playerId") == me,
                            )
                        }
                    Period.WEEK -> {
                        val start = LeaderboardService.weekStart(day)
                        scores.whereGreaterThanOrEqualTo("day", start).whereLessThanOrEqualTo("day", day).get().await().documents
                            .groupBy { it.getString("playerId").orEmpty() }
                            .map { (pid, docs) ->
                                val latest = docs.maxByOrNull { it.getLong("submittedAt") ?: 0L }
                                LeaderboardEntry(
                                    playerId = pid,
                                    nickname = latest?.getString("nickname").orEmpty(),
                                    points = docs.sumOf { it.getLong("points")?.toInt() ?: 0 },
                                    found = docs.sumOf { it.getLong("found")?.toInt() ?: 0 },
                                    total = docs.sumOf { it.getLong("total")?.toInt() ?: 0 },
                                    timeMs = docs.sumOf { it.getLong("timeMs") ?: 0L },
                                    isMe = pid == me,
                                )
                            }
                    }
                }
            }
        }.map { online ->
            // Eigen uitslag nog niet online (bijv. nog in de wachtrij)? Dan toch tonen.
            val all = if (online.any { it.isMe }) online else online + local.ranking(day, difficulty, period)
            if (only == null) all else all.filter { it.isMe || it.playerId in only }
        }.getOrElse {
            Log.w(TAG, "Ranglijst ophalen mislukt; eigen resultaten tonen", it)
            _problem.value = describe(it)
            local.ranking(day, difficulty, period)
        }.sortedWith(LeaderboardService.ORDER).take(100)
    }

    override suspend fun deleteMyData() {
        runCatching {
            withTimeout(15_000) {
                val me = uid()
                scores.whereEqualTo("playerId", me).get().await().documents.forEach { it.reference.delete().await() }
                profile.friendCode()?.let { codes.document(it).delete().await() }
            }
        }
        profile.setFriendCode(null)
        local.deleteMyData()
    }

    // ---------------- vrienden ----------------

    private val codes get() = db.collection("codes")

    override suspend fun myFriendCode(): String? = runCatching {
        withTimeout(15_000) {
            profile.friendCode()?.let { return@withTimeout it }
            val me = uid()
            val name = profile.currentNickname()
            var made: String? = null
            repeat(8) {
                if (made != null) return@repeat
                val code = (1..CODE_LENGTH).map { CODE_CHARS.random() }.joinToString("")
                val ref = codes.document(code)
                if (!ref.get().await().exists()) {
                    ref.set(mapOf("uid" to me, "nickname" to name)).await()
                    made = code
                }
            }
            made?.also { profile.setFriendCode(it) }
        }
    }.onFailure { Log.w(TAG, "Vriendcode maken mislukt", it); _problem.value = describe(it) }.getOrNull()

    override suspend fun findFriend(code: String): Result<Friend> = runCatching {
        val clean = code.trim().uppercase().replace(" ", "")
        require(clean.length == CODE_LENGTH) { "code" }
        withTimeout(10_000) {
            uid()
            val d = codes.document(clean).get().await()
            if (!d.exists()) throw NoSuchElementException("not found")
            Friend(d.getString("uid").orEmpty(), clean, d.getString("nickname").orEmpty())
        }
    }

    override suspend fun syncNickname() {
        runCatching {
            withTimeout(10_000) {
                val code = profile.friendCode() ?: return@withTimeout
                uid()
                codes.document(code).update("nickname", profile.currentNickname()).await()
            }
        }
    }

    companion object {
        private const val TAG = "Ranglijst"
        private const val CODE_LENGTH = 6
        /** Zonder verwarrende tekens (0/O, 1/I/L). */
        private const val CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

        /** Start Firebase met de gegevens uit google-services.json (via BuildConfig); null als die ontbreken. */
        fun firebaseApp(context: Context): FirebaseApp? {
            if (BuildConfig.FIREBASE_PROJECT_ID.isBlank()) return null
            FirebaseApp.getApps(context).firstOrNull()?.let { return it }
            return runCatching {
                FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .build(),
                )
            }.onFailure { Log.w(TAG, "Firebase starten mislukt", it) }.getOrNull()
        }
    }
}
