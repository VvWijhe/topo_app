package nl.topo.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.topo.app.data.content.Content
import nl.topo.app.data.content.Region
import nl.topo.app.data.content.RegionId
import nl.topo.app.data.content.RegionKind
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.Lang
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.ui.components.FlagImage
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
import nl.topo.app.ui.components.PointsChip
import nl.topo.app.ui.components.anchorAt
import nl.topo.app.ui.components.mapZoomGestures
import nl.topo.app.ui.components.rememberMapZoom
import nl.topo.app.ui.theme.Night
import java.text.NumberFormat

@Composable
fun HomeScreen(
    onOpenRegion: (RegionId) -> Unit,
    onPlay: (RegionId, String, Int) -> Unit,
) {
    val app = LocalAppContainer.current
    val progress by app.progress.progress.collectAsStateWithLifecycle(Progress())
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val world by produceState<WorldMap?>(null) { value = app.geo.world() }
    val haptics = LocalHapticFeedback.current
    val tapReachPx = with(LocalDensity.current) { 22.dp.toPx() }
    val lang = LocalLang.current

    // Wat er gekozen is: een gebied (wereld of werelddeel) en eventueel een land daarin.
    var regionId by rememberSaveable { mutableStateOf(RegionId.EUROPA) }
    var countryCode by rememberSaveable { mutableStateOf<String?>(null) }
    var initialised by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(progress.lastPlayed) {
        val last = progress.lastPlayed
        if (!initialised && last != null) {
            val r = Content.region(last.region)
            if (r.kind == RegionKind.COUNTRY) { regionId = RegionId.EUROPA; countryCode = r.countryCode } else regionId = r.id
            initialised = true
        }
    }
    val region = Content.region(regionId)
    fun tick() { if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    fun selectRegion(id: RegionId) { if (id != regionId || countryCode != null) tick(); regionId = id; countryCode = null }
    fun selectCountry(c: Country) {
        if (c.code != countryCode) tick()
        // Ligt het land niet in het gekozen gebied, ga dan naar zijn eigen werelddeel.
        val cont = Content.region(regionId).continent
        if (cont == null || cont !in c.continents) {
            c.continents.firstOrNull()?.let { regionId = Content.forContinent(it).id }
        }
        countryCode = c.code
    }

    Column(Modifier.fillMaxSize()) {
        // ---------- kaart, met de kop er zwevend overheen ----------
        Box(Modifier.weight(1f).fillMaxWidth().heightIn(min = 300.dp).clipToBounds()) {
            val w = world
            if (w == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Night.glow)
            } else {
                val country = countryCode?.let { w.countries[it] }
                val target = remember(regionId, countryCode, w) {
                    when {
                        country != null -> padded(country.bounds)
                        region.continent != null -> w.continents[region.continent]?.viewport ?: w.worldViewport
                        else -> w.worldViewport
                    }
                }
                val viewport by animateRectAsState(
                    target,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
                    label = "zoom",
                )
                val glow = remember(regionId, countryCode, w) {
                    when {
                        country != null -> setOf(country.code)
                        region.continent != null -> w.countriesIn(region.continent).map { it.code }.toSet()
                        else -> emptySet()
                    }
                }
                // Knijp-zoom bovenop het gekozen gebied; terug naar normaal bij een andere keuze.
                val mz = rememberMapZoom(regionId, countryCode)
                NightMap(
                    map = w.geo,
                    viewport = mz.viewport(viewport),
                    modifier = Modifier.fillMaxSize().mapZoomGestures(mz) { viewport },
                    padTop = 112.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    padBottom = 16.dp,
                    glow = glow,
                    glowColor = if (country != null) Night.ember else Night.glow,
                    pulseAt = country?.capitalPos,
                    moving = viewport != target,
                    onTap = { p, tf ->
                        val reach = tapReachPx / tf.scale
                        val among = region.continent?.let { w.countriesIn(it) }
                        val hit = w.countryAt(p, reach, if (country == null) among else null)
                        when {
                            hit == null -> if (country != null) countryCode = null
                            region.kind == RegionKind.WORLD && country == null ->
                                hit.continents.firstOrNull()?.let { selectRegion(Content.forContinent(it).id) }
                            else -> selectCountry(hit)
                        }
                    },
                ) { tf ->
                    androidx.compose.runtime.SideEffect { mz.scale = tf.scale }
                    when {
                        country != null -> country.capitalPos?.let { cp ->
                            val s = tf.toScreen(cp)
                            country.capital(lang)?.let { MapTag(it, Night.ember, true, Modifier.anchorAt(s.copy(y = s.y - 18f), 0.5f, 1f)) {} }
                        }
                        region.kind == RegionKind.WORLD -> w.continents.values.forEach { ci ->
                            MapTag(ci.name(lang), Night.glow, false, Modifier.anchorAt(tf.toScreen(ci.center), 0.5f, 0.5f)) {
                                selectRegion(Content.forContinent(ci.continent).id)
                            }
                        }
                    }
                    // Terug-knop onder de kop
                    val backLabel = when {
                        country != null -> region.name.text
                        region.kind == RegionKind.CONTINENT -> Content.wereld.name.text
                        else -> null
                    }
                    if (backLabel != null) {
                        MapTag("← $backLabel", Night.glow, false, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 128.dp)) {
                            if (country != null) countryCode = null else selectRegion(RegionId.WERELD)
                        }
                    }
                }
                if (mz.isZoomed) {
                    IconButton(
                        onClick = { mz.reset() },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(12.dp)).background(Night.surface.copy(alpha = 0.9f)),
                    ) { Icon(Icons.Rounded.CenterFocusStrong, tr("Reset view", "Hele kaart"), tint = Night.text) }
                }
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(40.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Night.sky.copy(alpha = 0.9f)))),
                )
            }
            // ---------- kop ----------
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Night.skyHigh.copy(alpha = 0.92f), Night.sky.copy(alpha = 0.6f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Lodestar", style = MaterialTheme.typography.displayMedium, color = Night.text)
                    Text(
                        when {
                            countryCode != null -> tr("Tap a neighbour, or go back", "Tik een buurland aan, of ga terug")
                            region.kind == RegionKind.WORLD -> tr("Tap a continent", "Tik een werelddeel aan")
                            else -> tr("Tap a country on the map", "Tik een land aan op de kaart")
                        },
                        style = MaterialTheme.typography.bodyMedium, color = Night.muted,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PointsChip(progress.points)
                    StreakChip(progress.streak)
                }
            }
        }

        // ---------- keuzes onder de kaart ----------
        Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RegionChips(regionId, onSelect = ::selectRegion)
            val w = world
            val panelKey = countryCode ?: regionId.name
            AnimatedContent(panelKey, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "panel", modifier = Modifier.padding(horizontal = 16.dp)) { key ->
                val c = w?.countries?.get(key)
                if (c != null && w != null) {
                    CountryCard(
                        country = c, world = w,
                        onClose = { countryCode = null },
                        onNeighbour = { n -> w.countries[n]?.let(::selectCountry) },
                        onLearn = onOpenRegion,
                        onPick = { pickerOpen = true },
                    )
                } else {
                    RegionCard(
                        region = region, progress = progress,
                        countryCount = w?.countriesIn(region.continent)?.size,
                        onStart = { onOpenRegion(regionId) },
                        onPick = { pickerOpen = true },
                    )
                }
            }
            if (countryCode == null) progress.lastPlayed?.let { last ->
                Box(Modifier.padding(horizontal = 16.dp)) { ContinueRow(last) { onPlay(last.region, last.topicId, last.level) } }
            }
        }
    }

    val w = world
    if (pickerOpen && w != null) {
        CountryPicker(
            title = if (region.kind == RegionKind.WORLD) tr("Choose a country", "Kies een land") else tr("Choose a country in ${region.name.text}", "Kies een land in ${region.name.text}"),
            countries = w.countriesIn(region.continent),
            onDismiss = { pickerOpen = false },
            onPick = { c -> pickerOpen = false; selectCountry(c) },
        )
    }
}

