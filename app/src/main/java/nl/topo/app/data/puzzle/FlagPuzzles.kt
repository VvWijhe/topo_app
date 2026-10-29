package nl.topo.app.data.puzzle

import nl.topo.app.data.geo.Continent
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.FlagInfo
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.Txt
import kotlin.random.Random

/** Namen van vlagkenmerken: als losse aanwijzing ("Een ster") en in een zin ("met een ster in de vlag"). */
object FlagWords {
    /** Aanwijzing, met hoofdletter. */
    fun clue(tag: String): Txt = when (tag) {
        "h" -> Txt("Horizontal stripes", "Horizontale strepen")
        "v" -> Txt("Vertical stripes", "Verticale strepen")
        "nordic" -> Txt("A Scandinavian cross", "Een Scandinavisch kruis")
        "cross" -> Txt("A cross", "Een kruis")
        "union" -> Txt("The British Union Jack", "De Britse Union Jack")
        "star" -> Txt("One or more stars", "Een of meer sterren")
        "crescent" -> Txt("A crescent moon", "Een maansikkel")
        "sun" -> Txt("A sun", "Een zon")
        "circle" -> Txt("A circle or disc", "Een cirkel of schijf")
        "triangle" -> Txt("A triangle", "Een driehoek")
        "diagonal" -> Txt("A diagonal line or band", "Een schuine lijn of baan")
        "emblem" -> Txt("A coat of arms or emblem", "Een wapen of embleem")
        "animal" -> Txt("An animal or bird", "Een dier of vogel")
        "weapon" -> Txt("A weapon (sword, spear, gun…)", "Een wapen (zwaard, speer, geweer…)")
        "plant" -> Txt("A plant, leaf or tree", "Een plant, blad of boom")
        "text" -> Txt("Writing", "Tekst")
        "map" -> Txt("A map of the country", "Een kaart van het land")
        "building" -> Txt("A building", "Een gebouw")
        else -> Txt(tag, tag)
    }

    /** In een zin: "with a crescent moon" / "met een maansikkel". */
    fun with(tag: String): Txt = when (tag) {
        "star" -> Txt("a star", "een ster")
        "crescent" -> Txt("a crescent moon", "een maansikkel")
        "sun" -> Txt("a sun", "een zon")
        "cross" -> Txt("a cross", "een kruis")
        "circle" -> Txt("a circle or disc", "een cirkel of schijf")
        "triangle" -> Txt("a triangle", "een driehoek")
        "diagonal" -> Txt("a diagonal line", "een schuine lijn")
        "emblem" -> Txt("a coat of arms or emblem", "een wapen of embleem")
        "animal" -> Txt("an animal or bird", "een dier of vogel")
        "weapon" -> Txt("a weapon", "een wapen (zwaard, speer…)")
        "plant" -> Txt("a plant, leaf or tree", "een plant, blad of boom")
        "h" -> Txt("horizontal stripes", "horizontale strepen")
        "v" -> Txt("vertical stripes", "verticale strepen")
        "union" -> Txt("the Union Jack", "de Union Jack")
        "nordic" -> Txt("a Scandinavian cross", "een Scandinavisch kruis")
        "text" -> Txt("writing", "tekst")
        else -> Txt(tag, tag)
    }

    fun color(c: String): Txt = when (c) {
        "red" -> Txt("red", "rood"); "white" -> Txt("white", "wit"); "blue" -> Txt("blue", "blauw")
        "green" -> Txt("green", "groen"); "yellow" -> Txt("yellow", "geel"); "black" -> Txt("black", "zwart")
        "orange" -> Txt("orange", "oranje"); else -> Txt(c, c)
    }

    /** "red, white and blue" / "rood, wit en blauw". */
    fun colors(cs: List<String>): Txt {
        fun join(words: List<String>, and: String) = if (words.size <= 1) words.joinToString("") else words.dropLast(1).joinToString(", ") + " $and " + words.last()
        return Txt(join(cs.map { color(it).en }, "and"), join(cs.map { color(it).nl }, "en"))
    }

    val colorOrder = listOf("red", "white", "blue", "green", "yellow", "black", "orange")
}

/** Wie meedoet: onafhankelijke landen met een vlag; makkelijk = bekende landen (≥ 10 miljoen inwoners). */
fun flagPool(w: WorldMap, easy: Boolean): List<Country> =
    w.all.filter { it.sovereign && it.code in w.flags && (!easy || it.population >= 10_000_000) }.sortedBy { it.code }

/**
 * Vlag van dichtbij: je ziet een klein stukje van een vlag; bij elke foute gok zoom je verder uit.
 * [stages] = hoe groot het stukje is (deel van de vlaghoogte); 1 = de hele vlag.
 */
class FlagZoomPuzzle(val target: String, val spotX: Float, val spotY: Float, val stages: List<Float>) {
    /** Gokken: één per stap, plus één met de hele vlag. */
    val maxGuesses: Int get() = stages.size + 1

    companion object {
        fun generate(w: WorldMap, rnd: Random, hard: Boolean): FlagZoomPuzzle {
            val c = flagPool(w, easy = !hard).random(rnd)
            val spot = w.flags.getValue(c.code).spots.ifEmpty { listOf(androidx.compose.ui.geometry.Offset(0.5f, 0.5f)) }.random(rnd)
            val stages = if (hard) listOf(0.22f, 0.32f, 0.46f, 0.66f) else listOf(0.34f, 0.5f, 0.72f)
            return FlagZoomPuzzle(c.code, spot.x, spot.y, stages)
        }
    }
}

/** Vlag-omschrijving: raad het land aan de hand van aanwijzingen over de vlag; elke foute gok geeft een nieuwe. */
class FlagCluesPuzzle(val target: String, val clues: List<Txt>, val shownAtStart: Int) {
    /** Na de laatste aanwijzing nog twee kansen. */
    val maxGuesses: Int get() = clues.size - shownAtStart + 2

