package nl.topo.app.data.content

import nl.topo.app.data.geo.Continent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Euro
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PinDrop
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.ui.graphics.vector.ImageVector

enum class RegionId { NEDERLAND, EUROPA, WERELD, AZIE, AFRIKA, NOORD_AMERIKA, ZUID_AMERIKA, OCEANIE }

enum class RegionKind { WORLD, CONTINENT, COUNTRY }

data class Topic(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
)

data class TopicGroup(val title: String, val subtitle: String, val topics: List<Topic>)

data class Level(val stars: Int, val title: String, val how: String)

data class Region(
    val id: RegionId,
    val name: String,
    val tagline: String,
    val available: Boolean,
    val kind: RegionKind,
    /** Bij een werelddeel: welk werelddeel op de kaart. */
    val continent: Continent? = null,
    /** Bij een land: ISO-code op de kaart (NLD). */
    val countryCode: String? = null,
    /** Wat elk niveau in deze regio betekent (index 0 = ★). */
    val levelScope: List<String>,
    val groups: List<TopicGroup>,
) {
    val topics: List<Topic> get() = groups.flatMap { it.topics }
    val cellCount: Int get() = topics.size * Levels.all.size
}

object Levels {
    val all = listOf(
        Level(1, "Makkelijk", "4 keuzes"),
        Level(2, "Gemiddeld", "4 lastigere keuzes"),
        Level(3, "Moeilijk", "6 lastige keuzes"),
        Level(4, "Expert", "zelf typen"),
    )
    /** Vanaf deze score telt een vakje als 'gekend'. */
    const val MASTERED = 90
    const val OK = 60
}

object Content {
    val nederland = Region(
        id = RegionId.NEDERLAND,
        name = "Nederland",
        tagline = "Provincies, steden en water",
        available = true,
        kind = RegionKind.COUNTRY,
        countryCode = "NLD",
        levelScope = listOf("de basis", "meer plaatsen", "alles", "alles"),
        groups = listOf(
            TopicGroup(
                "Topografie", "De kaart van Nederland",
                listOf(
                    Topic("provincies", "Provincies", "Tik de provincie aan", Icons.Rounded.Map),
                    Topic("hoofdsteden", "Hoofdsteden", "Van Assen tot Maastricht", Icons.Rounded.AccountBalance),
                    Topic("steden", "Steden", "De 50 bekendste steden", Icons.Rounded.LocationCity),
                    Topic("wadden", "Waddeneilanden", "Van Texel tot Schiermonnikoog", Icons.Rounded.BeachAccess),
                    Topic("wateren", "Wateren", "Rivieren, meren en zeeën", Icons.Rounded.Water),
                ),
            ),
        ),
    )

    val europa = Region(
        id = RegionId.EUROPA,
        name = "Europa",
        tagline = "Van Lissabon tot de Oeral",
        available = true,
        kind = RegionKind.CONTINENT,
        continent = Continent.EU,
        levelScope = listOf("16 bekende landen", "30 landen", "42 landen", "alle 50 landen"),
        groups = listOf(
            TopicGroup(
                "Landen", "Alles over de 50 landen van Europa",
                listOf(
                    Topic("kaart", "Zoeken op de kaart", "Tik het land aan", Icons.Rounded.Place),
                    Topic("hoofdstad", "Hoofdstad", "Wat is de hoofdstad?", Icons.Rounded.AccountBalance),
                    Topic("hoofdstad-kaart", "Hoofdstad op de kaart", "Tik de hoofdstad aan", Icons.Rounded.PinDrop),
                    Topic("vlag", "Vlag", "Van welk land is deze vlag?", Icons.Rounded.Flag),
                    Topic("contour", "Grenscontouren", "Welk land heeft deze vorm?", Icons.Rounded.Gesture),
                    Topic("buurlanden", "Buurlanden", "Welk land grenst eraan?", Icons.Rounded.Link),
                    Topic("taal", "Taal", "Welke taal is officieel?", Icons.Rounded.Translate),
                    Topic("valuta", "Valuta", "Met welk geld betaal je er?", Icons.Rounded.Euro),
                    Topic("lokaal", "Lokale naam", "Hoe noemt het land zichzelf?", Icons.Rounded.TextFields),
                    Topic("bevolking", "Bevolking", "Hoeveel inwoners?", Icons.Rounded.Groups),
                    Topic("oppervlakte", "Oppervlakte", "Hoe groot is het land?", Icons.Rounded.SquareFoot),
                    Topic("tld", "Internetextensie", ".nl, .de, .fr …", Icons.Rounded.Language),
                ),
            ),
            TopicGroup(
                "Kaart van Europa", "Steden, water en landschap",
                listOf(
                    Topic("steden", "Steden", "Grote steden buiten de hoofdsteden", Icons.Rounded.LocationCity),
                    Topic("rivieren", "Rivieren", "Van bron tot monding", Icons.Rounded.Water),
                    Topic("zeeen", "Zeeën en zeestraten", "Het water rond Europa", Icons.Rounded.Waves),
                    Topic("gebergten", "Gebergten", "Van Pyreneeën tot Kaukasus", Icons.Rounded.Terrain),
                    Topic("eilanden", "Eilanden", "Van Lofoten tot Kreta", Icons.Rounded.BeachAccess),
                ),
            ),
        ),
    )

