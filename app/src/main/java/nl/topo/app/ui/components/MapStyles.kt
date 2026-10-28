package nl.topo.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import nl.topo.app.data.geo.GeoMap
import nl.topo.app.data.i18n.Txt
import nl.topo.app.data.prefs.MapStyle
import nl.topo.app.ui.theme.DayPalette
import nl.topo.app.ui.theme.NightPalette
import kotlin.math.log10

/**
 * Hoe een kaartstijl getekend wordt. Landen worden in groepen met dezelfde kleur getekend ([palette] + [group]).
 *
 * @param sea eigen zeekleur over de hele kaart (null = de lucht van de app laat zich zien)
 * @param fillAlpha dekking van de landvulling (0 = alleen lijnen)
 * @param edgeByGroup grenzen in de kleur van de groep (Neon) in plaats van [edge]
 * @param dim hoeveel kleur de landen buiten het gekozen gebied houden (0..1)
 */
class StyleSpec(
    val sea: Color?,
    val palette: List<Color>,
    val group: Map<String, Int>,
    val fillAlpha: Float = 1f,
    val edge: Color,
    val edgeByGroup: Boolean = false,
    val edgeScale: Float = 1f,
    val graticule: Color,
    val dashedGraticule: Boolean = false,
    val decor: Color,
    val lakes: Color,
    val river: Color,
    val dim: Float = 0.4f,
    /** Basis waar gedimde landen naartoe mengen. */
    val base: Color,
)

/** Naam en korte uitleg per stijl, voor de keuzes. */
val MapStyle.title: Txt
    get() = when (this) {
        MapStyle.CLASSIC -> Txt("Classic", "Klassiek")
        MapStyle.ATLAS -> Txt("Atlas", "Atlas")
        MapStyle.PARCHMENT -> Txt("Parchment", "Perkament")
        MapStyle.BLUEPRINT -> Txt("Blueprint", "Blauwdruk")
        MapStyle.NEON -> Txt("Neon", "Neon")
        MapStyle.CONTINENTS -> Txt("Continents", "Werelddelen")
        MapStyle.DENSITY -> Txt("People per km²", "Inwoners/km²")
    }

val MapStyle.about: Txt
    get() = when (this) {
        MapStyle.CLASSIC -> Txt("One calm colour for all land", "Eén rustige kleur voor al het land")
        MapStyle.ATLAS -> Txt("Every country its own colour", "Elk land een eigen kleur")
        MapStyle.PARCHMENT -> Txt("An old explorer's map", "Een oude ontdekkingsreizigerskaart")
        MapStyle.BLUEPRINT -> Txt("Only lines, like a technical drawing", "Alleen lijnen, als een technische tekening")
        MapStyle.NEON -> Txt("A bright colour per continent", "Een felle kleur per werelddeel")
        MapStyle.CONTINENTS -> Txt("One colour per continent", "Eén kleur per werelddeel")
        MapStyle.DENSITY -> Txt("Darker = more crowded", "Hoe voller, hoe donkerder")
    }

private val continentOrder = listOf("EU", "AS", "AF", "NA", "SA", "OC")

/**
 * Kleur per land voor Atlas: buren (kaders die elkaar raken) krijgen verschillende kleuren.
 * Hebzuchtig, drukste landen eerst; anders de minst gebruikte kleur onder de buren.
 */
fun atlasGroups(map: GeoMap, n: Int): Map<String, Int> {
    val ids = map.countries.keys.sorted()
    val box = ids.associateWith { map.countries.getValue(it).bounds.inflate(1.5f) }
    val nb = ids.associateWith { a -> ids.filter { b -> b != a && box.getValue(a).overlaps(box.getValue(b)) } }
    val color = HashMap<String, Int>()
    for (id in ids.sortedByDescending { nb.getValue(it).size }) {
        val used = nb.getValue(id).mapNotNull { color[it] }
        color[id] = (0 until n).firstOrNull { it !in used } ?: (0 until n).minBy { c -> used.count { it == c } }
    }
    return color
}

private fun continentGroups(map: GeoMap): Map<String, Int> =
    map.countries.keys.associateWith { continentOrder.indexOf(map.continentOf[it]).coerceAtLeast(0) }

private const val DENSITY_STEPS = 8

/** Inwoners per km² op een log-schaal in 8 stappen (2 tot ~1000 per km²). */
private fun densityGroups(map: GeoMap): Map<String, Int> = map.countries.keys.associateWith { id ->
    val d = map.density[id] ?: return@associateWith 0
    if (d <= 0f) 0 else (((log10(d) - 0.3f) / 2.7f).coerceIn(0f, 0.999f) * DENSITY_STEPS).toInt()
}

private fun ramp(a: Color, b: Color, n: Int) = List(n) { lerp(a, b, it / (n - 1f)) }

/**
 * Stijl voor deze kaart; null = Klassiek (de gewone tekening).
 * Werelddelen/Neon/Inwoners hebben gegevens nodig die alleen de wereldkaart heeft; anders valt het terug op Atlas.
 */
