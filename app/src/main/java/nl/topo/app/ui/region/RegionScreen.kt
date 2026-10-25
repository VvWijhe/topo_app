package nl.topo.app.ui.region

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.topo.app.data.content.Content
import nl.topo.app.data.content.Levels
import nl.topo.app.data.content.Region
import nl.topo.app.data.content.RegionId
import nl.topo.app.data.content.Topic
import nl.topo.app.data.content.TopicGroup
import nl.topo.app.data.content.RegionKind
import nl.topo.app.data.quiz.Levels4
import androidx.compose.runtime.produceState
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.prefs.Progress
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.ProgressRing
import nl.topo.app.ui.components.ScoreDot
import nl.topo.app.ui.components.StarsText
import nl.topo.app.ui.theme.Night

private val CellWidth = 42.dp

/** Optiescherm per regio: onderwerpen in rijen, niveaus ★–★★★★ in kolommen. */
@Composable
fun RegionScreen(regionId: RegionId, onBack: () -> Unit, onPlay: (topicId: String, level: Int) -> Unit) {
    val region = Content.region(regionId)
    val app = LocalAppContainer.current
    val progress by app.progress.progress.collectAsStateWithLifecycle(Progress())
    // Aantal landen in dit gebied, om per niveau te tonen hoeveel er meedoen.
    val countryCount by produceState<Int?>(null, region) {
        if (region.kind != RegionKind.COUNTRY) value = app.geo.world().countriesIn(region.continent).size
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header(region, progress, onBack) }
        item { LevelLegend(region, countryCount) }
        items(region.groups, key = { it.title.en }) { group -> TopicTable(region, group, progress, onPlay) }
        item {
            Text(
                tr("Tap a cell to play. A green tick means ${Levels.MASTERED}% or more.", "Tik op een vakje om te spelen. Een groen vinkje betekent ${Levels.MASTERED}% of meer."),
                style = MaterialTheme.typography.bodySmall, color = Night.faint,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun Header(region: Region, progress: Progress, onBack: () -> Unit) {
    Column(Modifier.padding(top = 4.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = tr("Back", "Terug"), tint = Night.text)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Eyebrow(tr("Options", "Opties"))
                Text(region.name.text, style = MaterialTheme.typography.displayMedium, color = Night.text)
                Text(region.tagline.text, style = MaterialTheme.typography.bodyMedium, color = Night.muted)
            }
            ProgressRing(progress.fraction(region), diameter = 64.dp)
        }
    }
}

@Composable
private fun LevelLegend(region: Region, countryCount: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Levels.all.forEachIndexed { i, lv ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(Night.surface.copy(alpha = 0.85f))
                    .border(1.dp, Night.outline, MaterialTheme.shapes.small)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                StarsText(lv.stars, size = 9)
                Text(lv.title.text, style = MaterialTheme.typography.labelMedium, color = Night.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val scope = countryCount?.let { n ->
                    val k = Levels4.size(n, lv.stars)
                    if (k == n) tr("all $n countries", "alle $n landen") else tr("$k of $n countries", "$k van $n landen")
                } ?: region.levelScope[i].text
                Text(scope, style = MaterialTheme.typography.bodySmall, color = Night.muted, maxLines = 2)
                Text(lv.how.text, style = MaterialTheme.typography.bodySmall, color = Night.faint, maxLines = 2)
            }
        }
    }
}

@Composable
private fun TopicTable(region: Region, group: TopicGroup, progress: Progress, onPlay: (String, Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Night.surface.copy(alpha = 0.9f))
            .border(1.dp, Night.outline, MaterialTheme.shapes.medium),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)) {
            Text(group.title.text, style = MaterialTheme.typography.headlineSmall, color = Night.text)
            Text(group.subtitle.text, style = MaterialTheme.typography.bodySmall, color = Night.muted)
        }
        // kolomkoppen
        Row(
            Modifier.fillMaxWidth().background(Night.surfaceHigh.copy(alpha = 0.6f)).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(tr("Topic", "Onderwerp"), Modifier.weight(1f).padding(start = 16.dp))
            Levels.all.forEach { lv ->
                Box(Modifier.width(CellWidth), contentAlignment = Alignment.Center) { StarsText(lv.stars, size = 8) }
            }
            Spacer(Modifier.width(6.dp))
        }
        group.topics.forEachIndexed { i, t ->
            if (i > 0) HorizontalDivider(color = Night.outlineSoft, thickness = 1.dp)
            TopicRow(region, t, progress, onPlay)
        }
    }
}

@Composable
private fun TopicRow(region: Region, topic: Topic, progress: Progress, onPlay: (String, Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Night.glow.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) { Icon(topic.icon, null, tint = Night.glow, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(topic.name.text, style = MaterialTheme.typography.titleSmall, color = Night.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(topic.description.text, style = MaterialTheme.typography.bodySmall, color = Night.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Levels.all.forEach { lv ->
            Box(
                Modifier.width(CellWidth).fillMaxHeight64().clickable { onPlay(topic.id, lv.stars) },
                contentAlignment = Alignment.Center,
            ) { ScoreDot(progress.best(region.id, topic.id, lv.stars), diameter = 30.dp) }
        }
        Spacer(Modifier.width(6.dp))
    }
}

private fun Modifier.fillMaxHeight64() = this.height(64.dp)

