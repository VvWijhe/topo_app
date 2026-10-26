package nl.topo.app.ui.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.mutableLongStateOf
import nl.topo.app.data.online.ScoreSubmission
import nl.topo.app.data.daily.Challenge
import nl.topo.app.data.daily.DailyChallenges
import nl.topo.app.data.daily.Difficulty
import nl.topo.app.data.daily.GlobeLine
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.prefs.RoundOutcome
import nl.topo.app.data.prefs.Settings
import nl.topo.app.data.prefs.DailyRules
import nl.topo.app.data.quiz.Matching
import nl.topo.app.data.quiz.Names
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.FlagImage
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.NightMap
import nl.topo.app.ui.components.PointsPanel
import nl.topo.app.ui.components.ProgressBar
import nl.topo.app.ui.components.anchorAt
import nl.topo.app.ui.components.mapZoomGestures
import nl.topo.app.ui.components.rememberMapZoom
import nl.topo.app.ui.theme.Night

private sealed interface Said {
    data class Found(val c: Country) : Said
    data class Already(val c: Country) : Said
    data class NotIn(val c: Country, val penalty: Int) : Said
    data class Unknown(val text: String) : Said
}

@Composable
fun DailyPlayScreen(difficulty: Difficulty, onBack: () -> Unit) {
    val app = LocalAppContainer.current
    val lang = LocalLang.current
    val world by produceState<WorldMap?>(null) { value = app.geo.world() }
    val w = world
    if (w == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Night.glow) }
        return
    }
    val day = remember { DailyChallenges.today() }
    val challenge = remember(w, day, lang) { DailyChallenges.forDay(w, day, difficulty, lang) }
    // Opgeslagen stand één keer inlezen, daarna lokaal bijhouden
    val stored by produceState<nl.topo.app.data.prefs.DailyState?>(null) {
        app.progress.daily(day, difficulty.name).collect { if (value == null) value = it }
    }
    val s = stored
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Night.glow) }
        return
    }
    ChallengePlay(w, challenge, s, onBack)
}

