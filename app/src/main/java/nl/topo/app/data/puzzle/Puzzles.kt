package nl.topo.app.data.puzzle

import androidx.compose.ui.geometry.Rect
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.WorldMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** De puzzelsoorten. */
enum class PuzzleKind { ROUTE, MYSTERY }

/** Landgrenzen als graaf, zonder overzeese verbindingen die verwarren (Sint-Maarten, Frans-Guyana). */
object Borders {
    private val excluded = setOf(
        setOf("NLD", "FRA"), // Sint-Maarten
        setOf("FRA", "BRA"), setOf("FRA", "SUR"), // Frans-Guyana
        setOf("LKA", "IND"),
    )

    fun graph(w: WorldMap): Map<String, Set<String>> {
        val g = HashMap<String, MutableSet<String>>()
        for (c in w.all) for (n in c.neighbours) {
            if (n !in w.countries || setOf(c.code, n) in excluded) continue
            g.getOrPut(c.code) { mutableSetOf() } += n
            g.getOrPut(n) { mutableSetOf() } += c.code
        }
        return g
    }

    /** Afstand in grensovergangen vanaf [from]; alleen via [allowed] (null = alle landen). */
    fun distances(g: Map<String, Set<String>>, from: String, allowed: Set<String>? = null): Map<String, Int> {
        val dist = hashMapOf(from to 0)
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val a = queue.removeFirst()
            for (b in g[a].orEmpty()) {
                if (b in dist || (allowed != null && b !in allowed)) continue
                dist[b] = dist.getValue(a) + 1
                queue.addLast(b)
            }
        }
        return dist
    }
}

/**
 * Route: van [from] naar [to] over land, via zo min mogelijk landen.
 * [length] = aantal grensovergangen van de kortste route; je moet er [needed] landen tussen noemen.
 */
class RoutePuzzle(
    val from: String,
    val to: String,
    private val graph: Map<String, Set<String>>,
) {
    private val dFrom = Borders.distances(graph, from)
    private val dTo = Borders.distances(graph, to)
    val length: Int = dFrom.getValue(to)
    val needed: Int get() = length - 1
    /** Je mag een paar landen 'te veel' noemen. */
    val maxGuesses: Int get() = needed + 4

    /** Hoe goed past een geraden land? */
    enum class Fit { ON_ROUTE, CLOSE, OFF }

    fun fit(code: String): Fit {
        val a = dFrom[code] ?: return Fit.OFF
        val b = dTo[code] ?: return Fit.OFF
        return when (a + b) {
            length -> Fit.ON_ROUTE
            length + 1 -> Fit.CLOSE
            else -> Fit.OFF
        }
    }

    /** Vormen de geraden landen samen een aaneengesloten route van begin naar eind? */
    fun solved(guesses: Collection<String>): Boolean =
        to in Borders.distances(graph, from, guesses.toSet() + from + to)

    /** Eén kortste route (om te tonen als het niet lukte), bij voorkeur via jouw geraden landen. */
    fun route(prefer: Collection<String> = emptyList()): List<String> {
        val path = mutableListOf(from)
        var cur = from
        while (cur != to) {
            val next = graph[cur].orEmpty()
                .filter { (dFrom[it] ?: -1) == dFrom.getValue(cur) + 1 && (dTo[it] ?: Int.MAX_VALUE) == length - dFrom.getValue(cur) - 1 }
                .sortedWith(compareByDescending<String> { it in prefer }.thenBy { it })
                .firstOrNull() ?: break
            path += next; cur = next
        }
        return path
    }

    /** Hint: een land op een kortste route dat aansluit op wat je al hebt. */
    fun hint(guesses: Collection<String>): String? {
        val reach = Borders.distances(graph, from, guesses.toSet() + from).keys
        return graph.keys.filter { it !in guesses && it != from && it != to && fit(it) == Fit.ON_ROUTE && graph[it].orEmpty().any { n -> n in reach } }
            .minByOrNull { dFrom[it] ?: 99 }
    }

    /** Beeld rond begin, eind en een kortste route. */
    fun viewport(w: WorldMap): Rect {
        val boxes = route().mapNotNull { w.countries[it]?.bounds }
        var r = boxes.reduce { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
        val minSize = 60f
        if (r.width < minSize) r = Rect(r.center.x - minSize / 2, r.top, r.center.x + minSize / 2, r.bottom)
        if (r.height < minSize) r = Rect(r.left, r.center.y - minSize / 2, r.right, r.center.y + minSize / 2)
        val px = r.width * 0.25f; val py = r.height * 0.25f
        return Rect(r.left - px, r.top - py, r.right + px, r.bottom + py)
    }

    companion object {
        /** Kies een route van 3 tot 5 grensovergangen (2 tot 4 landen ertussen). */
        fun generate(w: WorldMap, rnd: Random): RoutePuzzle {
            val g = Borders.graph(w)
            val starts = w.all.filter { it.sovereign && it.population >= 1_000_000 && it.code in g }.sortedBy { it.code }
            repeat(200) {
                val a = starts.random(rnd)
                val d = Borders.distances(g, a.code)
                val ends = d.filter { (code, n) -> n in 3..5 && w.countries[code]?.let { it.sovereign && it.population >= 1_000_000 } == true }.keys.sorted()
                if (ends.isNotEmpty()) return RoutePuzzle(a.code, ends.random(rnd), g)
            }
            return RoutePuzzle("NLD", "AUT", g)
        }

        fun daily(w: WorldMap, day: Long) = generate(w, Random(day * 48271 + 11))
    }
}

/** Mysterieland: raad het land; elke gok zegt hoe ver (hoofdstad tot hoofdstad) en in welke richting het is. */
class MysteryPuzzle(val target: String) {
    data class Clue(val code: String, val km: Int, val bearing: Double, val closeness: Int)

    fun clue(w: WorldMap, code: String): Clue? {
        val a = w.countries[code]?.capitalLatLon ?: return null
        val b = w.countries[target]?.capitalLatLon ?: return null
        val km = Geo.km(a[0], a[1], b[0], b[1])
        val closeness = (100 * (1 - km / 20_000.0)).roundToInt().coerceIn(0, 100)
        return Clue(code, km.roundToInt(), Geo.bearing(a[0], a[1], b[0], b[1]), if (code == target) 100 else closeness.coerceAtMost(99))
    }

    companion object {
        fun pool(w: WorldMap): List<Country> =
            w.all.filter { it.sovereign && it.population >= 1_000_000 && it.capitalLatLon != null }.sortedBy { it.code }

        fun generate(w: WorldMap, rnd: Random) = MysteryPuzzle(pool(w).random(rnd).code)
        fun daily(w: WorldMap, day: Long) = generate(w, Random(day * 69621 + 7))
    }
}

object Geo {
    private const val R = 6371.0

    fun km(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
        val dp = p2 - p1; val dl = Math.toRadians(lon2 - lon1)
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * R * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Kompasrichting in graden (0 = noord, 90 = oost). */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2); val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }
}

/** Punten voor puzzels. Oefenen levert de helft op. */
object PuzzlePoints {
    fun route(needed: Int, guesses: Int, hints: Int, solved: Boolean): Int {
        if (!solved) return 0
        val spare = (needed + 4 - guesses).coerceAtLeast(0)
        return (40 + 20 * needed + 15 * spare - 15 * hints).coerceAtLeast(10)
    }

    fun mystery(guesses: Int, hints: Int, solved: Boolean): Int {
        if (!solved) return 0
        return (150 - 10 * (guesses - 1) - 20 * hints).coerceAtLeast(20)
    }
}
