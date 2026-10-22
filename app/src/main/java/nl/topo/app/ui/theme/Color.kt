package nl.topo.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Nachtkaart: diepblauwe nacht, land in zacht leisteen, gloeiend groen en oranje als accenten. */
object Night {
    val sky = Color(0xFF0A1220)
    val skyDeep = Color(0xFF050A14)
    val skyHigh = Color(0xFF13233A)

    val surface = Color(0xFF101B2C)
    val surfaceHigh = Color(0xFF16253A)
    val surfaceHighest = Color(0xFF1D3049)
    val outline = Color(0xFF243753)
    val outlineSoft = Color(0xFF1A2A41)

    val land = Color(0xFF213350)
    val landHover = Color(0xFF2A4062)
    val landEdge = Color(0xFF86A9D0)
    val landDecor = Color(0xFF16233A)
    val water = Color(0xFF0C1626)
    val river = Color(0xFF2E5E86)
    val graticule = Color(0xFF14243A)

    // Hoog contrast (buiten, in daglicht): lichter land en bijna witte grenzen.
    val landHi = Color(0xFF30496E)
    val landHoverHi = Color(0xFF3A5884)
    val landEdgeHi = Color(0xFFE2ECF7)
    val landDecorHi = Color(0xFF1F2F48)

    val text = Color(0xFFE8F0F7)
    val muted = Color(0xFF8BA0B5)
    val faint = Color(0xFF5B7089)

    /** Hoofdaccent: goed, actief, gloed. */
    val glow = Color(0xFF3BE8A0)
    val glowDim = Color(0xFF1E7A57)
    /** Tweede accent: doel, aandacht, reeks. */
    val ember = Color(0xFFFF9F43)
    val gold = Color(0xFFFFC857)
    val danger = Color(0xFFFF6B6B)
    val mid = Color(0xFFF2C14E)
}
