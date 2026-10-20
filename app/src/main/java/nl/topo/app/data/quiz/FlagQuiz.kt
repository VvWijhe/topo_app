package nl.topo.app.data.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import nl.topo.app.data.geo.Continent
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.Lang
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Eén vlaggenvraag. [reverse]: je ziet het land en kiest de vlag. [options] leeg = zelf typen. */
data class FlagQuestion(val answer: Country, val reverse: Boolean, val options: List<Country>)

/** Wat er geantwoord is, voor de feedback. */
data class FlagAnswer(val correct: Boolean, val picked: Country?, val typed: String?)

object FlagQuiz {
    /** Hoeveel opties per niveau (0 = typen). */
    fun optionCount(level: Int) = when (level) { 1, 2 -> 4; 3 -> 6; else -> 0 }

    /**
     * Welke landen meedoen op dit niveau: de grootste landen eerst (naar inwoners),
     * zodat ★ begint met landen die je waarschijnlijk kent.
     */
    fun pool(world: WorldMap, continent: Continent?, level: Int): List<Country> {
        val all = world.countriesIn(continent).sortedByDescending { it.population }
        val n = all.size
        val l1 = min(n, max(10, (n * 0.35).roundToInt()))
        val l2 = min(n, max(l1, (n * 0.6).roundToInt()))
        val l3 = min(n, max(l2, (n * 0.85).roundToInt()))
        val size = when (level) { 1 -> l1; 2 -> l2; 3 -> l3; else -> n }
        return all.take(size)
    }

    fun question(world: WorldMap, continent: Continent?, level: Int, answer: Country, rnd: Random): FlagQuestion {
        val count = optionCount(level)
        if (count == 0) return FlagQuestion(answer, reverse = false, options = emptyList())
        val region = world.countriesIn(continent).filter { it.code != answer.code }
        val distractors = if (level == 1) {
            // ★: willekeurige andere landen uit de groep van dit niveau
            pool(world, continent, 1).filter { it.code != answer.code }.shuffled(rnd)
        } else {
            // ★★ en hoger: dichtbij gelegen landen, die vaak op elkaar lijkende vlaggen hebben
            region.sortedBy { (it.label - answer.label).getDistance() }.take(count + 4).shuffled(rnd)
        }
        val options = (distractors.distinctBy { it.flag }.filter { it.flag != answer.flag }.take(count - 1) + answer).shuffled(rnd)
        val reverse = level >= 2 && rnd.nextFloat() < if (level == 2) 0.3f else 0.45f
        return FlagQuestion(answer, reverse, options)
    }

    // ---------- zelf typen (★★★★) ----------

    private val aliases: Map<String, List<String>> = mapOf(
        "USA" to listOf("vs", "usa", "us", "amerika", "america", "verenigde staten van amerika", "united states of america"),
        "GBR" to listOf("vk", "uk", "groot-brittannie", "great britain", "britain", "engeland", "england", "brittannie"),
        "BLR" to listOf("wit-rusland", "witrusland", "belorussia", "byelorussia"),
        "CZE" to listOf("tsjechie", "tsjechische republiek", "czech republic"),
        "NLD" to listOf("holland", "the netherlands"),
        "CIV" to listOf("ivoorkust", "cote d'ivoire", "côte d'ivoire"),
        "COD" to listOf("dr congo", "drc", "democratische republiek congo", "democratic republic of the congo", "congo kinshasa"),
        "COG" to listOf("congo", "republiek congo", "congo brazzaville", "congo-brazzaville"),
        "MKD" to listOf("macedonie"),
        "ARE" to listOf("vae", "emiraten"),
        "KOR" to listOf("korea"),
        "MMR" to listOf("birma", "burma"),
        "SWZ" to listOf("swaziland"),
        "TLS" to listOf("timor-leste", "oost timor"),
        "BIH" to listOf("bosnie", "bosnie-herzegovina"),
        "PSE" to listOf("palestijnse gebieden"),
        "CPV" to listOf("kaapverdie"),
        "TUR" to listOf("turkiye", "türkiye"),
        "VAT" to listOf("vaticaan", "vatican", "holy see"),
    )

    private fun fold(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
            .lowercase().replace("’", "'").replace(Regex("[^a-z0-9]"), "")

    fun matches(typed: String, c: Country): Boolean {
        val t = fold(typed)
        if (t.isEmpty()) return false
        // Goed in elke taal van de app, plus bekende andere namen.
        val names = Lang.entries.map { c.name(it) } + aliases[c.code].orEmpty()
        return names.any { n ->
            val f = fold(n)
            // Eén tikfout is goed bij langere namen.
            f == t || (f.length >= 6 && levenshtein(f, t) <= 1)
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            System.arraycopy(cur, 0, prev, 0, cur.size)
        }
        return prev[b.length]
    }
}

/**
 * Een ronde van de vlaggenquiz. Houdt de stand bij als Compose-state, zodat het scherm meebeweegt.
 * Fout beantwoorde vlaggen komen aan het eind van de ronde nog één keer terug.
 */
class FlagGame(
    private val world: WorldMap,
    private val continent: Continent?,
    val level: Int,
    private val roundLength: Int?,
    private val rnd: Random = Random.Default,
) {
    val poolSize: Int = FlagQuiz.pool(world, continent, level).size

    private val queue = mutableStateListOf<Country>()
    private val retried = mutableSetOf<String>()

    /** Aantal vragen in de eerste ronde (zonder herhalingen). */
    var firstCount by mutableIntStateOf(0); private set
    var index by mutableIntStateOf(0); private set
    var correctFirstTry by mutableIntStateOf(0); private set
    var correctTotal by mutableIntStateOf(0); private set
    val misses = mutableStateListOf<Country>()
    var question by mutableStateOf<FlagQuestion?>(null); private set
    var answer by mutableStateOf<FlagAnswer?>(null); private set
    var finished by mutableStateOf(false); private set

    val total: Int get() = queue.size
    val percent: Int get() = if (firstCount == 0) 0 else (correctFirstTry * 100f / firstCount).roundToInt()

    init { restart() }

    fun restart() {
        val pool = FlagQuiz.pool(world, continent, level).shuffled(rnd)
        queue.clear(); queue.addAll(if (roundLength == null) pool else pool.take(roundLength))
        retried.clear(); misses.clear()
        firstCount = queue.size; index = 0; correctFirstTry = 0; correctTotal = 0; finished = false
        load()
    }

    private fun load() {
        answer = null
        question = queue.getOrNull(index)?.let { FlagQuiz.question(world, continent, level, it, rnd) }
    }

    private fun isFirstAttempt(c: Country) = index < firstCount

    fun pick(c: Country) {
        val q = question ?: return
        if (answer != null) return
        resolve(c.code == q.answer.code, picked = c, typed = null)
    }

    fun type(text: String) {
        val q = question ?: return
        if (answer != null || text.isBlank()) return
        resolve(FlagQuiz.matches(text, q.answer), picked = null, typed = text.trim())
    }

    private fun resolve(ok: Boolean, picked: Country?, typed: String?) {
        val q = question ?: return
        answer = FlagAnswer(ok, picked, typed)
        if (ok) {
            correctTotal++
            if (isFirstAttempt(q.answer)) correctFirstTry++
        } else {
            if (misses.none { it.code == q.answer.code }) misses.add(q.answer)
            if (retried.add(q.answer.code)) queue.add(q.answer)
        }
    }

    fun next() {
        if (answer == null) return
        index++
        if (index >= queue.size) { finished = true; question = null; answer = null } else load()
    }
}
