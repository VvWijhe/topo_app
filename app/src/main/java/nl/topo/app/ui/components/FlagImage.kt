package nl.topo.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.topo.app.data.geo.Country
import nl.topo.app.ui.theme.Night

/** Vlaggen worden één keer ingelezen en daarna uit het geheugen gehaald. */
private object FlagCache {
    private val cache = LruCache<String, ImageBitmap>(80)
    fun peek(path: String): ImageBitmap? = cache.get(path)
    suspend fun load(context: Context, path: String): ImageBitmap? = cache.get(path) ?: withContext(Dispatchers.IO) {
        runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()
    }?.also { cache.put(path, it) }
}

/** Ratio breedte/hoogte als de afbeelding nog niet geladen is. */
private const val DEFAULT_RATIO = 1.5f

/**
 * Een echte vlag in zijn eigen verhoudingen (Zwitserland vierkant, Qatar lang),
 * met een dun randje zodat witte vlaggen zich aftekenen tegen de nacht.
 *
 * Geef [height] (de breedte volgt de vlag), of [maxWidth] en [maxHeight] om binnen een vak te passen.
 */
@Composable
fun FlagImage(
    country: Country,
    modifier: Modifier = Modifier,
    height: Dp = 24.dp,
    maxWidth: Dp? = null,
    shadow: Boolean = false,
) {
    val context = LocalContext.current
    val bmp by produceState(FlagCache.peek(country.flagAsset), country.flagAsset) {
        if (value == null) value = FlagCache.load(context, country.flagAsset)
    }
    val ratio = bmp?.let { it.width.toFloat() / it.height } ?: DEFAULT_RATIO
    // Lange vlaggen niet breder dan maxWidth: dan wordt de hoogte kleiner.
    val h = if (maxWidth != null && height * ratio > maxWidth) maxWidth / ratio else height
    val shape = RoundedCornerShape(if (h > 40.dp) 4.dp else 2.dp)
    val base = modifier
        .size(width = h * ratio, height = h)
        .then(if (shadow) Modifier.shadow(6.dp, shape) else Modifier)
        .clip(shape)
    val image = bmp
    if (image == null) {
        Box(base.background(Night.surfaceHigh))
    } else {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = base.border(1.dp, Night.text.copy(alpha = 0.12f), shape),
        )
    }
}

