package nl.topo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import nl.topo.app.data.i18n.tr
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import nl.topo.app.data.geo.Continent
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.text
import nl.topo.app.data.prefs.MapStyle
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.theme.Night

/**
 * Rij met voorbeeldkaartjes van elke kaartstijl (in het huidige licht/donker); tik om te kiezen.
 * Het voorbeeld toont Europa met Afrika, met Europa als gekozen gebied, zodat je ook het dimmen ziet.
 */
@Composable
fun MapStylePicker(selected: MapStyle, onSelect: (MapStyle) -> Unit, modifier: Modifier = Modifier) {
    val app = LocalAppContainer.current
    val world by produceState<WorldMap?>(null) { value = app.geo.world() }
    val w = world
    val list = rememberLazyListState(initialFirstVisibleItemIndex = selected.ordinal.coerceAtMost(MapStyle.entries.size - 2).coerceAtLeast(0))
    Column(modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Iets meer dan twee kaartjes breed: het derde steekt uit, zodat je ziet dat er meer zijn.
            val cardW = (maxWidth - 20.dp) / 2.35f
            LazyRow(state = list, horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                items(MapStyle.entries, key = { it.name }) { style ->
                    val active = style == selected
                    Column(
                        Modifier.width(cardW).clip(MaterialTheme.shapes.medium)
                            .background(if (active) Night.glow.copy(alpha = 0.12f) else Night.surface.copy(alpha = 0.9f))
                            .border(if (active) 2.dp else 1.dp, if (active) Night.glow else Night.outline, MaterialTheme.shapes.medium)
                            .clickable { onSelect(style) }.padding(6.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(10.dp)).background(Night.sky), contentAlignment = Alignment.Center) {
                            if (w == null) CircularProgressIndicator(Modifier.size(20.dp), color = Night.glow, strokeWidth = 2.dp)
                            else {
                                val eu = w.continents[Continent.EU]?.viewport ?: w.worldViewport
                                val vp = Rect(eu.left - eu.width * 0.15f, eu.top, eu.right + eu.width * 0.15f, eu.bottom + eu.height * 0.35f)
                                val glow = androidx.compose.runtime.remember(w) { w.countriesIn(Continent.EU).map { it.code }.toSet() }
                                NightMap(map = w.geo, viewport = vp, modifier = Modifier.fillMaxWidth().height(90.dp), glow = glow, styleOverride = style, dots = false)
                            }
                            if (active) Icon(Icons.Rounded.CheckCircle, null, tint = Night.glow, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp))
                        }
                        Text(style.title.text, style = MaterialTheme.typography.titleSmall, color = Night.text, maxLines = 1, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                        Text(style.about.text, style = MaterialTheme.typography.bodySmall, color = Night.muted, maxLines = 2, minLines = 2, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
            // Rechts een vervaging met pijltje zolang er nog meer staat.
            if (list.canScrollForward) {
                Box(
                    Modifier.align(Alignment.CenterEnd).width(40.dp).height(150.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Night.surface.copy(alpha = 0.95f)))),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Night.text, modifier = Modifier.size(28.dp))
                }
            }
        }
        // Stipjes: hoeveel stijlen er zijn en waar je bent.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val first = list.firstVisibleItemIndex
            MapStyle.entries.forEachIndexed { i, st ->
                val visible = i in first..(first + 2)
                Box(
                    Modifier.padding(horizontal = 3.dp).size(if (st == selected) 8.dp else 6.dp).clip(RoundedCornerShape(50))
                        .background(when { st == selected -> Night.glow; visible -> Night.muted; else -> Night.outline }),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(tr("${MapStyle.entries.size} styles · swipe", "${MapStyle.entries.size} stijlen · veeg"), style = MaterialTheme.typography.labelSmall, color = Night.faint)
        }
    }
}
