package nl.topo.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.topo.app.data.content.Content
import nl.topo.app.data.content.Levels
import nl.topo.app.data.content.Region
import nl.topo.app.data.content.RegionId
import nl.topo.app.data.prefs.Progress
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.ProgressBar
import nl.topo.app.ui.components.ProgressRing
import nl.topo.app.ui.components.StarsText
import nl.topo.app.ui.theme.Night

@Composable
fun ProgressScreen(onOpenRegion: (RegionId) -> Unit, onGoToMap: () -> Unit) {
    val progress by LocalAppContainer.current.progress.progress.collectAsStateWithLifecycle(Progress())
    val regions = Content.regions.filter { it.available }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(tr("Progress", "Voortgang"), style = MaterialTheme.typography.displayMedium, color = Night.text)
                Text(tr("What you know, and what you don't yet", "Wat je al kent, en wat nog niet"), style = MaterialTheme.typography.bodyMedium, color = Night.muted)
            }
        }
        if (progress.rounds == 0) {
            item { EmptyState(onGoToMap) }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Icons.Rounded.LocalFireDepartment, Night.ember, "${progress.streak}", if (progress.streak == 1) tr("day in a row", "dag op rij") else tr("days in a row", "dagen op rij"), Modifier.weight(1f))
                    StatTile(Icons.Rounded.Replay, Night.text, "${progress.rounds}", tr("rounds", "rondes"), Modifier.weight(1f))
                    StatTile(Icons.Rounded.CheckCircle, Night.glow, "${regions.sumOf { progress.mastered(it) }}", tr("cells mastered", "vakjes gekend"), Modifier.weight(1f))
                }
            }
        }
        items(regions, key = { it.id }) { r -> RegionProgress(r, progress) { onOpenRegion(r.id) } }
    }
}

@Composable
private fun StatTile(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier) {
    NightCard(modifier, padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.headlineLarge, color = Night.text)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Night.muted)
    }
}

@Composable
private fun RegionProgress(region: Region, progress: Progress, onOpen: () -> Unit) {
    NightCard(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress.fraction(region), diameter = 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(region.name.text, style = MaterialTheme.typography.headlineSmall, color = Night.text)
                Text(
                    tr("${progress.mastered(region)} of ${region.cellCount} mastered · ${progress.played(region)} played", "${progress.mastered(region)} van ${region.cellCount} gekend · ${progress.played(region)} gespeeld"),
                    style = MaterialTheme.typography.bodySmall, color = Night.muted,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Night.muted)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Levels.all.forEach { lv ->
                val done = progress.masteredAtLevel(region, lv.stars)
                val total = region.topics.size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(54.dp)) { StarsText(lv.stars, size = 11) }
                    ProgressBar(done.toFloat() / total, Modifier.weight(1f))
                    Text(
                        "$done/$total", style = MaterialTheme.typography.labelMedium, color = Night.muted,
                        textAlign = TextAlign.End, modifier = Modifier.width(44.dp),
                    )
                }
            }
        }
        // zwakste plekken: gespeeld maar nog niet gekend
        val weak = region.topics.flatMap { t -> (1..4).mapNotNull { lv -> progress.best(region.id, t.id, lv)?.takeIf { it < Levels.MASTERED }?.let { Triple(t, lv, it) } } }
            .sortedBy { it.third }.take(3)
        if (weak.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Eyebrow(tr("Keep practising", "Nog oefenen"))
            Spacer(Modifier.height(6.dp))
            weak.forEach { (t, lv, score) ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t.name.text, style = MaterialTheme.typography.bodyMedium, color = Night.text, modifier = Modifier.weight(1f))
                    StarsText(lv, size = 10)
                    Spacer(Modifier.width(10.dp))
                    Text("$score%", style = MaterialTheme.typography.labelMedium, color = if (score >= Levels.OK) Night.mid else Night.danger)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onGoToMap: () -> Unit) {
    NightCard(Modifier.fillMaxWidth(), padding = PaddingValues(24.dp)) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(Night.glow.copy(alpha = 0.12f)).border(1.dp, Night.glow.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Explore, null, tint = Night.glow, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(tr("No rounds played yet", "Nog geen rondes gespeeld"), style = MaterialTheme.typography.headlineSmall, color = Night.text)
        Text(
            tr("Pick an area on the map and play a round. Then you'll see your streak, your scores per level and what to practise here.", "Kies op de kaart een gebied en speel een ronde. Hier zie je daarna je reeks, je scores per niveau en wat je nog moet oefenen."),
            style = MaterialTheme.typography.bodyMedium, color = Night.muted,
        )
        Spacer(Modifier.height(16.dp))
        GlowButton(tr("Go to the map", "Naar de kaart"), onGoToMap)
    }
}
