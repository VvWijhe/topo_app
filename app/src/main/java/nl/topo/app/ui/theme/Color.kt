package nl.topo.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Alle kleuren van één thema. */
data class Palette(
    val isLight: Boolean,
    val sky: Color, val skyDeep: Color, val skyHigh: Color,
    val surface: Color, val surfaceHigh: Color, val surfaceHighest: Color,
    val outline: Color, val outlineSoft: Color,
    val land: Color, val landHover: Color, val landEdge: Color, val landDecor: Color,
    val water: Color, val river: Color, val graticule: Color,
    val landHi: Color, val landHoverHi: Color, val landEdgeHi: Color, val landDecorHi: Color,
    val text: Color, val muted: Color, val faint: Color,
    val glow: Color, val glowDim: Color, val ember: Color, val gold: Color, val danger: Color, val mid: Color,
    /** Tekst op een accentkleur (knoppen). */
    val onAccent: Color,
    /** Landkleuren voor de kaartstijl Atlas (buurlanden krijgen verschillende kleuren). */
    val atlas: List<Color> = emptyList(),
)

/** Nachtkaart: diepblauwe nacht, land in zacht leisteen, gloeiend groen en oranje als accenten. */
val NightPalette = Palette(
    isLight = false,
    sky = Color(0xFF0A1220), skyDeep = Color(0xFF050A14), skyHigh = Color(0xFF13233A),
    surface = Color(0xFF101B2C), surfaceHigh = Color(0xFF16253A), surfaceHighest = Color(0xFF1D3049),
    outline = Color(0xFF243753), outlineSoft = Color(0xFF1A2A41),
    land = Color(0xFF213350), landHover = Color(0xFF2A4062), landEdge = Color(0xFF86A9D0), landDecor = Color(0xFF16233A),
    water = Color(0xFF0C1626), river = Color(0xFF2E5E86), graticule = Color(0xFF14243A),
    // Hoog contrast (buiten, in daglicht): lichter land en bijna witte grenzen.
    landHi = Color(0xFF30496E), landHoverHi = Color(0xFF3A5884), landEdgeHi = Color(0xFFE2ECF7), landDecorHi = Color(0xFF1F2F48),
    text = Color(0xFFE8F0F7), muted = Color(0xFF8BA0B5), faint = Color(0xFF5B7089),
    glow = Color(0xFF3BE8A0), glowDim = Color(0xFF1E7A57), ember = Color(0xFFFF9F43), gold = Color(0xFFFFC857),
    danger = Color(0xFFFF6B6B), mid = Color(0xFFF2C14E),
    onAccent = Color(0xFF050A14),
    // Atlas bij nacht: diepe, gedempte kleuren, zodat de kaart donker blijft.
    atlas = listOf(
        Color(0xFF2E5577), Color(0xFF3A6450), Color(0xFF634A6E), Color(0xFF6A5636),
        Color(0xFF2B5E63), Color(0xFF6A4343), Color(0xFF4A5277),
    ),
)

/**
 * Dagkaart: rustige atlasstijl. Zachtblauwe zee, het gekozen gebied helder wit,
 * de rest van de wereld zacht grijs; dunne leisteengrenzen en dezelfde accenten, iets dieper voor contrast.
 */
val DayPalette = Palette(
    isLight = true,
    sky = Color(0xFFD6E6F3), skyDeep = Color(0xFFCADDEE), skyHigh = Color(0xFFE6F0F8),
    surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFF1F5F9), surfaceHighest = Color(0xFFE4EBF2),
    outline = Color(0xFFCAD5E1), outlineSoft = Color(0xFFDDE5ED),
    land = Color(0xFFE3E8EE), landHover = Color(0xFFFFFFFF), landEdge = Color(0xFF9AACBF), landDecor = Color(0xFFDCE2E9),
    water = Color(0xFFCDE1F1), river = Color(0xFF8FBCE0), graticule = Color(0xFFC4D8EA),
    // Hoog contrast: zelfde rust, maar duidelijkere (donker leisteen) grenzen.
    landHi = Color(0xFFEDF1F5), landHoverHi = Color(0xFFFFFFFF), landEdgeHi = Color(0xFF52657B), landDecorHi = Color(0xFFE0E6EC),
    text = Color(0xFF122030), muted = Color(0xFF4F6176), faint = Color(0xFF8395A8),
    glow = Color(0xFF0E9A69), glowDim = Color(0xFFBDEAD7), ember = Color(0xFFDD7414), gold = Color(0xFFB57F00),
    danger = Color(0xFFD23F3F), mid = Color(0xFFC0901A),
    onAccent = Color(0xFFFFFFFF),
    // Atlas bij dag: zachte pastels zoals in een schoolatlas.
    atlas = listOf(
        Color(0xFFF3D3A3), Color(0xFFCDE4B0), Color(0xFFF1C0C0), Color(0xFFC6D8F1),
        Color(0xFFE5CFEE), Color(0xFFF6E7A0), Color(0xFFBCE2DA),
    ),
)

/**
 * De kleuren van de app. Ze komen uit het actieve [palette], dat in MainActivity bij de
 * thema-instelling wordt gezet; omdat het een Compose-state is, kleurt alles direct mee.
 */
object Night {
    var palette: Palette by mutableStateOf(NightPalette)

    val isLight get() = palette.isLight
    val sky get() = palette.sky
    val skyDeep get() = palette.skyDeep
    val skyHigh get() = palette.skyHigh

    val surface get() = palette.surface
    val surfaceHigh get() = palette.surfaceHigh
    val surfaceHighest get() = palette.surfaceHighest
    val outline get() = palette.outline
    val outlineSoft get() = palette.outlineSoft

    val land get() = palette.land
    val landHover get() = palette.landHover
    val landEdge get() = palette.landEdge
    val landDecor get() = palette.landDecor
    val water get() = palette.water
    val river get() = palette.river
    val graticule get() = palette.graticule

    val landHi get() = palette.landHi
    val landHoverHi get() = palette.landHoverHi
    val landEdgeHi get() = palette.landEdgeHi
    val landDecorHi get() = palette.landDecorHi

    val text get() = palette.text
    val muted get() = palette.muted
    val faint get() = palette.faint

    /** Hoofdaccent: goed, actief, gloed. */
    val glow get() = palette.glow
    val glowDim get() = palette.glowDim
    /** Tweede accent: doel, aandacht, reeks. */
    val ember get() = palette.ember
    val gold get() = palette.gold
    val danger get() = palette.danger
    val mid get() = palette.mid
    val onAccent get() = palette.onAccent
    val atlas get() = palette.atlas
}
