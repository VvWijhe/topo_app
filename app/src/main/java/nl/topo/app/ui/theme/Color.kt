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
)

/** Dagkaart: lichtblauwe zee, crèmekleurig land, donkere tekst; dezelfde accenten, iets dieper voor contrast. */
val DayPalette = Palette(
    isLight = true,
    sky = Color(0xFFDCE9F4), skyDeep = Color(0xFFCFE0EE), skyHigh = Color(0xFFEAF2FA),
    surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFF0F4F9), surfaceHighest = Color(0xFFE3EAF2),
    outline = Color(0xFFC6D3E1), outlineSoft = Color(0xFFDCE5EE),
    land = Color(0xFFF7F2E6), landHover = Color(0xFFEDE1C4), landEdge = Color(0xFF7A8DA3), landDecor = Color(0xFFE6E0D2),
    water = Color(0xFFCFE2F0), river = Color(0xFF6FA6D2), graticule = Color(0xFFC7DAEA),
    landHi = Color(0xFFFFFFFF), landHoverHi = Color(0xFFF2E2B6), landEdgeHi = Color(0xFF1E2C3C), landDecorHi = Color(0xFFDCD5C5),
    text = Color(0xFF122030), muted = Color(0xFF4F6176), faint = Color(0xFF8395A8),
    glow = Color(0xFF0E9A69), glowDim = Color(0xFFBDEAD7), ember = Color(0xFFDD7414), gold = Color(0xFFB57F00),
    danger = Color(0xFFD23F3F), mid = Color(0xFFC0901A),
    onAccent = Color(0xFFFFFFFF),
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
}