@Composable
private fun ChallengePlay(w: WorldMap, c: Challenge, stored: nl.topo.app.data.prefs.DailyState, onBack: () -> Unit) {
    val app = LocalAppContainer.current
    val lang = LocalLang.current
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    val feel = LocalHapticFeedback.current
    val hard = c.difficulty == Difficulty.HARD

    val found = remember { mutableStateListOf<String>().apply { addAll(stored.found.filter { it in c.targets }) } }
    var hints by remember { mutableIntStateOf(stored.hints) }
    var hinted by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(stored.finished) }
    var gaveUp by remember { mutableStateOf(stored.gaveUp) }
    var mistakes by remember { mutableIntStateOf(stored.mistakes) }
    var lost by remember { mutableStateOf(stored.lost) }
    var rankingVersion by remember { mutableIntStateOf(0) }
    val maxMistakes = DailyRules.maxMistakes(hard)
    // Speeltijd: telt alleen zolang de app op de voorgrond is en de uitdaging loopt
    var elapsed by remember { mutableLongStateOf(stored.timeMs) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(finished) {
        if (finished) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var sinceSave = 0
            while (true) {
                delay(1000); elapsed += 1000; sinceSave++
                if (sinceSave >= 5) { sinceSave = 0; app.progress.saveDaily(c.day, c.difficulty.name, found.toList(), hints, mistakes, elapsed) }
            }
        }
    }
    var outcome by remember { mutableStateOf<RoundOutcome?>(null) }
    var said by remember { mutableStateOf<Said?>(null) }
    var text by remember { mutableStateOf("") }
    val total = c.targets.size

    fun finish(giveUp: Boolean, outOfLives: Boolean = false) {
        if (finished) return
        finished = true; gaveUp = giveUp; lost = outOfLives
        scope.launch {
            app.progress.saveDaily(c.day, c.difficulty.name, found.toList(), hints, mistakes, elapsed)
            val o = app.progress.finishDaily(c.day, c.difficulty.name, hard, found.size, total, hints, giveUp, mistakes, outOfLives)
            outcome = o
            // Naar de ranglijst. In de app-scope, zodat het ook doorgaat als je het scherm verlaat.
            app.scope.launch { app.leaderboard.submit(
                ScoreSubmission(
                    playerId = app.profile.playerId(), nickname = app.profile.currentNickname(),
                    day = c.day, difficulty = c.difficulty.name, points = o.gained, found = found.size, total = total,
                    mistakes = mistakes, hints = hints, timeMs = elapsed, completed = found.size == total,
                    submittedAt = System.currentTimeMillis(),
                ),
            ); rankingVersion++ }
        }
    }

    fun submit() {
        val t = text.trim()
        if (t.isEmpty() || finished) return
        fun ok(country: Country) = Names.country(country).let { if (settings.strictTyping) Matching.matchesStrict(t, it) else Matching.matches(t, it) }
        val target = c.targets.mapNotNull { w.countries[it] }.firstOrNull { it.code !in found && ok(it) }
        said = when {
            target != null -> {
                found.add(target.code)
                if (hinted == target.code) hinted = null
                if (settings.haptics) feel.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch { app.progress.saveDaily(c.day, c.difficulty.name, found.toList(), hints, mistakes) }
                if (found.size == total) finish(false)
                Said.Found(target)
            }
            else -> {
                if (settings.haptics) feel.performHapticFeedback(HapticFeedbackType.LongPress)
                val other = w.all.firstOrNull { ok(it) }
                when {
                    other == null -> Said.Unknown(t) // onbekende naam (vaak een tikfout): geen straf
                    other.code in found -> Said.Already(other)
                    else -> {
                        // een echt land dat er niet bij hoort: fout, kost punten en een leven
                        mistakes++
                        scope.launch { app.progress.saveDaily(c.day, c.difficulty.name, found.toList(), hints, mistakes) }
                        if (mistakes >= maxMistakes) finish(giveUp = false, outOfLives = true)
                        Said.NotIn(other, DailyRules.penalty(hard))
                    }
                }
            }
        }
        text = ""
    }

    fun hint() {
        val left = c.targets.filter { it !in found }
        if (left.isEmpty() || hard || finished) return
        hinted = left.random()
        hints++
        scope.launch { app.progress.saveDaily(c.day, c.difficulty.name, found.toList(), hints, mistakes) }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        // ---------- kop ----------
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back", "Terug"), tint = Night.text) }
                Eyebrow(tr("Daily challenge", "Dagelijkse uitdaging") + " · " + if (hard) tr("Hard", "Moeilijk") else tr("Easy", "Makkelijk"),
                    color = if (hard) Night.ember else Night.glow)
            }
            Text(c.title.text, style = MaterialTheme.typography.headlineSmall, color = Night.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("${found.size} of $total found", "${found.size} van $total gevonden"), style = MaterialTheme.typography.labelMedium, color = Night.muted, modifier = Modifier.weight(1f))
                Text(formatTime(elapsed), style = MaterialTheme.typography.labelMedium, color = Night.faint); Spacer(Modifier.width(10.dp))
                if (hints > 0) { Text(tr("$hints hints", "$hints hints"), style = MaterialTheme.typography.labelMedium, color = Night.faint); Spacer(Modifier.width(10.dp)) }
                // levens: elke fout kost er één
                repeat(maxMistakes) { i ->
                    Icon(
                        if (i < maxMistakes - mistakes) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null,
                        tint = if (i < maxMistakes - mistakes) Night.danger else Night.faint, modifier = Modifier.size(18.dp),
                    )
                }
            }
            ProgressBar(found.size.toFloat() / total.coerceAtLeast(1), color = if (hard) Night.ember else Night.glow)
        }
        Spacer(Modifier.height(8.dp))

        // ---------- kaart ----------
        Box(Modifier.weight(1f).fillMaxWidth()) {
            ChallengeMap(w, c, found, hinted, finished)
        }

        // ---------- invoer en uitslag ----------
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp).then(if (finished) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (found.isNotEmpty() || finished) FoundRow(w, c, found, finished)
            if (!finished) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(tr("Type a country", "Typ een land"), color = Night.faint) },
                        singleLine = true, shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Night.glow, unfocusedBorderColor = Night.outline,
                            focusedContainerColor = Night.surfaceHigh, unfocusedContainerColor = Night.surfaceHigh,
                            cursorColor = Night.glow, focusedTextColor = Night.text, unfocusedTextColor = Night.text,
                        ),
                    )
                    GlowButton(tr("Add", "Toevoegen"), { submit() }, enabled = text.isNotBlank())
                }
                said?.let { Feedback(it) }
                hinted?.let { h -> w.countries[h]?.let { hc ->
                    val n = hc.name(lang)
                    Text(tr("Hint: the outlined country starts with “${n.take(1)}” (${n.length} letters).", "Hint: het omlijnde land begint met “${n.take(1)}” (${n.length} letters)."),
                        style = MaterialTheme.typography.bodySmall, color = Night.ember)
                } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hard) {
                        OutlinedButton(onClick = { hint() }, border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.text)) {
                            Icon(Icons.Rounded.Lightbulb, null, tint = Night.gold); Spacer(Modifier.width(6.dp)); Text(tr("Hint (−5)", "Hint (−5)"))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { finish(true) }, border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.muted)) {
                        Text(tr("Give up", "Opgeven"))
                    }
                }
            } else {
                val allFound = found.size == total
                Text(
                    when {
                        allFound -> tr("All $total found. Well done!", "Alle $total gevonden. Goed gedaan!")
                        lost -> tr("Out of lives: $maxMistakes wrong answers. You found ${found.size} of $total; the missing ones are in red.",
                            "Geen levens meer: $maxMistakes foute antwoorden. Je vond er ${found.size} van $total; de ontbrekende staan in het rood.")
                        else -> tr("You found ${found.size} of $total. The missing ones are shown in red.", "Je vond er ${found.size} van $total. De ontbrekende staan in het rood.")
                    },
                    style = MaterialTheme.typography.titleSmall, color = if (allFound) Night.glow else Night.text,
                )
                outcome?.let { PointsPanel(it) } ?: run {
                    if (stored.finished) Text(tr("Points earned today: +${stored.points}", "Punten vandaag verdiend: +${stored.points}"), style = MaterialTheme.typography.bodyMedium, color = Night.gold)
                }
                Text(tr("Time: ", "Tijd: ") + formatTime(elapsed), style = MaterialTheme.typography.bodySmall, color = Night.muted)
                RankingCard(day = c.day, difficulty = c.difficulty, refreshKey = rankingVersion, compact = true)
                Text(tr("Come back tomorrow for new challenges.", "Kom morgen terug voor nieuwe uitdagingen."), style = MaterialTheme.typography.bodySmall, color = Night.faint)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun Feedback(s: Said) {
    val lang = LocalLang.current
    val (msg, col) = when (s) {
        is Said.Found -> tr("✓ ${s.c.name(lang)}", "✓ ${s.c.name(lang)}") to Night.glow
        is Said.Already -> tr("You already have ${s.c.name(lang)}.", "${s.c.name(lang)} heb je al.") to Night.muted
        is Said.NotIn -> tr("${s.c.name(lang)} isn't part of this one. −${s.penalty} points", "${s.c.name(lang)} hoort er niet bij. −${s.penalty} punten") to Night.danger
        is Said.Unknown -> tr("“${s.text}” isn't a country we know — check the spelling (no penalty).", "“${s.text}” kennen we niet als land — check de spelling (geen straf).") to Night.muted
    }
    Text(msg, style = MaterialTheme.typography.labelMedium, color = col)
}

@Composable
private fun FoundRow(w: WorldMap, c: Challenge, found: List<String>, finished: Boolean) {
    val lang = LocalLang.current
    val missing = if (finished) c.targets.filter { it !in found } else emptyList()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (found.reversed().map { it to true } + missing.map { it to false }).forEach { (code, ok) ->
            val country = w.countries[code] ?: return@forEach
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Night.surfaceHigh)
                    .border(1.dp, if (ok) Night.glow.copy(alpha = 0.6f) else Night.danger.copy(alpha = 0.7f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlagImage(country, height = 14.dp)
                Spacer(Modifier.width(6.dp))
                Text(country.name(lang), style = MaterialTheme.typography.labelMedium, color = Night.text)
            }
        }
    }
}

