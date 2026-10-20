package nl.topo.app.data.geo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** Werelddelen zoals in de kaartdata (EU, AS, AF, NA, SA, OC). */
enum class Continent(val code: String) {
    EU("EU"), AS("AS"), AF("AF"), NA("NA"), SA("SA"), OC("OC");

    companion object {
        fun of(code: String): Continent? = entries.firstOrNull { it.code == code }
    }
}

data class Country(
    /** ISO-code met drie letters (NLD, DEU …). Kosovo = KOS. */
    val code: String,
    val name: String,
    /** ISO-code met twee letters, voor de vlag-emoji. */
    val iso2: String,
    val capital: String?,
    val capitalPos: Offset?,
    val population: Long,
    val areaKm2: Double?,
    /** Een land kan in twee werelddelen liggen (Rusland, Turkije). Het eerste is het 'hoofd'-werelddeel. */
    val continents: List<Continent>,
    val neighbours: List<String>,
    val label: Offset,
    /** Kader van het hoofdgebied (zonder verre overzeese delen). */
    val bounds: Rect,
    /** Onafhankelijke staat (Groenland, Westelijke Sahara … niet). */
    val sovereign: Boolean,
) {
    /** 🇳🇱 uit "NL". */
    val flag: String
        get() = if (iso2.length != 2) "🏳️" else buildString {
            iso2.uppercase().forEach { c -> appendCodePoint(0x1F1E6 + (c - 'A')) }
        }
}

data class ContinentInfo(val continent: Continent, val name: String, val viewport: Rect, val center: Offset)

class WorldMap(
    val geo: GeoMap,
    val countries: Map<String, Country>,
    val continents: Map<Continent, ContinentInfo>,
) {
    /** Hele wereld, zonder Antarctica. */
    val worldViewport: Rect = Rect(0f, geo.height * 0.04f, geo.width, geo.height * 0.84f)

    private val byContinent: Map<Continent, List<Country>> =
        Continent.entries.associateWith { c -> countries.values.filter { c in it.continents }.sortedWith(compareBy(DUTCH) { it.name }) }

    val all: List<Country> = countries.values.sortedWith(compareBy(DUTCH) { it.name })

    fun countriesIn(c: Continent?): List<Country> = if (c == null) all else byContinent[c].orEmpty()

    /** Land onder dit kaartpunt; valt terug op het dichtstbijzijnde label binnen [maxDist] (voor kleine eilandstaten). */
    fun countryAt(p: Offset, maxDist: Float, among: Collection<Country>? = null): Country? {
        geo.countryAt(p)?.let { code -> countries[code]?.let { return it } }
        val pool = among ?: countries.values
        return pool.minByOrNull { (it.label - p).getDistance() }?.takeIf { (it.label - p).getDistance() <= maxDist }
    }

    companion object {
        val DUTCH: java.text.Collator = java.text.Collator.getInstance(java.util.Locale("nl", "NL"))
    }
}
