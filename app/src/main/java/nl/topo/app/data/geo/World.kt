package nl.topo.app.data.geo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import nl.topo.app.data.i18n.Lang
import nl.topo.app.data.i18n.Txt

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
    /** Naam in elke taal. */
    val names: Txt,
    /** ISO-code met twee letters, voor de vlag. */
    val iso2: String,
    val capitals: Txt?,
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
    /** Hoofdstad als [breedte, lengte] in graden. */
    val capitalLatLon: List<Double>? = null,
) {
    fun name(lang: Lang): String = names[lang]
    fun capital(lang: Lang): String? = capitals?.get(lang)

    /** Bestandsnaam van de vlag in assets/flags. */
    val flagAsset: String get() = "flags/${iso2.lowercase()}.webp"

    /** 🇳🇱 uit "NL" (reserve, als de afbeelding ontbreekt). */
    val flag: String
        get() = if (iso2.length != 2) "🏳️" else buildString {
            iso2.uppercase().forEach { c -> appendCodePoint(0x1F1E6 + (c - 'A')) }
        }
}

/**
 * Kenmerken van een vlag.
 * [colors]: red, white, blue, green, yellow, black, orange. [tags]: h, v, cross, nordic, union, star, crescent, sun,
 * circle, triangle, diagonal, emblem, animal, weapon, plant, text, map, building.
 * [spots]: interessante punten (0..1) voor een uitsnede; [ratio]: breedte/hoogte.
 */
data class FlagInfo(val colors: Set<String>, val tags: Set<String>, val spots: List<Offset>, val ratio: Float)

data class ContinentInfo(val continent: Continent, val names: Txt, val viewport: Rect, val center: Offset) {
    fun name(lang: Lang): String = names[lang]
}

class WorldMap(
    val geo: GeoMap,
    val countries: Map<String, Country>,
    val continents: Map<Continent, ContinentInfo>,
    /** Grote Europese rivieren, op Nederlandse naam (Rijn, Donau …). */
    val rivers: Map<String, GeoShape> = emptyMap(),
    /** Internetextensie per land (.nl). */
    val tld: Map<String, String> = emptyMap(),
    /** Vlagkenmerken per land (ISO3-code). */
    val flags: Map<String, FlagInfo> = emptyMap(),
) {
    /** Hele wereld, zonder Antarctica. */
    val worldViewport: Rect = Rect(0f, geo.height * 0.04f, geo.width, geo.height * 0.84f)

    private val byContinent: Map<Continent, List<Country>> =
        Continent.entries.associateWith { c -> countries.values.filter { c in it.continents } }

    val all: List<Country> = countries.values.toList()

    fun countriesIn(c: Continent?): List<Country> = if (c == null) all else byContinent[c].orEmpty()

    /** Land onder dit kaartpunt; valt terug op het dichtstbijzijnde label binnen [maxDist] (voor kleine eilandstaten). */
    fun countryAt(p: Offset, maxDist: Float, among: Collection<Country>? = null): Country? {
        geo.countryAt(p)?.let { code -> countries[code]?.let { return it } }
        val pool = among ?: countries.values
        return pool.minByOrNull { (it.label - p).getDistance() }?.takeIf { (it.label - p).getDistance() <= maxDist }
    }

}
