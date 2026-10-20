package nl.topo.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.topo.app.data.content.Content
import nl.topo.app.data.content.Levels
import nl.topo.app.data.content.Region
import nl.topo.app.data.content.RegionId
import nl.topo.app.data.geo.GeoMap
import nl.topo.app.data.prefs.LastPlayed
import nl.topo.app.data.prefs.Progress
import nl.topo.app.data.prefs.Settings
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.NightMap
import nl.topo.app.ui.components.ProgressRing
import nl.topo.app.ui.components.StarsText
import nl.topo.app.ui.components.StreakChip
import nl.topo.app.ui.components.anchorAt
import nl.topo.app.ui.theme.Night

/** Uitsnede van de Europakaart voor het startscherm (kaart-eenheden). */
private val HomeViewport = Rect(170f, 150f, 880f, 830f)

@Composable
fun HomeScreen(
    onOpenRegion: (RegionId) -> Unit,
    onPlay: (RegionId, String, Int) -> Unit,
) {
    val app = LocalAppContainer.current
    val progress by app.progress.progress.collectAsStateWithLifecycle(Progress())
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val map by produceState<GeoMap?>(null) { value = app.geo.europe() }
    val haptics = LocalHapticFeedback.current

    var selected by rememberSaveable { mutableStateOf(RegionId.EUROPA) }
    var initialised by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(progress.lastPlayed) {
        if (!initialised && progress.lastPlayed != null) { selected = progress.lastPlayed!!.region; initialised = true }
    }
    fun select(id: RegionId) {
        if (id != selected && settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selected = id
    }

    Column(Modifier.fillMaxSize()) {
        // ---------- kop ----------
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Topo", style = MaterialTheme.typography.displayMedium, color = Night.text)
                Text("Tik een gebied aan op de kaart", style = MaterialTheme.typography.bodyMedium, color = Night.muted)
            }
            StreakChip(progress.streak)
        }

        // ---------- kaart ----------
        // clipToBounds: de kaart mag niet over de kop of het paneel heen tekenen.
        Box(Modifier.weight(1f).fillMaxWidth().heightIn(min = 220.dp).clipToBounds()) {
            val m = map
            if (m == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Night.glow)
            } else {
                // Kader per regio: heel Europa, of ingezoomd op Nederland met wat buurland eromheen.
                val target = remember(selected, m) {
                    when (selected) {
                        RegionId.NEDERLAND -> m.countries["NLD"]?.bounds?.let { b ->
                            val padX = b.width * 0.55f; val padY = b.height * 0.35f
                            Rect(b.left - padX, b.top - padY, b.right + padX, b.bottom + padY)
                        } ?: HomeViewport
                        else -> HomeViewport
                    }
                }
                val viewport by animateRectAsState(
                    target,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
                    label = "zoom",
                )
                val zoomedIn = selected == RegionId.NEDERLAND
                val glow = remember(selected, m) {
                    when (selected) {
                        RegionId.NEDERLAND -> setOf("NLD")
                        RegionId.EUROPA -> m.countries.keys
                        RegionId.WERELD -> emptySet()
                    }
                }
                NightMap(
                    map = m,
                    viewport = viewport,
                    modifier = Modifier.fillMaxSize(),
                    glow = glow,
                    glowColor = if (selected == RegionId.NEDERLAND) Night.ember else Night.glow,
                    pulseAt = if (zoomedIn) null else m.labels["NLD"],
                    onTap = { p ->
                        when (m.countryAt(p)) {
                            null -> Unit
                            "NLD" -> select(RegionId.NEDERLAND)
                            else -> select(RegionId.EUROPA)
                        }
                    },
                ) { tf ->
                    // Labels op de kaart
                    m.labels["NLD"]?.let { p ->
                        val s = tf.toScreen(p)
                        MapTag(
                            "Nederland", Night.ember, zoomedIn,
                            if (zoomedIn) Modifier.anchorAt(s, 0.5f, 0.5f) else Modifier.anchorAt(s.copy(y = s.y - 14f), ax = 1.05f, ay = 1f),
                        ) { select(RegionId.NEDERLAND) }
                    }
                    if (zoomedIn) {
                        // Terug naar Europa: knop linksboven op de kaart
                        MapTag("← Europa", Night.glow, false, Modifier.align(Alignment.TopStart).padding(12.dp)) { select(RegionId.EUROPA) }
                    } else {
                        MapTag("Europa", Night.glow, true, Modifier.anchorAt(tf.toScreen(m.projection.project(50.2, 17.5)), 0.5f, 0.5f)) { select(RegionId.EUROPA) }
                    }
                }
                // zachte overgang naar het paneel eronder
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(40.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Night.sky.copy(alpha = 0.9f)))),
                )
            }
        }

        // ---------- regio-keuze ----------
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RegionChips(selected, ::select)
            AnimatedContent(selected, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "region") { id ->
                RegionCard(Content.region(id), progress) { onOpenRegion(id) }
            }
            progress.lastPlayed?.let { last -> ContinueRow(last) { onPlay(last.region, last.topicId, last.level) } }
        }
    }
}

@Composable
private fun MapTag(text: String, color: Color, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) color.copy(alpha = 0.22f) else Night.surface.copy(alpha = 0.8f))
            .border(1.dp, if (active) color else Night.outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (active) Night.text else Night.muted)
    }
}

@Composable
private fun RegionChips(selected: RegionId, onSelect: (RegionId) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Content.regions.forEach { r ->
            val active = r.id == selected
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Night.glow.copy(alpha = 0.16f) else Night.surface.copy(alpha = 0.85f))
                    .border(1.dp, if (active) Night.glow else Night.outline, RoundedCornerShape(50))
                    .clickable(enabled = r.available) { onSelect(r.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!r.available) Icon(Icons.Rounded.Lock, null, tint = Night.faint, modifier = Modifier.size(14.dp))
                Text(
                    r.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = when { active -> Night.text; r.available -> Night.muted; else -> Night.faint },
                )
            }
        }
    }
}

@Composable
private fun RegionCard(region: Region, progress: Progress, onStart: () -> Unit) {
    NightCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow(if (region.id == RegionId.NEDERLAND) "Thuisland" else "Werelddeel")
                Text(region.name, style = MaterialTheme.typography.headlineMedium, color = Night.text)
                Text(region.tagline, style = MaterialTheme.typography.bodyMedium, color = Night.muted)
            }
            ProgressRing(progress.fraction(region))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${region.topics.size} onderwerpen · 4 niveaus",
                    style = MaterialTheme.typography.labelMedium, color = Night.text,
                )
                Text(
                    "${progress.mastered(region)} van ${region.cellCount} vakjes gekend (≥ ${Levels.MASTERED}%)",
                    style = MaterialTheme.typography.bodySmall, color = Night.muted,
                )
            }
            GlowButton("Kies onderwerp", onStart)
        }
    }
}

@Composable
private fun ContinueRow(last: LastPlayed, onClick: () -> Unit) {
    val topic = Content.topic(last.region, last.topicId) ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Night.surface.copy(alpha = 0.7f))
            .border(1.dp, Night.outlineSoft, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Night.ember.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Night.ember, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Verder spelen", style = MaterialTheme.typography.labelSmall, color = Night.muted)
            Text("${Content.region(last.region).name} · ${topic.name}", style = MaterialTheme.typography.titleSmall, color = Night.text)
        }
        StarsText(last.level)
    }
}
