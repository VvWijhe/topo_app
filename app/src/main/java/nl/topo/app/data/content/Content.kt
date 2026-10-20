package nl.topo.app.data.content

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
import nl.topo.app.data.geo.Continent
import nl.topo.app.data.i18n.Txt

enum class RegionId { NEDERLAND, EUROPA, WERELD, AZIE, AFRIKA, NOORD_AMERIKA, ZUID_AMERIKA, OCEANIE }

enum class RegionKind { WORLD, CONTINENT, COUNTRY }

data class Topic(
    val id: String,
    val name: Txt,
    val description: Txt,
    val icon: ImageVector,
)

data class TopicGroup(val title: Txt, val subtitle: Txt, val topics: List<Topic>)

data class Level(val stars: Int, val title: Txt, val how: Txt)

data class Region(
    val id: RegionId,
    val name: Txt,
    val tagline: Txt,
    val available: Boolean,
    val kind: RegionKind,
    /** Bij een werelddeel: welk werelddeel op de kaart. */
    val continent: Continent? = null,
    /** Bij een land: ISO-code op de kaart (NLD). */
    val countryCode: String? = null,
    /** Wat elk niveau in deze regio betekent (index 0 = ★). */
    val levelScope: List<Txt>,
    val groups: List<TopicGroup>,
) {
    val topics: List<Topic> get() = groups.flatMap { it.topics }
    val cellCount: Int get() = topics.size * Levels.all.size
}

object Levels {
    val all = listOf(
        Level(1, Txt("Easy", "Makkelijk"), Txt("4 choices", "4 keuzes")),
        Level(2, Txt("Medium", "Gemiddeld"), Txt("4 harder choices", "4 lastigere keuzes")),
        Level(3, Txt("Hard", "Moeilijk"), Txt("6 tricky choices", "6 lastige keuzes")),
        Level(4, Txt("Expert", "Expert"), Txt("type it yourself", "zelf typen")),
    )
    /** Vanaf deze score telt een vakje als 'gekend'. */
    const val MASTERED = 90
    const val OK = 60
}

private fun topic(id: String, en: String, nl: String, descEn: String, descNl: String, icon: ImageVector) =
    Topic(id, Txt(en, nl), Txt(descEn, descNl), icon)

object Content {
    private val tMap = topic("kaart", "Find on the map", "Zoeken op de kaart", "Tap the country", "Tik het land aan", Icons.Rounded.Place)
    private val tCapital = topic("hoofdstad", "Capital", "Hoofdstad", "What is the capital?", "Wat is de hoofdstad?", Icons.Rounded.AccountBalance)
    private val tCapitalMap = topic("hoofdstad-kaart", "Capital on the map", "Hoofdstad op de kaart", "Tap the capital", "Tik de hoofdstad aan", Icons.Rounded.PinDrop)
    private val tFlag = topic("vlag", "Flag", "Vlag", "Which country has this flag?", "Van welk land is deze vlag?", Icons.Rounded.Flag)
    private val tShape = topic("contour", "Outline", "Grenscontouren", "Which country has this shape?", "Welk land heeft deze vorm?", Icons.Rounded.Gesture)
    private val tBorders = topic("buurlanden", "Neighbours", "Buurlanden", "Which country borders it?", "Welk land grenst eraan?", Icons.Rounded.Link)
    private val tPop = topic("bevolking", "Population", "Bevolking", "How many people live there?", "Hoeveel inwoners?", Icons.Rounded.Groups)
    private val tArea = topic("oppervlakte", "Area", "Oppervlakte", "How big is the country?", "Hoe groot is het land?", Icons.Rounded.SquareFoot)

    val nederland = Region(
        id = RegionId.NEDERLAND,
        name = Txt("Netherlands", "Nederland"),
        tagline = Txt("Provinces, cities and water", "Provincies, steden en water"),
        available = true,
        kind = RegionKind.COUNTRY,
        countryCode = "NLD",
        levelScope = listOf(Txt("the basics", "de basis"), Txt("more places", "meer plaatsen"), Txt("everything", "alles"), Txt("everything", "alles")),
        groups = listOf(
            TopicGroup(
                Txt("Topography", "Topografie"), Txt("The map of the Netherlands", "De kaart van Nederland"),
                listOf(
                    topic("provincies", "Provinces", "Provincies", "Tap the province", "Tik de provincie aan", Icons.Rounded.Map),
                    topic("hoofdsteden", "Provincial capitals", "Hoofdsteden", "From Assen to Maastricht", "Van Assen tot Maastricht", Icons.Rounded.AccountBalance),
                    topic("steden", "Cities", "Steden", "The 50 best-known cities", "De 50 bekendste steden", Icons.Rounded.LocationCity),
                    topic("wadden", "Wadden Islands", "Waddeneilanden", "From Texel to Schiermonnikoog", "Van Texel tot Schiermonnikoog", Icons.Rounded.BeachAccess),
                    topic("wateren", "Waters", "Wateren", "Rivers, lakes and seas", "Rivieren, meren en zeeën", Icons.Rounded.Water),
                ),
            ),
        ),
    )

