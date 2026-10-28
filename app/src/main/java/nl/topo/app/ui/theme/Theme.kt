package nl.topo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private fun schemeFor(p: Palette) = run {
    if (p.isLight) lightColorScheme(
        primary = p.glow, onPrimary = p.onAccent, primaryContainer = p.glowDim, onPrimaryContainer = p.text,
        secondary = p.ember, onSecondary = p.onAccent, tertiary = p.gold,
        background = p.sky, onBackground = p.text, surface = p.surface, onSurface = p.text,
        surfaceVariant = p.surfaceHigh, onSurfaceVariant = p.muted,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surfaceHigh, surfaceContainerHighest = p.surfaceHighest,
        outline = p.outline, outlineVariant = p.outlineSoft, error = p.danger,
    ) else darkColorScheme(
        primary = p.glow, onPrimary = p.onAccent, primaryContainer = p.glowDim, onPrimaryContainer = p.text,
        secondary = p.ember, onSecondary = p.onAccent, tertiary = p.gold,
        background = p.sky, onBackground = p.text, surface = p.surface, onSurface = p.text,
        surfaceVariant = p.surfaceHigh, onSurfaceVariant = p.muted,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surfaceHigh, surfaceContainerHighest = p.surfaceHighest,
        outline = p.outline, outlineVariant = p.outlineSoft, error = p.danger,
    )
}

private val TopoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

/** Kaartstijl (instelling); wordt in MainActivity gezet. */
val LocalMapStyle = androidx.compose.runtime.staticCompositionLocalOf { nl.topo.app.data.prefs.MapStyle.CLASSIC }

/** Kaart met hoog contrast (instelling); wordt in MainActivity gezet. */
val LocalHighContrast = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun TopoTheme(palette: Palette, content: @Composable () -> Unit) {
    // Alle Night.*-kleuren volgen het gekozen palet.
    if (Night.palette != palette) Night.palette = palette
    MaterialTheme(colorScheme = schemeFor(palette), typography = TopoTypography, shapes = TopoShapes, content = content)
}
