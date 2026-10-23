package nl.topo.app.data.daily

import androidx.compose.ui.geometry.Rect
import nl.topo.app.data.geo.Continent
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.GeoShape
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.Lang
import nl.topo.app.data.i18n.Txt
import java.time.LocalDate
import kotlin.random.Random

enum class Difficulty { EASY, HARD }

/** Een lijn over de wereld die op de kaart getekend wordt. */
sealed interface GlobeLine {
    val name: Txt
    data class Latitude(val lat: Double, override val name: Txt) : GlobeLine
    data class Meridian(val lon: Double, override val name: Txt) : GlobeLine
}

/**
 * Een dagelijkse uitdaging: noem alle landen in [targets].
 * [subject] is het land dat gegeven is (bijv. Rusland bij 'buurlanden van Rusland').
 */
data class Challenge(
    val day: Long,
    val difficulty: Difficulty,
    val kind: Kind,
    val title: Txt,
    val targets: List<String>,
    val subject: String? = null,
    val line: GlobeLine? = null,
    val viewport: Rect,
) {
    enum class Kind { NEIGHBOURS, LINE, LETTER, TOP }
}

/**
 * Maakt elke dag dezelfde twee uitdagingen voor iedereen (vast zaad per datum).
 * Makkelijk: 3–8 landen, vooral bekende. Moeilijk: 8–20 landen, wereldwijd.
 */
object DailyChallenges {

    fun today(): Long = LocalDate.now().toEpochDay()

    fun forDay(w: WorldMap, day: Long, difficulty: Difficulty, lang: Lang): Challenge {
        val rnd = Random(day * 7919 + difficulty.ordinal * 104729)
        val kinds = if (difficulty == Difficulty.EASY)
            listOf(Challenge.Kind.NEIGHBOURS, Challenge.Kind.LETTER, Challenge.Kind.TOP, Challenge.Kind.NEIGHBOURS, Challenge.Kind.LINE)
        else
            listOf(Challenge.Kind.NEIGHBOURS, Challenge.Kind.LINE, Challenge.Kind.LETTER, Challenge.Kind.TOP)
        val kind = kinds[Math.floorMod(day, kinds.size.toLong()).toInt()]
        val c = when (kind) {
            Challenge.Kind.NEIGHBOURS -> neighbours(w, difficulty, rnd)
            Challenge.Kind.LINE -> line(w, difficulty, rnd)
            Challenge.Kind.LETTER -> letter(w, difficulty, rnd, lang)
            Challenge.Kind.TOP -> top(w, difficulty, rnd)
        }
        return c.copy(day = day, difficulty = difficulty)
    }

    private fun blank(kind: Challenge.Kind, title: Txt, targets: List<String>, viewport: Rect, subject: String? = null, line: GlobeLine? = null) =
        Challenge(0, Difficulty.EASY, kind, title, targets, subject, line, viewport)

    // ---------------- buurlanden ----------------

    private fun neighbours(w: WorldMap, d: Difficulty, rnd: Random): Challenge {
        val withNb = w.all.filter { c -> c.neighbours.count { it in w.countries } >= 3 }
        val pool = if (d == Difficulty.EASY) {
            // Bekende landen (veel inwoners) met 3 tot 7 buren
            withNb.filter { c -> c.neighbours.count { it in w.countries } in 3..7 }.sortedByDescending { it.population }.take(45)
        } else {
            withNb.filter { c -> c.neighbours.count { it in w.countries } >= 7 }
        }
        val s = pool.random(rnd)
        val targets = s.neighbours.filter { it in w.countries }
        return blank(
            Challenge.Kind.NEIGHBOURS,
            Txt("Name every country that borders ${s.names.en}", "Noem alle buurlanden van ${s.names.nl}"),
            targets, viewportFor(w, targets + s.code), subject = s.code,
        )
    }

    // ---------------- lijnen over de wereld ----------------

    private data class LineDef(val line: GlobeLine, val add: Set<String>, val remove: Set<String>, val hard: Boolean)

    private val lines = listOf(
        LineDef(GlobeLine.Latitude(0.0, Txt("the Equator", "de evenaar")), setOf("STP", "MDV", "KIR"), emptySet(), hard = true),
        LineDef(GlobeLine.Latitude(23.436, Txt("the Tropic of Cancer", "de Kreeftskeerkring")), emptySet(), setOf("MAR", "TCD"), hard = true),
        LineDef(GlobeLine.Latitude(-23.436, Txt("the Tropic of Capricorn", "de Steenbokskeerkring")), emptySet(), emptySet(), hard = true),
        LineDef(GlobeLine.Latitude(66.563, Txt("the Arctic Circle", "de noordpoolcirkel")), setOf("ISL"), emptySet(), hard = false),
        LineDef(GlobeLine.Meridian(0.0, Txt("the Greenwich meridian", "de nulmeridiaan van Greenwich")), emptySet(), emptySet(), hard = false),
    )

