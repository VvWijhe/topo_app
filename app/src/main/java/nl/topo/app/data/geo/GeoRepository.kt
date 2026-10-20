package nl.topo.app.data.geo

import android.content.res.AssetManager
import androidx.compose.ui.geometry.Offset
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

/** Laadt kaartdata uit de app-assets en houdt ze in het geheugen. */
class GeoRepository(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var europe: GeoMap? = null

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