    /** Onderwerpen die voor elk werelddeel werken. */
    private fun landTopics(extra: List<Topic> = emptyList()) = listOf(
        TopicGroup(
            "Landen", "Alle landen van dit gebied",
            listOf(
                Topic("kaart", "Zoeken op de kaart", "Tik het land aan", Icons.Rounded.Place),
                Topic("hoofdstad", "Hoofdstad", "Wat is de hoofdstad?", Icons.Rounded.AccountBalance),
                Topic("hoofdstad-kaart", "Hoofdstad op de kaart", "Tik de hoofdstad aan", Icons.Rounded.PinDrop),
                Topic("vlag", "Vlag", "Van welk land is deze vlag?", Icons.Rounded.Flag),
                Topic("contour", "Grenscontouren", "Welk land heeft deze vorm?", Icons.Rounded.Gesture),
                Topic("buurlanden", "Buurlanden", "Welk land grenst eraan?", Icons.Rounded.Link),
                Topic("bevolking", "Bevolking", "Hoeveel inwoners?", Icons.Rounded.Groups),
                Topic("oppervlakte", "Oppervlakte", "Hoe groot is het land?", Icons.Rounded.SquareFoot),
            ) + extra,
        ),
    )

    private val continentScope = listOf("bekendste landen", "meer landen", "bijna alle landen", "alle landen")

    private fun continent(id: RegionId, c: Continent, name: String, tagline: String) = Region(
        id = id, name = name, tagline = tagline, available = true,
        kind = RegionKind.CONTINENT, continent = c,
        levelScope = continentScope, groups = landTopics(),
    )

    val wereld = Region(
        id = RegionId.WERELD,
        name = "Wereld",
        tagline = "Alle landen, alle werelddelen",
        available = true,
        kind = RegionKind.WORLD,
        levelScope = listOf("bekendste landen", "meer landen", "bijna alle landen", "alle landen"),
        groups = landTopics(listOf(Topic("werelddeel", "Werelddeel", "In welk werelddeel ligt het?", Icons.Rounded.Public))),
    )

    val azie = continent(RegionId.AZIE, Continent.AS, "Azië", "Van Istanbul tot Tokio")
    val afrika = continent(RegionId.AFRIKA, Continent.AF, "Afrika", "Van Caïro tot Kaapstad")
    val noordAmerika = continent(RegionId.NOORD_AMERIKA, Continent.NA, "Noord-Amerika", "Van Groenland tot Panama")
    val zuidAmerika = continent(RegionId.ZUID_AMERIKA, Continent.SA, "Zuid-Amerika", "Van Colombia tot Vuurland")
    val oceanie = continent(RegionId.OCEANIE, Continent.OC, "Oceanië", "Australië en de eilanden van de Grote Oceaan")

    /** Volgorde van de keuzes op het startscherm. */
    val regions = listOf(wereld, europa, azie, afrika, noordAmerika, zuidAmerika, oceanie, nederland)

    /** Wat je bovenaan het startscherm kiest: de wereld en de werelddelen. */
    val mapRegions = regions.filter { it.kind != RegionKind.COUNTRY }

    fun forContinent(c: Continent): Region = regions.first { it.continent == c }

    /** Landen met eigen onderwerpen (nu alleen Nederland). */
    fun forCountry(code: String): Region? = regions.firstOrNull { it.countryCode == code }

    fun region(id: RegionId): Region = regions.first { it.id == id }
    fun topic(region: RegionId, topicId: String): Topic? = region(region).topics.firstOrNull { it.id == topicId }
}