/** Kaart zonder grenzen: alleen het gegeven land, de gevonden landen en (na afloop) de gemiste landen krijgen een vorm. */
@Composable
private fun ChallengeMap(w: WorldMap, c: Challenge, found: List<String>, hinted: String?, finished: Boolean) {
    val lang = LocalLang.current
    val mz = rememberMapZoom(c)
    val base = c.viewport
    val viewport = mz.viewport(base)
    val missing = if (finished) c.targets.filter { it !in found } else emptyList()
    val shown = buildSet { c.subject?.let { add(it) }; addAll(found); addAll(missing); hinted?.let { add(it) } }

    Box(
        Modifier.fillMaxSize().mapZoomGestures(mz) { base },
    ) {
        NightMap(map = w.geo, viewport = viewport, modifier = Modifier.fillMaxSize(), borders = shown) { tf ->
            androidx.compose.runtime.SideEffect { mz.scale = tf.scale }
            Canvas(Modifier.fillMaxSize()) {
                val s = tf.scale
                withTransform({ translate(tf.dx, tf.dy); scale(s, s, Offset.Zero) }) {
                    fun fill(code: String, col: Color, alpha: Float) {
                        val path = w.geo.countries[code]?.toPath() ?: return
                        drawPath(path, col.copy(alpha = alpha))
                        drawPath(path, col, style = Stroke(2f / s, join = StrokeJoin.Round))
                    }
                    c.subject?.let { fill(it, Night.ember, 0.35f) }
                    found.forEach { fill(it, Night.glow, 0.28f) }
                    missing.forEach { fill(it, Night.danger, 0.25f) }
                    hinted?.let { h ->
                        w.geo.countries[h]?.toPath()?.let { drawPath(it, Night.ember, style = Stroke(2.2f / s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f / s, 6f / s)))) }
                    }
                }
                // de lijn over de wereld
                when (val l = c.line) {
                    is GlobeLine.Latitude -> {
                        val y = tf.toScreen(w.geo.projection.project(l.lat, 0.0)).y
                        drawLine(Night.gold, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)))
                    }
                    is GlobeLine.Meridian -> {
                        val top = tf.toScreen(w.geo.projection.project(85.0, l.lon)); val bottom = tf.toScreen(w.geo.projection.project(-85.0, l.lon))
                        drawLine(Night.gold, top, bottom, strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)))
                    }
                    null -> Unit
                }
            }
            // namen van gevonden en gemiste landen
            (found + missing).forEach { code ->
                val country = w.countries[code] ?: return@forEach
                val p = tf.toScreen(country.label)
                Text(
                    country.name(lang), style = MaterialTheme.typography.labelSmall,
                    color = if (code in found) Night.text else Night.danger,
                    modifier = Modifier.anchorAt(p, 0.5f, 0.5f).clip(RoundedCornerShape(4.dp)).background(Night.skyDeep.copy(alpha = 0.6f)).padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
            c.subject?.let { sc -> w.countries[sc]?.let { country ->
                Text(country.name(lang), style = MaterialTheme.typography.labelMedium, color = Night.ember,
                    modifier = Modifier.anchorAt(tf.toScreen(country.label), 0.5f, 0.5f).clip(RoundedCornerShape(4.dp)).background(Night.skyDeep.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp))
            } }
            c.line?.let { l ->
                val at = when (l) {
                    // rechts op de lijn, uit de buurt van de landnamen
                    is GlobeLine.Latitude -> Offset(tf.toScreen(Offset(viewport.right, 0f)).x - 12f, tf.toScreen(w.geo.projection.project(l.lat, 0.0)).y)
                    is GlobeLine.Meridian -> tf.toScreen(w.geo.projection.project(0.0, l.lon)).let { Offset(it.x + 6f, tf.toScreen(Offset(0f, viewport.top)).y + 40f) }
                }
                Text(
                    l.name.text, style = MaterialTheme.typography.labelSmall, color = Night.gold,
                    modifier = Modifier.anchorAt(at, if (l is GlobeLine.Latitude) 1f else 0f, 1.3f)
                        .clip(RoundedCornerShape(4.dp)).background(Night.skyDeep.copy(alpha = 0.75f)).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        if (mz.isZoomed) {
            IconButton(
                onClick = { mz.reset() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(12.dp)).background(Night.surface.copy(alpha = 0.9f)),
            ) { Icon(Icons.Rounded.CenterFocusStrong, tr("Reset view", "Hele kaart"), tint = Night.text) }
        }
    }
}