/** Kader rond een land met wat ruimte eromheen; kleine landen niet absurd ver inzoomen. */
private fun padded(b: Rect): Rect {
    val minSize = 22f
    val w = maxOf(b.width, minSize)
    val h = maxOf(b.height, minSize)
    val pw = w * 0.75f
    val ph = h * 0.75f
    return Rect(b.center.x - pw, b.center.y - ph, b.center.x + pw, b.center.y + ph)
}

internal fun formatPopulation(n: Long, lang: Lang): String {
    val loc = lang.locale
    return when {
        n >= 1_000_000 -> NumberFormat.getNumberInstance(loc).apply { maximumFractionDigits = if (n >= 10_000_000) 0 else 1 }.format(n / 1_000_000.0) + lang.t(" million", " mln")
        n >= 10_000 -> NumberFormat.getIntegerInstance(loc).format((n / 1000) * 1000)
        else -> NumberFormat.getIntegerInstance(loc).format(n)
    }
}

internal fun formatArea(a: Double?, lang: Lang): String = when {
    a == null -> "–"
    a < 10 -> NumberFormat.getNumberInstance(lang.locale).apply { maximumFractionDigits = 2 }.format(a) + " km²"
    else -> NumberFormat.getIntegerInstance(lang.locale).format(a.toLong()) + " km²"
}

