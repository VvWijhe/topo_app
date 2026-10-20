package nl.topo.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.topo.app.data.geo.GeoMap
import nl.topo.app.ui.theme.Night
import kotlin.math.min
import kotlin.math.roundToInt

/** Rekent kaart-eenheden om naar schermpixels en terug. */
class MapTransform(val scale: Float, val dx: Float, val dy: Float) {
    fun toScreen(p: Offset) = Offset(p.x * scale + dx, p.y * scale + dy)
    fun toMap(s: Offset) = Offset((s.x - dx) / scale, (s.y - dy) / scale)

    companion object {
        /** Past het kader in de beschikbare ruimte (alles zichtbaar, gecentreerd). */
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

private class MapPaths(val countries: Map<String, Path>, val background: List<Path>, val lakes: List<Path>, val rivers: List<Path>, val graticule: List<Path>)

private fun buildPaths(map: GeoMap): MapPaths {
    val grat = buildList {
        for (lon in -30..60 step 10) add(Path().apply { for (lat in 30..80) { val p = map.projection.project(lat.toDouble(), lon.toDouble()); if (lat == 30) moveTo(p.x, p.y) else lineTo(p.x, p.y) } })
        for (lat in 30..80 step 10) add(Path().apply { for (lon in -40..70) { val p = map.projection.project(lat.toDouble(), lon.toDouble()); if (lon == -40) moveTo(p.x, p.y) else lineTo(p.x, p.y) } })
    }
    return MapPaths(
        countries = map.countries.mapValues { it.value.toPath() },
        background = map.background.map { it.toPath() },
        lakes = map.lakes.map { it.toPath() },
        rivers = map.rivers.values.map { it.toPath(closed = false) },
        graticule = grat,
    )
}

/**
 * De kaart in Nachtkaart-stijl.
 * @param glow landen die oplichten (bijv. de gekozen regio)
 * @param glowColor kleur van de gloed
 * @param pulseAt kaartpunt waar een kloppende ring op staat
 * @param onTap tik op de kaart, in kaart-eenheden
 * @param overlay composables bovenop de kaart; gebruik [anchorAt] om ze op een kaartpunt te zetten
 */
@Composable
fun NightMap(
    map: GeoMap,
    viewport: Rect,
    modifier: Modifier = Modifier,
    /** Ruimte aan boven- en onderkant waar iets over de kaart zweeft; het kader past ertussen. */
    padTop: Dp = 0.dp,
    padBottom: Dp = 0.dp,
    glow: Set<String> = emptySet(),
    glowColor: Color = Night.glow,
    pulseAt: Offset? = null,
    onTap: ((Offset) -> Unit)? = null,
    overlay: @Composable BoxScope.(MapTransform) -> Unit = {},
) {
    val paths = remember(map) { buildPaths(map) }
    val tap by rememberUpdatedState(onTap)
    val anim = rememberInfiniteTransition(label = "map")
    val breathe by anim.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val pulse by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "pulse")

    BoxWithConstraints(modifier.clipToBounds()) {
        val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val density = LocalDensity.current
        val pt = with(density) { padTop.toPx() }
        val pb = with(density) { padBottom.toPx() }
        val tf = remember(size, viewport, pt, pb) { MapTransform.fit(size, viewport, pt, pb) }
        Canvas(
            Modifier
                .fillMaxSize()
                .then(if (onTap != null) Modifier.pointerInput(tf) { detectTapGestures { tap?.invoke(tf.toMap(it)) } } else Modifier),
        ) {
            val s = tf.scale
            withTransform({ translate(tf.dx, tf.dy); scale(s, s, pivot = Offset.Zero) }) {
                paths.graticule.forEach { drawPath(it, Night.graticule, style = Stroke(1f / s)) }
                paths.background.forEach { drawPath(it, Night.landDecor) }
                for ((id, p) in paths.countries) {
                    drawPath(p, if (id in glow) Night.landHover else Night.land)
                }
                paths.lakes.forEach { drawPath(it, Night.water) }
                paths.rivers.forEach { drawPath(it, Night.river, style = Stroke(0.9f / s)) }
                for ((id, p) in paths.countries) {
                    if (id !in glow) drawPath(p, Night.landEdge, style = Stroke(0.8f / s, join = StrokeJoin.Round))
                }
                // Gloed: een paar brede, doorzichtige lijnen onder een scherpe rand.
                for (id in glow) {
                    val p = paths.countries[id] ?: continue
                    glowPath(p, glowColor, s, breathe)
                }
                pulseAt?.let { c ->
                    drawCircle(glowColor.copy(alpha = (1f - pulse) * 0.8f), radius = (6f + 22f * pulse) / s, center = c, style = Stroke(2f / s))
                    drawCircle(glowColor, radius = 4f / s, center = c)
                }
            }
        }
        Box(Modifier.fillMaxSize()) { overlay(tf) }
    }
}

private fun DrawScope.glowPath(p: Path, color: Color, s: Float, breathe: Float) {
    drawPath(p, color.copy(alpha = 0.10f * breathe))
    drawPath(p, color.copy(alpha = 0.07f * breathe), style = Stroke(12f / s, join = StrokeJoin.Round))
    drawPath(p, color.copy(alpha = 0.14f * breathe), style = Stroke(6f / s, join = StrokeJoin.Round))
    drawPath(p, color.copy(alpha = 0.9f), style = Stroke(1.4f / s, join = StrokeJoin.Round))
}

/** Plaatst een element op een schermpunt; ax/ay bepalen welk deel van het element op het punt ligt. */
fun Modifier.anchorAt(point: Offset, ax: Float = 0.5f, ay: Float = 1f): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(p.width, p.height) {
        p.place((point.x - p.width * ax).roundToInt(), (point.y - p.height * ay).roundToInt())
    }
}
