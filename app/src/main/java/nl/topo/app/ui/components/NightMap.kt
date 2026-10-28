package nl.topo.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.topo.app.data.geo.GeoMap
import nl.topo.app.ui.theme.LocalHighContrast
import nl.topo.app.ui.theme.Night
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/** Rekent kaart-eenheden om naar schermpixels en terug. */
class MapTransform(val scale: Float, val dx: Float, val dy: Float) {
    fun toScreen(p: Offset) = Offset(p.x * scale + dx, p.y * scale + dy)
    fun toMap(s: Offset) = Offset((s.x - dx) / scale, (s.y - dy) / scale)

    companion object {
        /** Past het kader in de ruimte, zonder [padTop]/[padBottom] pixels aan boven- en onderkant. */
        fun fit(size: Size, viewport: Rect, padTop: Float = 0f, padBottom: Float = 0f): MapTransform {
            val h = (size.height - padTop - padBottom).coerceAtLeast(1f)
            val s = min(size.width / viewport.width, h / viewport.height)
            return MapTransform(
                s,
                (size.width - viewport.width * s) / 2f - viewport.left * s,
                padTop + (h - viewport.height * s) / 2f - viewport.top * s,
            )
        }
    }
}

/** Eén detailniveau van de kaart: hoe grover, hoe minder punten om te tekenen. */
private class MapLevel(
    /** Maximale afwijking in kaart-eenheden (0 = volledig detail). */
    val tol: Float,
    val countries: Map<String, Path>,
    val background: Path,
    val lakes: Path,
)

private class MapPaths(val levels: List<MapLevel>, val rivers: List<Path>, val graticule: List<Path>)

/** Alle landen behalve [skip] samengevoegd tot één vlak en één grenslijn: veel sneller dan 200 losse paden. */
private class Merged(val fill: Path, val edge: Path)

private fun merge(level: MapLevel, skip: Set<String>, borders: Set<String>?): Merged {
    val fill = Path().apply { fillType = PathFillType.EvenOdd }
    val edge = Path()
    for ((id, p) in level.countries) {
        if (id in skip) continue
        fill.addPath(p)
        if (borders == null || id in borders) edge.addPath(p)
    }
    return Merged(fill, edge)
}

private fun buildLevel(map: GeoMap, tol: Float) = MapLevel(
    tol,
    countries = map.countries.mapValues { it.value.simplified(tol).toPath() },
    background = Path().apply { fillType = PathFillType.EvenOdd; map.background.forEach { addPath(it.simplified(tol).toPath()) } },
    lakes = Path().apply { fillType = PathFillType.EvenOdd; map.lakes.forEach { addPath(it.simplified(tol).toPath()) } },
)

private fun buildGraticule(map: GeoMap): List<Path> = buildList {
    for (lon in -180..180 step 15) add(Path().apply { for (lat in -80..85) { val p = map.projection.project(lat.toDouble(), lon.toDouble()); if (lat == -80) moveTo(p.x, p.y) else lineTo(p.x, p.y) } })
    for (lat in -75..75 step 15) add(Path().apply { for (lon in -180..180 step 2) { val p = map.projection.project(lat.toDouble(), lon.toDouble()); if (lon == -180) moveTo(p.x, p.y) else lineTo(p.x, p.y) } })
}

/** Afwijkingen per niveau, als deel van de kaartbreedte (op de wereldkaart: 0,35 · 1 · 2,5 eenheden van 2000). */
private val LevelTolerances = floatArrayOf(0f, 0.35f / 2000f, 1f / 2000f, 2.5f / 2000f)

/**
 * De kaart in Nachtkaart-stijl.
 *
 * De kaart zelf wordt alleen opnieuw getekend als het kader of de selectie verandert;
 * de kloppende ring zit in een eigen laag, zodat de (grote) wereldkaart niet elk frame opnieuw hoeft.
 *
 * @param glow landen die oplichten
 * @param pulseAt kaartpunt waar een kloppende ring op staat
 * @param onTap tik op de kaart, in kaart-eenheden, met de huidige omrekening
 * @param overlay composables bovenop de kaart; gebruik [anchorAt] om ze op een kaartpunt te zetten
 */