@Composable
internal fun MapTag(text: String, color: Color, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) color.copy(alpha = 0.22f) else Night.surface.copy(alpha = 0.85f))
            .border(1.dp, if (active) color else Night.outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (active) Night.text else Night.muted)
    }
}

@Composable
private fun RegionChips(selected: RegionId, onSelect: (RegionId) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Content.mapRegions.forEach { r ->
            val active = r.id == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Night.glow.copy(alpha = 0.16f) else Night.surface.copy(alpha = 0.85f))
                    .border(1.dp, if (active) Night.glow else Night.outline, RoundedCornerShape(50))
                    .clickable { onSelect(r.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(r.name.text, style = MaterialTheme.typography.labelMedium, color = if (active) Night.text else Night.muted)
            }
        }
    }
}

@Composable
private fun RegionCard(region: Region, progress: Progress, countryCount: Int?, onStart: () -> Unit, onPick: () -> Unit) {
    NightCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow(if (region.kind == RegionKind.WORLD) tr("The whole world", "De hele wereld") else tr("Continent", "Werelddeel"))
                Text(region.name.text, style = MaterialTheme.typography.headlineMedium, color = Night.text)
                Text(
                    listOfNotNull(countryCount?.let { tr("$it countries", "$it landen") }, region.tagline.text).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = Night.muted,
                )
            }
            ProgressRing(progress.fraction(region))
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton(tr("Choose topic", "Kies onderwerp"), onStart, Modifier.weight(1f))
            PickButton(onPick)
        }
    }
}

@Composable
private fun PickButton(onClick: () -> Unit, label: String = tr("Country", "Land"), modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(1.dp, Night.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.text),
    ) {
        Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CountryCard(
    country: Country,
    world: WorldMap,
    onClose: () -> Unit,
    onNeighbour: (String) -> Unit,
    onLearn: (RegionId) -> Unit,
    onPick: () -> Unit,
) {
    val lang = LocalLang.current
    NightCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagImage(country, height = 40.dp, maxWidth = 72.dp, shadow = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(
                    country.continents.mapNotNull { world.continents[it]?.name(lang) }.joinToString(tr(" and ", " en ")) +
                        if (!country.sovereign) tr(" · territory", " · gebied") else "",
                )
                Text(country.name(lang), style = MaterialTheme.typography.headlineMedium, color = Night.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, tr("Close", "Sluiten"), tint = Night.muted) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fact(tr("Capital", "Hoofdstad"), country.capital(lang) ?: "–", Modifier.weight(1.3f))
            Fact(tr("People", "Inwoners"), formatPopulation(country.population, lang), Modifier.weight(1f))
            Fact(tr("Area", "Oppervlakte"), formatArea(country.areaKm2, lang), Modifier.weight(1.1f))
        }
        val neighbours = country.neighbours.mapNotNull { world.countries[it] }
        if (neighbours.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Eyebrow(tr("Neighbours", "Buurlanden"))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                neighbours.forEach { n ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Night.surfaceHigh)
                            .border(1.dp, Night.outline, RoundedCornerShape(50))
                            .clickable { onNeighbour(n.code) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FlagImage(n, height = 14.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(n.name(lang), style = MaterialTheme.typography.labelMedium, color = Night.text)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        val own = Content.forCountry(country.code)
        if (own != null) {
            // Eigen onderwerpen: grote knop over de hele breedte, 'ander land' eronder.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton(tr("Learn ${country.name(lang)}", "Leer ${country.name(lang)}"), { onLearn(own.id) }, Modifier.fillMaxWidth())
                PickButton(onPick, tr("Other country", "Ander land"), Modifier.fillMaxWidth())
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            run {
                val home = country.continents.firstOrNull()?.let { Content.forContinent(it) }
                Column(Modifier.weight(1f)) {
                    Text(tr("Topics for this country are coming later.", "Onderwerpen voor dit land komen later."), style = MaterialTheme.typography.bodySmall, color = Night.muted)
                    if (home != null) {
                        Text(
                            tr("Practise ${home.name.text} →", "Oefen ${home.name.text} →"),
                            style = MaterialTheme.typography.labelLarge, color = Night.glow,
                            modifier = Modifier.clickable { onLearn(home.id) }.padding(top = 4.dp),
                        )
                    }
                }
            }
            PickButton(onPick, tr("Other country", "Ander land"))
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(Night.surfaceHigh.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Eyebrow(label)
        Text(value, style = MaterialTheme.typography.titleSmall, color = Night.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            Text(tr("Continue", "Verder spelen"), style = MaterialTheme.typography.labelSmall, color = Night.muted)
            Text("${Content.region(last.region).name.text} · ${topic.name.text}", style = MaterialTheme.typography.titleSmall, color = Night.text)
        }
        StarsText(last.level)
    }
}
