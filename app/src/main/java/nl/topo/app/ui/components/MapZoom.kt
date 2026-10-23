package nl.topo.app.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Knijp-zoom en schuiven voor een kaart, bovenop een vast kader.
 * Er wordt ingezoomd rond het punt tussen je vingers.
 */
class MapZoom(private val maxZoom: Float = 60f) {
    var zoom by mutableFloatStateOf(1f); private set
    /** Midden van het beeld ten opzichte van het midden van het kader, in kaart-eenheden. */
    var panX by mutableFloatStateOf(0f); private set
    var panY by mutableFloatStateOf(0f); private set
    /** Schermpixels per kaart-eenheid, zoals laatst getekend (de kaart zet dit). */
    var scale by mutableFloatStateOf(1f)

    val isZoomed: Boolean get() = zoom > 1.02f || panX != 0f || panY != 0f

    fun viewport(base: Rect): Rect {
        val w = base.width / zoom
        val h = base.height / zoom
        val cx = base.center.x + panX
        val cy = base.center.y + panY
        return Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
    }

    fun reset() { zoom = 1f; panX = 0f; panY = 0f }

    /** Verwerk één stap van een gebaar. [centroid] en [pan] in schermpixels, [screen] = grootte van de kaart. */
    fun gesture(base: Rect, screenCenter: Offset, centroid: Offset, pan: Offset, zoomChange: Float) {
        val s = scale.coerceAtLeast(1e-4f)
        // kaartpunt onder de vingers
        val cx = base.center.x + panX
        val cy = base.center.y + panY
        val px = cx + (centroid.x - screenCenter.x) / s
        val py = cy + (centroid.y - screenCenter.y) / s
        val newZoom = (zoom * zoomChange).coerceIn(1f, maxZoom)
        val ns = s * newZoom / zoom
        // houd dat punt onder de vingers, en verschuif mee met de beweging
        var ncx = px - (centroid.x - screenCenter.x) / ns - pan.x / ns
        var ncy = py - (centroid.y - screenCenter.y) / ns - pan.y / ns
        ncx = ncx.coerceIn(base.left, base.right)
        ncy = ncy.coerceIn(base.top, base.bottom)
        zoom = newZoom
        scale = ns
        panX = if (newZoom <= 1.0001f) 0f else ncx - base.center.x
        panY = if (newZoom <= 1.0001f) 0f else ncy - base.center.y
    }
}

@Composable
fun rememberMapZoom(vararg keys: Any?): MapZoom = remember(*keys) { MapZoom() }

/** Koppelt knijp- en sleepgebaren aan [zoom]. */
fun Modifier.mapZoomGestures(zoom: MapZoom, base: () -> Rect): Modifier = pointerInput(zoom) {
    detectTransformGestures { centroid, pan, z, _ ->
        zoom.gesture(base(), Offset(size.width / 2f, size.height / 2f), centroid, pan, z)
    }
}
