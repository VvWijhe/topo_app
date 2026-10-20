package nl.topo.app.data.geo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Een vorm op de kaart: een of meer ringen met x,y-paren in kaart-eenheden. */
class GeoShape(val rings: List<FloatArray>) {
    val bounds: Rect by lazy {
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        for (ring in rings) {
            var i = 0
            while (i < ring.size) {
                val x = ring[i]; val y = ring[i + 1]
                if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y
                i += 2
            }
        }
        Rect(l, t, r, b)
    }

    fun toPath(closed: Boolean = true): Path = Path().apply {
        fillType = PathFillType.EvenOdd
        for (ring in rings) {
            if (ring.size < 4) continue
            moveTo(ring[0], ring[1])
            var i = 2
            while (i < ring.size) { lineTo(ring[i], ring[i + 1]); i += 2 }
            if (closed) close()
        }
    }

    /** Kortste afstand van [p] tot de lijnen van deze vorm (voor rivieren). */
    fun distanceTo(p: Offset): Float {
        var best = Float.MAX_VALUE
        for (ring in rings) {
            var i = 2
            while (i < ring.size) {
                val ax = ring[i - 2]; val ay = ring[i - 1]; val bx = ring[i]; val by = ring[i + 1]
                val dx = bx - ax; val dy = by - ay
                val len2 = dx * dx + dy * dy
                val t = if (len2 == 0f) 0f else (((p.x - ax) * dx + (p.y - ay) * dy) / len2).coerceIn(0f, 1f)
                val ex = ax + t * dx - p.x; val ey = ay + t * dy - p.y
                val d = kotlin.math.sqrt(ex * ex + ey * ey)
                if (d < best) best = d
                i += 2
            }
        }
        return best
    }

    /** Alleen de delen binnen [area] (bijv. Frankrijk zonder Frans-Guyana). */
    fun within(area: Rect): GeoShape = GeoShape(rings.filter { r ->
        var cx = 0f; var cy = 0f; val n = r.size / 2
        var i = 0
        while (i < r.size) { cx += r[i]; cy += r[i + 1]; i += 2 }
        area.contains(Offset(cx / n, cy / n))
    }.ifEmpty { rings })

    /** Even-odd test: ligt het punt binnen deze vorm? */
    fun contains(p: Offset): Boolean {
        if (!bounds.contains(p)) return false
        var inside = false
        for (ring in rings) {
            val n = ring.size / 2
            var j = n - 1
            for (i in 0 until n) {
                val xi = ring[2 * i]; val yi = ring[2 * i + 1]
                val xj = ring[2 * j]; val yj = ring[2 * j + 1]
                if ((yi > p.y) != (yj > p.y) && p.x < (xj - xi) * (p.y - yi) / (yj - yi) + xi) inside = !inside
                j = i
            }
        }
        return inside
    }
}

/** Zet breedte-/lengtegraad om naar kaart-eenheden. */
interface MapProjection {
    fun project(lat: Double, lon: Double): Offset
}

/**
 * Natural Earth I-projectie (Šavrič e.a.): de wereldkaart van de app.
 * Een compromis tussen vorm en oppervlakte, zoals in moderne atlassen.
 */
class NaturalEarthProjection(private val k: Double, private val ox: Double, private val oy: Double) : MapProjection {
    override fun project(lat: Double, lon: Double): Offset {
        val lam = Math.toRadians(lon)
        val phi = Math.toRadians(lat)
        val p2 = phi * phi
        val p4 = p2 * p2
        val x = lam * (0.8707 - 0.131979 * p2 + p4 * (-0.013791 + p4 * (0.003971 * p2 - 0.001529 * p4)))
        val y = phi * (1.007226 + p2 * (0.015085 + p4 * (-0.044475 + 0.028874 * p2 - 0.005916 * p4)))
        return Offset((ox + x * k).toFloat(), (oy - y * k).toFloat())
    }
}

/** Eenvoudige projectie van de Nederland-kaart (lengtegraden geschaald met cos 52,2°). */
class NederlandProjection(private val x0: Double, private val y0: Double, private val s: Double, private val pad: Double) : MapProjection {
    private val k = Math.cos(Math.toRadians(52.2))
    override fun project(lat: Double, lon: Double): Offset =
        Offset((pad + (lon - x0) * k * s).toFloat(), (pad + (y0 - lat) * s).toFloat())
}

/**
 * Lambert-azimutale projectie (gelijkvlakkig), zoals Europese atlassen.
 * Zet lengte-/breedtegraad om naar kaart-eenheden en terug.
 */
class Projection(
    lon0: Double, lat0: Double,
    private val unitKm: Double,
    private val ox: Double, private val oy: Double,
) : MapProjection {
    private val l0 = Math.toRadians(lon0)
    private val p0 = Math.toRadians(lat0)
    private val r = 6371.0

    override fun project(lat: Double, lon: Double): Offset {
        val l = Math.toRadians(lon) - l0
        val p = Math.toRadians(lat)
        val k = sqrt(2 / (1 + sin(p0) * sin(p) + cos(p0) * cos(p) * cos(l)))
        val x = r * k * cos(p) * sin(l) / unitKm + ox
        val y = -r * k * (cos(p0) * sin(p) - sin(p0) * cos(p) * cos(l)) / unitKm + oy
        return Offset(x.toFloat(), y.toFloat())
    }

    /** Geeft (breedte, lengte) in graden terug. */
    fun unproject(pt: Offset): Pair<Double, Double> {
        val x = (pt.x - ox) * unitKm
        val y = -(pt.y - oy) * unitKm
        val rho = hypot(x, y)
        if (rho < 1e-6) return Math.toDegrees(p0) to Math.toDegrees(l0)
        val c = 2 * asin(min(1.0, rho / (2 * r)))
        val lat = asin(cos(c) * sin(p0) + y * sin(c) * cos(p0) / rho)
        val lon = l0 + atan2(x * sin(c), rho * cos(p0) * cos(c) - y * sin(p0) * sin(c))
        return Math.toDegrees(lat) to Math.toDegrees(lon)
    }
}

class GeoMap(
    val width: Float,
    val height: Float,
    /** Landen waar de quiz over gaat, op ISO-code (NLD, DEU, …). */
    val countries: Map<String, GeoShape>,
    /** Omliggend land (Noord-Afrika, Midden-Oosten …), alleen decor. */
    val background: List<GeoShape>,
    val lakes: List<GeoShape>,
    val rivers: Map<String, GeoShape>,
    /** Goede plek voor de naam van een land. */
    val labels: Map<String, Offset>,
    val projection: MapProjection,
) {
    fun countryAt(p: Offset): String? = countries.entries.firstOrNull { it.value.contains(p) }?.key
}

/** Leest padteksten als "M1,2L3,4Z M…" in. */
internal fun parsePath(d: String): GeoShape {
    val rings = ArrayList<FloatArray>()
    for (part in d.split('M')) {
        if (part.isBlank()) continue
        val pts = part.replace("Z", "").split('L')
        val arr = FloatArray(pts.size * 2)
        var n = 0
        for (pt in pts) {
            val c = pt.indexOf(',')
            if (c <= 0) continue
            arr[n++] = pt.substring(0, c).toFloat()
            arr[n++] = pt.substring(c + 1).toFloat()
        }
        if (n >= 4) rings.add(if (n == arr.size) arr else arr.copyOf(n))
    }
    return GeoShape(rings)
}