    private fun line(w: WorldMap, d: Difficulty, rnd: Random): Challenge {
        val options = lines.filter { it.hard == (d == Difficulty.HARD) }
        val def = options.random(rnd)
        val crossing = w.all.filter { c ->
            val shape = w.geo.countries[c.code] ?: return@filter false
            when (val l = def.line) {
                is GlobeLine.Latitude -> crossesY(shape, w.geo.projection.project(l.lat, 0.0).y)
                is GlobeLine.Meridian -> crossesX(shape, w.geo.projection.project(0.0, l.lon).x)
            }
        }.map { it.code }.toSet()
        val targets = ((crossing + def.add) - def.remove).filter { it in w.countries }
        val vp = viewportFor(w, targets).let { r ->
            when (def.line) {
                is GlobeLine.Latitude -> Rect(r.left, r.top - 40f, r.right, r.bottom + 40f)
                is GlobeLine.Meridian -> r
            }
        }
        val n = def.line.name
        return blank(Challenge.Kind.LINE, Txt("Name every country on ${n.en}", "Noem alle landen op ${n.nl}"), targets, vp, line = def.line)
    }

    /** Loopt een rand van deze vorm over de horizontale lijn y? (ringen zijn gesloten) */
    private fun crossesY(s: GeoShape, y: Float): Boolean = s.rings.any { r -> crosses(r, y, offset = 1) }

    /** Loopt een rand van deze vorm over de verticale lijn x? */
    private fun crossesX(s: GeoShape, x: Float): Boolean = s.rings.any { r -> crosses(r, x, offset = 0) }

    /** [offset] 0 = x-waarden, 1 = y-waarden in de ring (x,y,x,y …). */
    private fun crosses(r: FloatArray, v: Float, offset: Int): Boolean {
        val pts = r.size / 2
        for (k in 0 until pts) {
            val a = r[2 * k + offset]
            val b = r[2 * ((k + 1) % pts) + offset]
            if ((a - v) * (b - v) < 0) return true
        }
        return false
    }

    // ---------------- beginletter ----------------

    private fun letter(w: WorldMap, d: Difficulty, rnd: Random, lang: Lang): Challenge {
        val continent = if (d == Difficulty.EASY) listOf(Continent.EU, Continent.EU, Continent.AF).random(rnd) else null
        val pool = w.countriesIn(continent)
        fun initial(c: Country, l: Lang) = c.name(l).firstOrNull()?.uppercaseChar()
        val range = if (d == Difficulty.EASY) 3..6 else 8..18
        // Een letter die in beide talen een goed aantal landen oplevert
        val letters = ('A'..'Z').filter { ch -> Lang.entries.all { l -> pool.count { initial(it, l) == ch } in range } }
            .ifEmpty { ('A'..'Z').filter { ch -> pool.count { initial(it, lang) == ch } in range } }
            .ifEmpty { listOf('S') }
        val ch = letters.random(rnd)
        val targets = pool.filter { initial(it, lang) == ch }.map { it.code }
        val where = continent?.let { w.continents[it]?.names }
        val title = if (where != null) Txt("Name every country in ${where.en} that starts with $ch", "Noem alle landen in ${where.nl} die beginnen met een $ch")
        else Txt("Name every country in the world that starts with $ch", "Noem alle landen ter wereld die beginnen met een $ch")
        val vp = if (continent != null) w.continents[continent]?.viewport ?: w.worldViewport else w.worldViewport
        return blank(Challenge.Kind.LETTER, title, targets, vp)
    }

    // ---------------- toplijsten ----------------

    private fun top(w: WorldMap, d: Difficulty, rnd: Random): Challenge {
        val continent = if (d == Difficulty.EASY) listOf(Continent.EU, Continent.SA, Continent.AF, Continent.EU).random(rnd)
        else Continent.entries.filter { it != Continent.OC }.random(rnd)
        val n = if (d == Difficulty.EASY) 5 else 10
        val byArea = rnd.nextBoolean()
        val pool = w.countriesIn(continent).filter { it.sovereign }
        val targets = (if (byArea) pool.filter { it.areaKm2 != null }.sortedByDescending { it.areaKm2 } else pool.sortedByDescending { it.population })
            .take(n).map { it.code }
        val where = w.continents[continent]?.names ?: Txt("", "")
        val title = if (byArea) Txt("Name the $n largest countries of ${where.en} by area", "Noem de $n grootste landen van ${where.nl} (oppervlakte)")
        else Txt("Name the $n countries of ${where.en} with the most people", "Noem de $n landen van ${where.nl} met de meeste inwoners")
        return blank(Challenge.Kind.TOP, title, targets, w.continents[continent]?.viewport ?: w.worldViewport)
    }

    // ---------------- kaartkader ----------------

    private fun viewportFor(w: WorldMap, codes: List<String>): Rect {
        val bs = codes.mapNotNull { w.countries[it]?.bounds }
        if (bs.isEmpty()) return w.worldViewport
        var r = bs.first()
        for (b in bs.drop(1)) r = Rect(minOf(r.left, b.left), minOf(r.top, b.top), maxOf(r.right, b.right), maxOf(r.bottom, b.bottom))
        val padX = maxOf(r.width * 0.15f, 15f); val padY = maxOf(r.height * 0.15f, 15f)
        return Rect(r.left - padX, r.top - padY, r.right + padX, r.bottom + padY)
    }
}