fun styleSpec(style: MapStyle, map: GeoMap, light: Boolean, atlas: Map<String, Int>): StyleSpec? {
    val p = if (light) DayPalette else NightPalette
    val hasWorldData = map.continentOf.isNotEmpty()
    val s = if (!hasWorldData && style in setOf(MapStyle.NEON, MapStyle.CONTINENTS, MapStyle.DENSITY)) MapStyle.ATLAS else style
    return when (s) {
        MapStyle.CLASSIC -> null
        MapStyle.ATLAS -> StyleSpec(
            sea = null, palette = p.atlas, group = atlas, edge = p.landEdge,
            graticule = p.graticule, decor = p.landDecor, lakes = p.water, river = p.river, base = p.land,
        )
        MapStyle.PARCHMENT -> if (light) StyleSpec(
            sea = Color(0xFFEADBB8), palette = listOf(Color(0xFFF4E9CF)), group = emptyMap(), edge = Color(0xFF7A5A34),
            graticule = Color(0x598B6A3E), dashedGraticule = true, decor = Color(0xFFEFE2C4), lakes = Color(0xFFE3D2AA),
            river = Color(0xFF9C7B4E), dim = 0.55f, base = Color(0xFFEFE2C4),
        ) else StyleSpec(
            sea = Color(0xFF221A12), palette = listOf(Color(0xFF3B2E21)), group = emptyMap(), edge = Color(0xFFC9A46A),
            graticule = Color(0x40C9A46A), dashedGraticule = true, decor = Color(0xFF30261B), lakes = Color(0xFF2A2118),
            river = Color(0xFF8A6C44), dim = 0.55f, base = Color(0xFF30261B),
        )
        MapStyle.BLUEPRINT -> if (light) StyleSpec(
            sea = Color(0xFFF5F9FF), palette = listOf(Color(0xFFDCE8FB)), group = emptyMap(), fillAlpha = 0.45f, edge = Color(0xFF2F6FD6),
            edgeScale = 0.9f, graticule = Color(0x402F6FD6), decor = Color(0xFFEAF1FC), lakes = Color(0xFFF5F9FF),
            river = Color(0x992F6FD6), dim = 0.5f, base = Color(0xFFF5F9FF),
        ) else StyleSpec(
            sea = Color(0xFF0F3366), palette = listOf(Color(0xFF174784)), group = emptyMap(), fillAlpha = 0.6f, edge = Color(0xFFDCEBFF),
            edgeScale = 0.9f, graticule = Color(0x26FFFFFF), decor = Color(0xFF123B73), lakes = Color(0xFF0F3366),
            river = Color(0xFF9CC2FF), dim = 0.5f, base = Color(0xFF0F3366),
        )
        MapStyle.NEON -> if (light) StyleSpec(
            sea = Color(0xFFFAFBFD),
            palette = listOf(Color(0xFF00A3B8), Color(0xFFD6249F), Color(0xFFD98A00), Color(0xFF5E9E00), Color(0xFF00B386), Color(0xFF6B4BE0)),
            group = continentGroups(map), fillAlpha = 0.14f, edgeByGroup = true, edgeScale = 1.1f,
            graticule = Color(0x0F000000), decor = Color(0xFFF1F3F6), lakes = Color(0xFFFAFBFD), river = Color(0x6600A3B8),
            dim = 0.35f, base = Color(0xFFFAFBFD),
        ) else StyleSpec(
            sea = Color(0xFF05040A),
            palette = listOf(Color(0xFF00E5FF), Color(0xFFFF3DCB), Color(0xFFFFB300), Color(0xFFB4FF39), Color(0xFF33FFB0), Color(0xFF9B7BFF)),
            group = continentGroups(map), fillAlpha = 0.10f, edgeByGroup = true, edgeScale = 1.1f,
            graticule = Color(0x14FFFFFF), decor = Color(0xFF0B0A14), lakes = Color(0xFF05040A), river = Color(0x6600E5FF),
            dim = 0.35f, base = Color(0xFF05040A),
        )
        MapStyle.CONTINENTS -> StyleSpec(
            sea = null,
            palette = if (light) listOf(Color(0xFFC6D8F1), Color(0xFFF3D3A3), Color(0xFFF1C0C0), Color(0xFFCDE4B0), Color(0xFFE5CFEE), Color(0xFFBCE2DA))
            else listOf(Color(0xFF2E5577), Color(0xFF6A5636), Color(0xFF6A4343), Color(0xFF3A6450), Color(0xFF634A6E), Color(0xFF2B5E63)),
            group = continentGroups(map), edge = if (light) Color(0xFFFFFFFF) else p.sky,
            graticule = p.graticule, decor = p.landDecor, lakes = p.water, river = p.river, base = p.land,
        )
        MapStyle.DENSITY -> StyleSpec(
            sea = null,
            palette = if (light) ramp(Color(0xFFFFF4D6), Color(0xFFC0392B), DENSITY_STEPS) else ramp(Color(0xFF1C2B44), Color(0xFFFFB347), DENSITY_STEPS),
            group = densityGroups(map), edge = if (light) Color(0xFFFFFFFF) else p.sky,
            graticule = p.graticule, decor = p.landDecor, lakes = p.water, river = p.river, dim = 0.6f, base = p.land,
        )
    }
}
