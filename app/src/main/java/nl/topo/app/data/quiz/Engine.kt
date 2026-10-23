package nl.topo.app.data.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.GeoMap
import nl.topo.app.data.geo.GeoShape
import nl.topo.app.data.i18n.Txt
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ======================= vragen =======================

/** Een antwoordknop. Met [flag] wordt een vlag getoond in plaats van tekst. */
data class Opt(val id: String, val label: Txt, val flag: Country? = null)

/** Wat de vraag laat zien. */
sealed interface Prompt {
    /** "Wat is de hoofdstad van" + "Frankrijk". */
    data class Words(val lead: Txt, val subject: Txt? = null, val note: Txt? = null, val flag: Country? = null) : Prompt
    /** Een grote vlag. */
    data class FlagPic(val lead: Txt, val country: Country) : Prompt
    /** Een silhouet van een land. */
    data class ShapePic(val lead: Txt, val shape: GeoShape) : Prompt
}

enum class Mode { CHOICE, TYPE, TAP }

data class Question(
    /** Het item waar de vraag over gaat (land, stad, rivier …). */
    val itemId: String,
    val prompt: Prompt,
    val mode: Mode,
    val options: List<Opt> = emptyList(),
    /** Goede keuzes (optie-ids) of goede kaartdelen (bij tikken). */
    val correct: Set<String>,
    /** Goede getypte antwoorden (alle talen en bekende andere namen). */
    val accept: List<String> = emptyList(),
    /** Het goede antwoord, voor de feedback. */
    val answer: Txt,
    /** Extra uitleg na het antwoord. */
    val info: Txt? = null,
    /** Kaartdeel dat uitgelicht wordt (bij 'typ wat hier ligt' en na het antwoord). */
    val focus: String? = null,
)

data class Given(val correct: Boolean, val pickedId: String?, val typed: String?)

// ======================= kaart bij een quiz =======================

/** Een aanklikbaar deel van de kaart. */
sealed interface Feature {
    val id: String
    val label: Txt
    data class Area(override val id: String, override val label: Txt, val shape: GeoShape, val labelAt: Offset) : Feature
    data class Point(override val id: String, override val label: Txt, val pos: Offset) : Feature
    data class Line(override val id: String, override val label: Txt, val shape: GeoShape) : Feature
}

/** De kaart die bij een quiz hoort: welke kaart, welk kader, en wat je kunt aantikken. */
data class QuizMap(
    val geo: GeoMap,
    val viewport: Rect,
    val features: List<Feature>,
    /** Landen van de kaart zelf die meedoen (gloeien zacht op). */
    val activeAreas: Set<String> = emptySet(),
    val graticule: Boolean = true,
    /** Extra vlakken om te tekenen (Waddeneilanden). */
    val extraAreas: List<Feature.Area> = emptyList(),
) {
    private val byId = features.associateBy { it.id }
    fun feature(id: String?): Feature? = id?.let { byId[it] }

    /** Welk kaartdeel ligt hier? [reach] in kaart-eenheden. */
    fun hit(p: Offset, reach: Float): Feature? {
        features.filterIsInstance<Feature.Area>().firstOrNull { it.shape.contains(p) }?.let { return it }
        var best: Feature? = null
        var bestD = Float.MAX_VALUE
        for (f in features) {
            val d = when (f) {
                is Feature.Point -> (f.pos - p).getDistance()
                is Feature.Line -> f.shape.distanceTo(p)
                is Feature.Area -> (f.labelAt - p).getDistance() * 1.4f
            }
            if (d < bestD) { bestD = d; best = f }
        }
        return best?.takeIf { bestD <= reach }
    }
}

// ======================= een quiz =======================

/** Alles wat een quizscherm nodig heeft. */
class QuizSetup(
    val title: Txt,
    val itemIds: List<String>,
    val map: QuizMap?,
    private val maker: (String, Random) -> Question,
) {
    fun question(id: String, rnd: Random) = maker(id, rnd)
}

/**
 * Een ronde. Fout beantwoorde vragen komen aan het eind nog één keer terug;
 * de score telt alleen de eerste poging.
 */
class QuizGame(private val setup: QuizSetup, private val roundLength: Int?, private val rnd: Random = Random.Default) {
    private val queue = mutableStateListOf<String>()
    private val retried = mutableSetOf<String>()

    var firstCount by mutableIntStateOf(0); private set
    var index by mutableIntStateOf(0); private set
    var correctFirstTry by mutableIntStateOf(0); private set
    var correctTotal by mutableIntStateOf(0); private set
    val misses = mutableStateListOf<Question>()
    var question by mutableStateOf<Question?>(null); private set
    var given by mutableStateOf<Given?>(null); private set
    var finished by mutableStateOf(false); private set

