package nl.topo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NightScheme = darkColorScheme(
    primary = Night.glow,
    onPrimary = Night.skyDeep,
    primaryContainer = Night.glowDim,
    onPrimaryContainer = Night.text,
    secondary = Night.ember,
    onSecondary = Night.skyDeep,
    tertiary = Night.gold,
    background = Night.sky,
    onBackground = Night.text,
    surface = Night.surface,
    onSurface = Night.text,
    surfaceVariant = Night.surfaceHigh,
    onSurfaceVariant = Night.muted,
    surfaceContainer = Night.surface,
    surfaceContainerHigh = Night.surfaceHigh,
    surfaceContainerHighest = Night.surfaceHighest,
    outline = Night.outline,
    outlineVariant = Night.outlineSoft,
    error = Night.danger,
)

private val TopoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

/** Kaart met hoog contrast (instelling); wordt in MainActivity gezet. */
val LocalHighContrast = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun TopoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NightScheme, typography = TopoTypography, shapes = TopoShapes, content = content)
}