    val europa = Region(
        id = RegionId.EUROPA,
        name = Txt("Europe", "Europa"),
        tagline = Txt("From Lisbon to the Urals", "Van Lissabon tot de Oeral"),
        available = true,
        kind = RegionKind.CONTINENT,
        continent = Continent.EU,
        levelScope = listOf(Txt("best-known countries", "bekendste landen"), Txt("more countries", "meer landen"), Txt("almost all", "bijna alle landen"), Txt("all 50 countries", "alle 50 landen")),
        groups = listOf(
            TopicGroup(
                Txt("Countries", "Landen"), Txt("All about the 50 countries of Europe", "Alles over de 50 landen van Europa"),
                listOf(
                    tMap, tCapital, tCapitalMap, tFlag, tShape, tBorders,
                    topic("taal", "Language", "Taal", "Which language is official?", "Welke taal is officieel?", Icons.Rounded.Translate),
                    topic("valuta", "Currency", "Valuta", "What money do they use?", "Met welk geld betaal je er?", Icons.Rounded.Euro),
                    topic("lokaal", "Local name", "Lokale naam", "What does the country call itself?", "Hoe noemt het land zichzelf?", Icons.Rounded.TextFields),
                    tPop, tArea,
                    topic("tld", "Internet domain", "Internetextensie", ".nl, .de, .fr …", ".nl, .de, .fr …", Icons.Rounded.Language),
                ),
            ),
            TopicGroup(
                Txt("Map of Europe", "Kaart van Europa"), Txt("Cities, water and landscape", "Steden, water en landschap"),
                listOf(
                    topic("steden", "Cities", "Steden", "Big cities besides the capitals", "Grote steden buiten de hoofdsteden", Icons.Rounded.LocationCity),
                    topic("rivieren", "Rivers", "Rivieren", "From source to mouth", "Van bron tot monding", Icons.Rounded.Water),
                    topic("zeeen", "Seas and straits", "Zeeën en zeestraten", "The waters around Europe", "Het water rond Europa", Icons.Rounded.Waves),
                    topic("gebergten", "Mountains", "Gebergten", "From the Pyrenees to the Caucasus", "Van Pyreneeën tot Kaukasus", Icons.Rounded.Terrain),
                    topic("eilanden", "Islands", "Eilanden", "From Lofoten to Crete", "Van Lofoten tot Kreta", Icons.Rounded.BeachAccess),
                ),
            ),
        ),
    )

    /** Onderwerpen die voor elk werelddeel werken. */
    private fun landTopics(extra: List<Topic> = emptyList()) = listOf(
        TopicGroup(
            Txt("Countries", "Landen"), Txt("Every country in this area", "Alle landen van dit gebied"),
            listOf(tMap, tCapital, tCapitalMap, tFlag, tShape, tBorders, tPop, tArea) + extra,
        ),
    )

    private val continentScope = listOf(
        Txt("best-known countries", "bekendste landen"), Txt("more countries", "meer landen"),
        Txt("almost all", "bijna alle landen"), Txt("all countries", "alle landen"),
    )

    private fun continent(id: RegionId, c: Continent, name: Txt, tagline: Txt) = Region(
        id = id, name = name, tagline = tagline, available = true,
        kind = RegionKind.CONTINENT, continent = c,
        levelScope = continentScope, groups = landTopics(),
    )

    val wereld = Region(
        id = RegionId.WERELD,
        name = Txt("World", "Wereld"),
        tagline = Txt("Every country, every continent", "Alle landen, alle werelddelen"),
        available = true,
        kind = RegionKind.WORLD,
        levelScope = continentScope,
        groups = landTopics(listOf(topic("werelddeel", "Continent", "Werelddeel", "Which continent is it in?", "In welk werelddeel ligt het?", Icons.Rounded.Public))),
    )

    val azie = continent(RegionId.AZIE, Continent.AS, Txt("Asia", "Azië"), Txt("From Istanbul to Tokyo", "Van Istanbul tot Tokio"))
    val afrika = continent(RegionId.AFRIKA, Continent.AF, Txt("Africa", "Afrika"), Txt("From Cairo to Cape Town", "Van Caïro tot Kaapstad"))
    val noordAmerika = continent(RegionId.NOORD_AMERIKA, Continent.NA, Txt("North America", "Noord-Amerika"), Txt("From Greenland to Panama", "Van Groenland tot Panama"))
    val zuidAmerika = continent(RegionId.ZUID_AMERIKA, Continent.SA, Txt("South America", "Zuid-Amerika"), Txt("From Colombia to Tierra del Fuego", "Van Colombia tot Vuurland"))
    val oceanie = continent(RegionId.OCEANIE, Continent.OC, Txt("Oceania", "Oceanië"), Txt("Australia and the Pacific islands", "Australië en de eilanden van de Grote Oceaan"))

    /** Volgorde van de keuzes op het startscherm. */
    val regions = listOf(wereld, europa, azie, afrika, noordAmerika, zuidAmerika, oceanie, nederland)

    /** Wat je bovenaan het startscherm kiest: de wereld en de werelddelen. */
    val mapRegions = regions.filter { it.kind != RegionKind.COUNTRY }

    fun forContinent(c: Continent): Region = regions.first { it.continent == c }

    /** Landen met eigen onderwerpen (nu alleen Nederland). */
    fun forCountry(code: String): Region? = regions.firstOrNull { it.countryCode == code }

    /** Onderwerpen waarvan de quiz al echt werkt. */
    fun isPlayable(region: Region, topicId: String): Boolean = true

    fun region(id: RegionId): Region = regions.first { it.id == id }
    fun topic(region: RegionId, topicId: String): Topic? = region(region).topics.firstOrNull { it.id == topicId }
}
