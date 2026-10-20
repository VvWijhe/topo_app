package nl.topo.app.data.geo

import android.content.res.AssetManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import nl.topo.app.data.i18n.Txt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ProjJson(val l0: Double, val p0: Double, val unit: Double, val ox: Double, val oy: Double)

@Serializable
private data class EuropeJson(
    val c: Map<String, String>,
    val bg: List<String>,
    val lbl: Map<String, List<Float>>,
    val lakes: List<String>,
    val riv: Map<String, String>,
    val vb: List<Float>,
    val proj: ProjJson,
)

@Serializable
private data class NeProjJson(val k: Double, val ox: Double, val oy: Double)

@Serializable
private data class CountryJson(
    val n: String,
    val ne: String = n,
    val a2: String,
    val cap: String? = null,
    val ce: String? = cap,
    val cll: List<Double>? = null,
    val pop: Long = 0,
    val area: Double? = null,
    val cont: List<String> = emptyList(),
    val nb: List<String> = emptyList(),
    val lbl: List<Float>,
    val b: List<Float>,
    val d: String,
    val sov: Boolean = true,
    val tld: String? = null,
)

@Serializable
private data class ContJson(val n: String, val ne: String = n, val v: List<Float>, val c: List<Float>)

@Serializable
private data class WorldJson(
    val vb: List<Float>,
    val proj: NeProjJson,
    val c: Map<String, CountryJson>,
    val bg: List<String>,
    val lakes: List<String>,
    val cont: Map<String, ContJson>,
    val riv: Map<String, String> = emptyMap(),
)

@Serializable
private data class NlProjJson(val x0: Double, val y0: Double, val s: Double, val pad: Double)

@Serializable
private data class NederlandJson(val vb: List<Float>, val prov: Map<String, String>, val isl: Map<String, String>, val proj: NlProjJson)

/** De kaart van Nederland: provincies als vlakken, plus de Waddeneilanden los. */
class NederlandMap(val geo: GeoMap, val islands: Map<String, GeoShape>)

/** Laadt kaartdata uit de app-assets en houdt ze in het geheugen. */
class GeoRepository(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var europe: GeoMap? = null
    private var world: WorldMap? = null
    private var nederland: NederlandMap? = null

    suspend fun nederland(): NederlandMap = mutex.withLock {
        nederland ?: withContext(Dispatchers.Default) {
            val text = assets.open("geo/nederland.json").bufferedReader().use { it.readText() }
            val raw = json.decodeFromString(NederlandJson.serializer(), text)
            val proj = NederlandProjection(raw.proj.x0, raw.proj.y0, raw.proj.s, raw.proj.pad)
            val provinces = raw.prov.mapValues { parsePath(it.value) }
            val geo = GeoMap(
                width = raw.vb[0], height = raw.vb[1],
                countries = provinces, background = emptyList(), lakes = emptyList(), rivers = emptyMap(),
                labels = provinces.mapValues { it.value.bounds.center },
                projection = proj,
            )
            NederlandMap(geo, raw.isl.mapValues { parsePath(it.value) })
        }.also { nederland = it }
    }

    suspend fun world(): WorldMap = mutex.withLock {
        world ?: withContext(Dispatchers.Default) {
            val text = assets.open("geo/world.json").bufferedReader().use { it.readText() }
            val raw = json.decodeFromString(WorldJson.serializer(), text)
            val proj = NaturalEarthProjection(raw.proj.k, raw.proj.ox, raw.proj.oy)
            val geo = GeoMap(
                width = raw.vb[0],
                height = raw.vb[1],
                countries = raw.c.mapValues { parsePath(it.value.d) },
                background = raw.bg.map(::parsePath),
                lakes = raw.lakes.map(::parsePath),
                rivers = emptyMap(),
                labels = raw.c.mapValues { Offset(it.value.lbl[0], it.value.lbl[1]) },
                projection = proj,
            )
            val countries = raw.c.mapValues { (code, c) ->
                Country(
                    code = code,
                    names = Txt(en = c.ne, nl = c.n),
                    iso2 = c.a2,
                    capitals = c.cap?.let { nl -> Txt(en = c.ce ?: nl, nl = nl) },
                    capitalPos = c.cll?.let { proj.project(it[0], it[1]) },
                    population = c.pop,
                    areaKm2 = c.area,
                    continents = c.cont.mapNotNull(Continent::of).sortedBy { primaryOrder(code, it) },
                    neighbours = c.nb,
                    label = Offset(c.lbl[0], c.lbl[1]),
                    bounds = Rect(c.b[0], c.b[1], c.b[2], c.b[3]),
                    sovereign = c.sov,
                )
            }
            val continents = raw.cont.mapNotNull { (k, v) ->
                Continent.of(k)?.let { it to ContinentInfo(it, Txt(en = v.ne, nl = v.n), Rect(v.v[0], v.v[1], v.v[2], v.v[3]), Offset(v.c[0], v.c[1])) }
            }.toMap()
            WorldMap(
                geo, countries, continents,
                rivers = raw.riv.mapValues { parsePath(it.value) },
                tld = raw.c.mapNotNull { (k, v) -> v.tld?.let { k to it } }.toMap(),
            )
        }.also { world = it }
    }

    /** Bij landen in twee werelddelen: welk telt als 'thuis'? Rusland → Europa, Turkije e.d. → Azië. */
    private fun primaryOrder(code: String, c: Continent): Int = when {
        code == "RUS" -> if (c == Continent.EU) 0 else 1
        code in setOf("TUR", "CYP", "GEO", "ARM", "AZE") -> if (c == Continent.AS) 0 else 1
        else -> c.ordinal
    }

    suspend fun europe(): GeoMap = mutex.withLock {
        europe ?: withContext(Dispatchers.Default) {
            val text = assets.open("geo/europe.json").bufferedReader().use { it.readText() }
            val raw = json.decodeFromString(EuropeJson.serializer(), text)
            GeoMap(
                width = raw.vb[0],
                height = raw.vb[1],
                countries = raw.c.mapValues { parsePath(it.value) },
                background = raw.bg.map(::parsePath),
                lakes = raw.lakes.map(::parsePath),
                rivers = raw.riv.mapValues { parsePath(it.value) },
                labels = raw.lbl.mapValues { Offset(it.value[0], it.value[1]) },
                projection = Projection(raw.proj.l0, raw.proj.p0, raw.proj.unit, raw.proj.ox, raw.proj.oy),
            )
        }.also { europe = it }
    }
}