@Composable
fun NightMap(
    map: GeoMap,
    viewport: Rect,
    modifier: Modifier = Modifier,
    padTop: Dp = 0.dp,
    padBottom: Dp = 0.dp,
    glow: Set<String> = emptySet(),
    glowColor: Color = Night.glow,
    pulseAt: Offset? = null,
    graticule: Boolean = true,
    /** Alleen deze landen krijgen een grens (null = alle). Zo verraadt de kaart geen landen in de uitdaging. */
    borders: Set<String>? = null,
    onTap: ((Offset, MapTransform) -> Unit)? = null,
    /** De kaart beweegt (animatie): teken grover, zodat het soepel blijft. */
    moving: Boolean = false,
    overlay: @Composable BoxScope.(MapTransform) -> Unit = {},
) {
    // Volledig detail meteen; de grovere niveaus op de achtergrond.
    val full = remember(map) { buildLevel(map, 0f) }
    val coarse by produceState(emptyList<MapLevel>(), map) {
        value = withContext(Dispatchers.Default) { LevelTolerances.drop(1).map { buildLevel(map, it * map.width) } }
    }
    val rivers = remember(map) { map.rivers.values.map { it.toPath(closed = false) } }
    val grat = remember(map, graticule) { if (graticule) buildGraticule(map) else emptyList() }
    val paths = remember(full, coarse, rivers, grat) { MapPaths(listOf(full) + coarse, rivers, grat) }
    // Samengevoegde paden per niveau, opnieuw bij een andere selectie.
    val merged = remember(paths, glow, borders) { arrayOfNulls<Merged>(paths.levels.size) }
    val hi = LocalHighContrast.current
    val landCol = if (hi) Night.landHi else Night.land
    val hoverCol = if (hi) Night.landHoverHi else Night.landHover
    val edgeCol = if (hi) Night.landEdgeHi else Night.landEdge
    val decorCol = if (hi) Night.landDecorHi else Night.landDecor
    // Op de Dagkaart iets dunnere grenzen: donker op licht valt sneller op.
    val borderDp = if (Night.isLight) (if (hi) 1.4.dp else 1.1.dp) else (if (hi) 2.dp else 1.5.dp)
    val tap by rememberUpdatedState(onTap)

    BoxWithConstraints(modifier.clipToBounds()) {
        val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val density = LocalDensity.current
        val pt = with(density) { padTop.toPx() }
        val pb = with(density) { padBottom.toPx() }
        val tf = remember(size, viewport, pt, pb) { MapTransform.fit(size, viewport, pt, pb) }
        val currentTf by rememberUpdatedState(tf)
        Canvas(
            Modifier
                .fillMaxSize()
                .then(if (onTap != null) Modifier.pointerInput(Unit) { detectTapGestures { tap?.invoke(currentTf.toMap(it), currentTf) } } else Modifier),
        ) {
            val s = tf.scale
            // Kies het grofste niveau dat op het scherm nog niet opvalt (minder dan ~½ pixel, of 1 pixel tijdens bewegen).
            val maxDev = (if (moving) 1.1f else 0.45f) / s
            var li = 0
            for (i in paths.levels.indices) if (paths.levels[i].tol <= maxDev) li = i
            val level = paths.levels[li]
            val m = merged[li] ?: merge(level, glow, borders).also { merged[li] = it }
            // Grenzen en kusten: ongeveer 1,2 dp, ongeacht de zoom
            val borderPx = borderDp.toPx()
            withTransform({ translate(tf.dx, tf.dy); scale(s, s, pivot = Offset.Zero) }) {
                paths.graticule.forEach { drawPath(it, Night.graticule, style = Stroke(1f / s)) }
                // Zonder grenzen ook geen kleurverschil met omliggend land, anders zie je toch de vorm.
                drawPath(level.background, if (borders != null) landCol else decorCol)
                if (borders == null) drawPath(level.background, edgeCol.copy(alpha = 0.5f), style = Stroke(borderPx * 0.8f / s, join = StrokeJoin.Round))
                drawPath(m.fill, landCol)
                for (id in glow) level.countries[id]?.let { drawPath(it, hoverCol) }
                drawPath(level.lakes, Night.water)
                paths.rivers.forEach { drawPath(it, Night.river, style = Stroke(0.9f / s)) }
                drawPath(m.edge, edgeCol, style = Stroke(borderPx / s, join = StrokeJoin.Round))
                // Landen die te klein zijn om te zien (eilandstaten) krijgen een stip.
                val minPx = 5.dp.toPx()
                for ((id, shape) in map.countries) {
                    if (borders != null && id !in borders) continue // in de uitdaging niets verklappen
                    val b = shape.bounds
                    if (maxOf(b.width, b.height) * s < minPx) {
                        val c = map.labels[id] ?: b.center
                        val col = if (id in glow) glowColor else edgeCol
                        drawCircle(Night.skyDeep, radius = 4.dp.toPx() / s, center = c)
                        drawCircle(col, radius = 2.6.dp.toPx() / s, center = c)
                    }
                }
                // Gloed: een brede, doorzichtige lijn onder een scherpe rand.
                for (id in glow) {
                    val p = level.countries[id] ?: continue
                    drawPath(p, glowColor.copy(alpha = 0.08f))
                    drawPath(p, glowColor.copy(alpha = 0.16f), style = Stroke(7f / s, join = StrokeJoin.Round))
                    drawPath(p, glowColor.copy(alpha = 0.9f), style = Stroke(1.3f / s, join = StrokeJoin.Round))
                }
            }
        }
        if (pulseAt != null) PulseLayer(tf.toScreen(pulseAt), glowColor)
        Box(Modifier.fillMaxSize()) { overlay(tf) }
    }
}

@Composable
private fun PulseLayer(center: Offset, color: Color) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "p",
    )
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(color.copy(alpha = (1f - pulse) * 0.8f), radius = 6.dp.toPx() + 16.dp.toPx() * pulse, center = center, style = Stroke(2.dp.toPx()))
        drawCircle(Night.skyDeep, radius = 5.dp.toPx(), center = center)
        drawCircle(color, radius = 3.5.dp.toPx(), center = center)
    }
}

/** Plaatst een element op een schermpunt; ax/ay bepalen welk deel van het element op het punt ligt. */
fun Modifier.anchorAt(point: Offset, ax: Float = 0.5f, ay: Float = 1f): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(p.width, p.height) {
        p.place((point.x - p.width * ax).roundToInt(), (point.y - p.height * ay).roundToInt())
    }
}