    companion object {
        private val layout = listOf("nordic", "union", "h", "v", "cross", "diagonal", "triangle", "circle")
        private val symbols = listOf("star", "crescent", "sun", "map", "animal", "weapon", "plant", "building", "emblem", "text")

        fun generate(w: WorldMap, rnd: Random, hard: Boolean): FlagCluesPuzzle {
            val c = flagPool(w, easy = !hard).random(rnd)
            val f: FlagInfo = w.flags.getValue(c.code)
            val shape = layout.filter { it in f.tags }
                // Scandinavisch kruis of Union Jack zegt al dat er een kruis is
                .let { l -> if ("nordic" in l || "union" in l) l - "cross" else l }
                .map { FlagWords.clue(it) }
            val sym = symbols.filter { it in f.tags }.map { FlagWords.clue(it) }
            val colors = FlagWords.colorOrder.filter { it in f.colors }
            val colorClue = Txt(
                "Colours: " + FlagWords.colors(colors).en,
                "Kleuren: " + FlagWords.colors(colors).nl,
            ).takeIf { colors.isNotEmpty() }
            val none = Txt("No stripes, cross or other division", "Geen strepen, kruis of andere verdeling").takeIf { shape.isEmpty() }
            val home = c.continents.firstOrNull()?.let { w.continents[it]?.names }?.let { Txt("Continent: ${it.en}", "Werelddeel: ${it.nl}") }
            val letter = Txt("Starts with “${c.names.en.first()}”", "Begint met “${c.names.nl.first()}”")
            val clues = buildList {
                if (hard) {
                    // Moeilijk: eerst de vorm, kleuren pas later
                    addAll(shape); none?.let { add(it) }; addAll(sym); colorClue?.let { add(it) }
                } else {
                    colorClue?.let { add(it) }; addAll(shape); none?.let { add(it) }; addAll(sym)
                }
                home?.let { add(it) }
                add(letter)
            }
            return FlagCluesPuzzle(c.code, clues, shownAtStart = if (hard) 1 else 2)
        }
    }
}

/** Puzzelpunten voor de vlagpuzzels. */
object FlagPoints {
    fun zoom(wrong: Int, solved: Boolean, hard: Boolean) = if (!solved) 0 else ((120 - 20 * wrong).coerceAtLeast(20) * (if (hard) 1.5f else 1f)).toInt()
    fun clues(wrong: Int, solved: Boolean, hard: Boolean) = if (!solved) 0 else ((140 - 20 * wrong).coerceAtLeast(20) * (if (hard) 1.5f else 1f)).toInt()
}

/** Vlagkenmerken die als "noem alle landen met…"-uitdaging werken. */
object FlagFeatures {
    sealed interface Rule {
        val title: Txt
        fun test(f: FlagInfo): Boolean
    }

    data class Has(val tag: String) : Rule {
        override val title get() = FlagWords.with(tag)
        override fun test(f: FlagInfo) = tag in f.tags
    }

    data class HasColor(val color: String) : Rule {
        override val title get() = Txt("${FlagWords.color(color).en} in it", "${FlagWords.color(color).nl} erin")
        override fun test(f: FlagInfo) = color in f.colors
    }

    /** Precies deze kleuren en geen andere. */
    data class OnlyColors(val colors: Set<String>) : Rule {
        override val title get() = FlagWords.colors(FlagWords.colorOrder.filter { it in colors }).let { Txt("only ${it.en}", "alleen ${it.nl}") }
        // Ook kleine details tellen (de gele zon van Argentinië), en een embleem heeft vrijwel altijd andere kleuren.
        override fun test(f: FlagInfo) = f.allColors == colors && "emblem" !in f.tags
    }

    private val tagRules = listOf("star", "crescent", "sun", "cross", "circle", "triangle", "diagonal", "emblem", "animal", "weapon", "plant", "h", "v", "union", "nordic").map { Has(it) }
    private val colorRules = listOf("green", "black", "orange", "yellow", "blue").map { HasColor(it) }
    private val onlyRules = listOf(setOf("red", "white"), setOf("red", "white", "blue"), setOf("green", "white"), setOf("red", "yellow"),
        setOf("blue", "white"), setOf("red", "white", "green"), setOf("green", "yellow", "red"), setOf("black", "red", "yellow")).map { OnlyColors(it) }
    val all: List<Rule> = tagRules + colorRules + onlyRules

    /**
     * Kies een regel en een gebied met een goed aantal landen.
     * Makkelijk: binnen één werelddeel, 3–8 landen. Moeilijk: wereldwijd 6–25, of een werelddeel met 8–20.
     */
    fun pick(w: WorldMap, rnd: Random, hard: Boolean): Triple<Rule, Continent?, List<String>> {
        val conts = listOf(Continent.EU, Continent.AF, Continent.AS, Continent.NA, Continent.SA, Continent.OC)
        repeat(300) {
            val rule = all.random(rnd)
            val where: Continent? = if (hard && rnd.nextBoolean()) null else conts.random(rnd)
            val pool = w.countriesIn(where).filter { it.sovereign && it.code in w.flags }
            val hits = pool.filter { rule.test(w.flags.getValue(it.code)) }.map { it.code }
            val ok = when {
                !hard -> hits.size in 3..8
                where == null -> hits.size in 6..25
                else -> hits.size in 8..20
            }
            if (ok) return Triple(rule, where, hits)
        }
        val hits = w.countriesIn(Continent.EU).filter { it.code in w.flags && "nordic" in w.flags.getValue(it.code).tags }.map { it.code }
        return Triple(Has("nordic"), Continent.EU, hits)
    }
}