    val total: Int get() = queue.size
    val poolSize: Int get() = setup.itemIds.size
    val percent: Int get() = if (firstCount == 0) 0 else (correctFirstTry * 100f / firstCount).roundToInt()

    init { restart() }

    fun restart() {
        val pool = setup.itemIds.shuffled(rnd)
        queue.clear(); queue.addAll(if (roundLength == null) pool else pool.take(roundLength))
        retried.clear(); misses.clear()
        firstCount = queue.size; index = 0; correctFirstTry = 0; correctTotal = 0; finished = false
        load()
    }

    private fun load() {
        given = null
        question = queue.getOrNull(index)?.let { setup.question(it, rnd) }
    }

    fun pick(optionId: String) {
        val q = question ?: return
        if (given != null) return
        resolve(optionId in q.correct, optionId, null)
    }

    fun tap(featureId: String) {
        val q = question ?: return
        if (given != null) return
        resolve(featureId in q.correct, featureId, null)
    }

    fun type(text: String) {
        val q = question ?: return
        if (given != null || text.isBlank()) return
        resolve(Matching.matches(text, q.accept), null, text.trim())
    }

    private fun resolve(ok: Boolean, picked: String?, typed: String?) {
        val q = question ?: return
        given = Given(ok, picked, typed)
        if (ok) {
            correctTotal++
            if (index < firstCount) correctFirstTry++
        } else {
            if (misses.none { it.itemId == q.itemId }) misses.add(q)
            if (retried.add(q.itemId)) queue.add(q.itemId)
        }
    }

    fun next() {
        if (given == null) return
        index++
        if (index >= queue.size) { finished = true; question = null; given = null } else load()
    }
}

// ======================= hulpjes =======================

object Matching {
    /** Letters die Unicode niet in 'letter + accent' opsplitst. */
    private val special = mapOf(
        'ı' to "i", 'ł' to "l", 'đ' to "d", 'ð' to "d", 'þ' to "th", 'ħ' to "h", 'ø' to "o", 'æ' to "ae", 'œ' to "oe",
        'ß' to "ss", 'ə' to "e", 'ŀ' to "l", 'ŋ' to "n", 'ſ' to "s", 'ĸ' to "k", '’' to "'", 'ʼ' to "'", 'ʻ' to "'",
    )

    /**
     * Maakt een naam vergelijkbaar: geen hoofdletters, accenten, leestekens of spaties;
     * speciale letters naar gewone; 'ij' en 'y' gelijk; Saint/Sint/Sankt/St. vooraan gelijk.
     */
    fun fold(s: String): String {
        var x = Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        x = buildString { for (c in x) append(special[c] ?: c.toString()) }
        x = x.replace(Regex("[^a-z0-9]"), "")
        x = x.replace(Regex("^(sainte|saint|sankt|sint)"), "st")
        return x.replace("ij", "y")
    }

    private val article = Regex("^(the|de|het|la|le|les|el|al|il)\\s+")

    /** Varianten om te vergelijken: met en zonder lidwoord vooraan ("The Hague" ↔ "Hague"). */
    private fun variants(s: String): Set<String> {
        val low = s.lowercase().trim()
        return setOf(fold(low), fold(low.replace(article, ""))).filter { it.isNotEmpty() }.toSet()
    }

    /** Goed als een variant gelijk is; één tikfout mag vanaf 5 letters, twee vanaf 10. */
    fun matches(typed: String, accept: List<String>): Boolean {
        val ts = variants(typed)
        if (ts.isEmpty()) return false
        return accept.any { a ->
            variants(a).any { f ->
                val allowed = when { f.length >= 10 -> 2; f.length >= 5 -> 1; else -> 0 }
                ts.any { t -> f == t || (allowed > 0 && levenshtein(f, t, allowed) <= allowed) }
            }
        }
    }

    private fun levenshtein(a: String, b: String, max: Int): Int {
        if (kotlin.math.abs(a.length - b.length) > max) return max + 1
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            System.arraycopy(cur, 0, prev, 0, cur.size)
        }
        return prev[b.length]
    }
}

object Levels4 {
    /** Hoeveel items op elk niveau meedoen: de belangrijkste eerst. */
    fun size(n: Int, level: Int): Int {
        val l1 = min(n, max(10, (n * 0.35).roundToInt()))
        val l2 = min(n, max(l1, (n * 0.6).roundToInt()))
        val l3 = min(n, max(l2, (n * 0.85).roundToInt()))
        return when (level) { 1 -> l1; 2 -> l2; 3 -> l3; else -> n }
    }

    fun <T> pool(ordered: List<T>, level: Int): List<T> = ordered.take(size(ordered.size, level))

    /** Aantal keuzes per niveau (0 = zelf typen). */
    fun options(level: Int) = when (level) { 1, 2 -> 4; 3 -> 6; else -> 0 }
}
