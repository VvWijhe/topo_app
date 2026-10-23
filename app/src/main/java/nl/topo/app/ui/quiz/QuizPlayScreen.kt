package nl.topo.app.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import nl.topo.app.data.content.Levels
import nl.topo.app.data.content.Region
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.quiz.Feature
import nl.topo.app.data.prefs.Settings
import nl.topo.app.ui.LocalAppContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.topo.app.data.quiz.Mode
import nl.topo.app.data.quiz.Opt
import nl.topo.app.data.quiz.Prompt
import nl.topo.app.data.quiz.Question
import nl.topo.app.data.quiz.QuizGame
import nl.topo.app.data.quiz.QuizMap
import nl.topo.app.data.quiz.QuizSetup
import nl.topo.app.data.prefs.RoundOutcome
import nl.topo.app.ui.components.PointsPanel
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.FlagImage
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.NightMap
import nl.topo.app.ui.components.ProgressBar
import nl.topo.app.ui.components.StarsText
import nl.topo.app.ui.components.anchorAt
import nl.topo.app.ui.components.mapZoomGestures
import nl.topo.app.ui.components.rememberMapZoom
import nl.topo.app.ui.theme.Night

@Composable
fun QuizPlayScreen(
    setup: QuizSetup,
    game: QuizGame,
    region: Region,
    level: Int,
    haptics: Boolean,
    mapLabels: Boolean,
    outcome: RoundOutcome?,
    onBack: () -> Unit,
    onNextLevel: (() -> Unit)?,
) {
    val feel = LocalHapticFeedback.current
    val g = game.given
    LaunchedEffect(g) {
        if (g == null) return@LaunchedEffect
        if (haptics) feel.performHapticFeedback(if (g.correct) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
        if (g.correct) { delay(1100); game.next() } // fout: wacht op 'Volgende'
    }

    if (game.finished) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header(setup, game, region, level, onBack)
            Result(game, level, outcome, onAgain = { game.restart() }, onBack = onBack, onNextLevel = onNextLevel)
            Spacer(Modifier.height(24.dp))
        }
        return
    }
    val q = game.question ?: return

    if (setup.map != null) {
        // ---------- kaartquiz: de kaart neemt de meeste ruimte ----------
        Column(Modifier.fillMaxSize().imePadding()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Header(setup, game, region, level, onBack)
                Progress(game)
                MapPrompt(q)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                QuizMapView(setup.map, q, game, mapLabels)
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (q.mode == Mode.TYPE) TypeAnswer(game, q)
                Feedback(game, q)
                if (game.given == null && q.mode == Mode.TAP) {
                    Text(tr("Pinch to zoom, drag to move.", "Knijp om te zoomen, sleep om te verschuiven."), style = MaterialTheme.typography.bodySmall, color = Night.faint)
                }
            }
        }
    } else {
        // ---------- kaartjesquiz ----------
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(setup, game, region, level, onBack)
            Progress(game)
            AnimatedContent(game.index, transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(120)) }, label = "q") { _ ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CardPrompt(q)
                    when (q.mode) {
                        Mode.TYPE -> TypeAnswer(game, q)
                        else -> if (q.options.any { it.flag != null }) FlagOptions(game, q) else TextOptions(game, q)
                    }
                    Feedback(game, q)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ======================= kop en voortgang =======================

@Composable
private fun Header(setup: QuizSetup, game: QuizGame, region: Region, level: Int, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back", "Terug"), tint = Night.text) }
        Column(Modifier.weight(1f)) {
            Eyebrow(setup.title.text + " · " + region.name.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarsText(level, size = 14)
                Spacer(Modifier.width(8.dp))
                Text("${Levels.all[level - 1].title.text} · ${game.poolSize}" + tr(" to learn", " om te leren"), style = MaterialTheme.typography.bodySmall, color = Night.muted)
            }
        }
    }
}

@Composable
private fun Progress(game: QuizGame) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(tr("Question", "Vraag") + " ${game.index + 1} / ${game.total}", style = MaterialTheme.typography.labelMedium, color = Night.muted, modifier = Modifier.weight(1f))
            Text(tr("${game.correctTotal} correct", "${game.correctTotal} goed"), style = MaterialTheme.typography.labelMedium, color = Night.glow)
        }
        ProgressBar(game.index.toFloat() / game.total.coerceAtLeast(1))
    }
}

// ======================= vragen =======================

@Composable
private fun MapPrompt(q: Question) {
    val p = q.prompt as? Prompt.Words ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(p.lead.text, style = MaterialTheme.typography.bodyMedium, color = Night.muted)
            p.subject?.let { Text(it.text, style = MaterialTheme.typography.headlineLarge, color = Night.text, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        p.flag?.let { FlagImage(it, height = 32.dp, maxWidth = 56.dp) }
    }
}

@Composable
private fun CardPrompt(q: Question) {
    NightCard(Modifier.fillMaxWidth()) {
        when (val p = q.prompt) {
            is Prompt.Words -> {
                Text(p.lead.text, style = MaterialTheme.typography.bodyMedium, color = Night.muted)
                p.subject?.let { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.text, style = MaterialTheme.typography.displayMedium, color = Night.text, modifier = Modifier.weight(1f, fill = false))
                        p.flag?.let { Spacer(Modifier.width(12.dp)); FlagImage(it, height = 36.dp, maxWidth = 64.dp) }
                    }
                }
                p.note?.let { Text(it.text, style = MaterialTheme.typography.bodySmall, color = Night.faint) }
            }
            is Prompt.FlagPic -> {
                Text(p.lead.text, style = MaterialTheme.typography.bodyMedium, color = Night.muted)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 150.dp), contentAlignment = Alignment.Center) {
                    FlagImage(p.country, height = 150.dp, maxWidth = 290.dp, shadow = true)
                }
            }
            is Prompt.ShapePic -> {
                Text(p.lead.text, style = MaterialTheme.typography.bodyMedium, color = Night.muted)
                Spacer(Modifier.height(8.dp))
                ShapeView(p.shape, Modifier.fillMaxWidth().height(190.dp))
            }
        }
    }
}

/** Silhouet van een land, passend in het vak. */
@Composable
private fun ShapeView(shape: nl.topo.app.data.geo.GeoShape, modifier: Modifier) {
    val path = remember(shape) { shape.toPath() }
    val b = shape.bounds
    Canvas(modifier.clip(MaterialTheme.shapes.medium).background(Night.water)) {
        val pad = 16.dp.toPx()
        val s = minOf((size.width - 2 * pad) / b.width, (size.height - 2 * pad) / b.height)
        val dx = (size.width - b.width * s) / 2 - b.left * s
        val dy = (size.height - b.height * s) / 2 - b.top * s
        withTransform({ translate(dx, dy); scale(s, s, Offset.Zero) }) {
            drawPath(path, Night.glow.copy(alpha = 0.18f))
            drawPath(path, Night.glow.copy(alpha = 0.25f), style = Stroke(6f / s, join = StrokeJoin.Round))
            drawPath(path, Night.glow, style = Stroke(1.6f / s, join = StrokeJoin.Round))
        }
    }
}

// ======================= antwoorden =======================

private enum class OptState { IDLE, RIGHT, WRONG, DIM }

private fun optState(game: QuizGame, q: Question, o: Opt): OptState {
    val g = game.given ?: return OptState.IDLE
    return when {
        o.id in q.correct -> OptState.RIGHT
        g.pickedId == o.id -> OptState.WRONG
        else -> OptState.DIM
    }
}

@Composable
private fun TextOptions(game: QuizGame, q: Question) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { o ->
                    OptionBox(optState(game, q, o), Modifier.weight(1f).heightIn(min = 58.dp), { game.pick(o.id) }) {
                        Text(o.label.text, style = MaterialTheme.typography.titleSmall, color = Night.text, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FlagOptions(game: QuizGame, q: Question) {
    val cols = if (q.options.size > 4) 3 else 2
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.options.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { o ->
                    val st = optState(game, q, o)
                    OptionBox(st, Modifier.weight(1f).aspectRatio(1.3f), { game.pick(o.id) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            o.flag?.let { FlagImage(it, height = if (cols == 3) 44.dp else 62.dp, maxWidth = if (cols == 3) 84.dp else 124.dp) }
                            if (game.given?.correct == false && st != OptState.IDLE) {
                                Text(o.label.text, style = MaterialTheme.typography.labelSmall, color = Night.muted, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun OptionBox(state: OptState, modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    val (bg, edge) = when (state) {
        OptState.IDLE -> Night.surface.copy(alpha = 0.92f) to Night.outline
        OptState.RIGHT -> Night.glow.copy(alpha = 0.18f) to Night.glow
        OptState.WRONG -> Night.danger.copy(alpha = 0.18f) to Night.danger
        OptState.DIM -> Night.surface.copy(alpha = 0.6f) to Night.outlineSoft
    }
    val alpha by animateFloatAsState(if (state == OptState.DIM) 0.45f else 1f, label = "dim")
    Box(
        modifier
            .alpha(alpha)
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .border(if (state == OptState.IDLE) 1.dp else 2.dp, edge, MaterialTheme.shapes.medium)
            .clickable(enabled = state == OptState.IDLE, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun TypeAnswer(game: QuizGame, q: Question) {
    var text by remember(q) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(q) { runCatching { focus.requestFocus() } }
    val done = game.given != null
    val strict = LocalAppContainer.current.settings.settings.collectAsStateWithLifecycle(Settings()).value.strictTyping
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (!done) text = it },
            modifier = Modifier.weight(1f).focusRequester(focus),
            placeholder = { Text(if (strict) tr("Type it — exact spelling", "Typ het — precies goed gespeld") else tr("Type it — accents don't matter", "Typ het — accenten maken niet uit"), color = Night.faint) },
            singleLine = true,
            enabled = !done,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { game.type(text) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Night.glow, unfocusedBorderColor = Night.outline,
                focusedContainerColor = Night.surfaceHigh, unfocusedContainerColor = Night.surfaceHigh,
                disabledContainerColor = Night.surface, disabledBorderColor = Night.outlineSoft, disabledTextColor = Night.muted,
                cursorColor = Night.glow, focusedTextColor = Night.text, unfocusedTextColor = Night.text,
            ),
        )
        GlowButton(tr("Check", "Check"), { game.type(text) }, enabled = !done && text.isNotBlank())
    }
}

@Composable
private fun Feedback(game: QuizGame, q: Question) {
    val g = game.given ?: return
    val color = if (g.correct) Night.glow else Night.danger
    val pickedLabel = g.pickedId?.let { id -> q.options.firstOrNull { it.id == id }?.label }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.6f), MaterialTheme.shapes.medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (g.correct) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = color)
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    g.correct -> tr("Correct!", "Goed!") + "  " + q.answer.text
                    g.typed != null -> tr("You typed “${g.typed}”.", "Jij typte “${g.typed}”.")
                    pickedLabel != null -> tr("Not ${pickedLabel.text}.", "Niet ${pickedLabel.text}.")
                    else -> tr("Not quite.", "Net niet.")
                },
                style = MaterialTheme.typography.titleSmall, color = Night.text,
            )
        }
        if (!g.correct) Text(tr("Answer: ", "Antwoord: ") + q.answer.text, style = MaterialTheme.typography.bodyMedium, color = Night.text)
        q.info?.let { Text(it.text, style = MaterialTheme.typography.bodySmall, color = Night.muted) }
        if (!g.correct) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("This one comes back once more at the end.", "Deze komt aan het eind nog een keer terug."), style = MaterialTheme.typography.bodySmall, color = Night.faint, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                GlowButton(tr("Next", "Volgende"), { game.next() })
            }
        }
    }
}

// ======================= kaart =======================

/** De quizkaart: zoomen en schuiven met twee vingers, tikken om te antwoorden. */
@Composable
private fun QuizMapView(map: QuizMap, q: Question, game: QuizGame, mapLabels: Boolean) {
    val lang = LocalLang.current
    val reachPx = with(LocalDensity.current) { 32.dp.toPx() }
    val mz = rememberMapZoom(map)
    var hint by remember(q) { mutableStateOf(false) }
    val base = map.viewport
    val viewport = mz.viewport(base)
    val g = game.given

    Box(
        Modifier
            .fillMaxSize()
            .mapZoomGestures(mz) { base },
    ) {
        NightMap(
            map = map.geo,
            viewport = viewport,
            modifier = Modifier.fillMaxSize(),
            glow = emptySet(),
            graticule = map.graticule,
            onTap = { p, tf ->
                if (q.mode != Mode.TAP || game.given != null) return@NightMap
                val hit = map.hit(p, reachPx / tf.scale)
                if (hit == null) hint = true else game.tap(hit.id)
            },
        ) { tf ->
            SideEffect { mz.scale = tf.scale }
            val focusId = q.focus
            val showFocus = q.mode == Mode.TYPE || g != null
            val rightIds = if (g != null) q.correct else emptySet()
            val wrongId = g?.takeIf { !it.correct }?.pickedId
            Canvas(Modifier.fillMaxSize()) {
                val s = tf.scale
                // vlakken en lijnen in kaart-eenheden
                withTransform({ translate(tf.dx, tf.dy); scale(s, s, Offset.Zero) }) {
                    for (f in map.features) {
                        val col = when {
                            f.id in rightIds -> Night.glow
                            f.id == wrongId -> Night.danger
                            showFocus && f.id == focusId -> Night.ember
                            else -> null
                        }
                        when (f) {
                            is Feature.Area -> if (col != null) {
                                val path = f.shape.toPath()
                                drawPath(path, col.copy(alpha = 0.28f))
                                drawPath(path, col, style = Stroke(2.2f / s, join = StrokeJoin.Round))
                            }
                            is Feature.Line -> {
                                val path = f.shape.toPath(closed = false)
                                drawPath(path, col ?: Night.river, style = Stroke((if (col != null) 4.5f else 2.6f) / s, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                            is Feature.Point -> Unit
                        }
                    }
                }
                // punten in schermpixels, zodat ze altijd even groot zijn
                for (f in map.features) {
                    if (f !is Feature.Point) continue
                    val c = tf.toScreen(f.pos)
                    val col = when {
                        f.id in rightIds -> Night.glow
                        f.id == wrongId -> Night.danger
                        showFocus && f.id == focusId -> Night.ember
                        else -> null
                    }
                    drawCircle(Night.skyDeep, radius = 6.dp.toPx(), center = c)
                    drawCircle(col ?: Night.text, radius = (if (col != null) 5f else 3.6f) * density, center = c)
                    if (col != null) drawCircle(col.copy(alpha = 0.5f), radius = 12.dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
                }
                // ring om kleine uitgelichte vlakken, zodat je ze ziet
                val ringId = if (showFocus) focusId else null
                (map.feature(ringId) as? Feature.Area)?.let { a ->
                    if (a.shape.bounds.width * s < 28.dp.toPx()) {
                        drawCircle(Night.ember, radius = 16.dp.toPx(), center = tf.toScreen(a.labelAt), style = Stroke(2.5.dp.toPx()))
                    }
                }
            }
            // namen
            if (mapLabels) {
                for (f in map.features) {
                    val at = labelPos(f) ?: continue
                    Text(f.label[lang], style = MaterialTheme.typography.labelSmall, color = Night.muted,
                        modifier = Modifier.anchorAt(tf.toScreen(at).copy(y = tf.toScreen(at).y - 8f), 0.5f, 1f))
                }
            }
            if (g != null) {
                (rightIds.firstNotNullOfOrNull { map.feature(it) } ?: map.feature(focusId))?.let { f ->
                    labelPos(f)?.let { at -> Tag(f.label[lang], Night.glow, Modifier.anchorAt(tf.toScreen(at).copy(y = tf.toScreen(at).y - 14f), 0.5f, 1f)) }
                }
                map.feature(wrongId)?.let { f ->
                    labelPos(f)?.let { at -> Tag(f.label[lang], Night.danger, Modifier.anchorAt(tf.toScreen(at).copy(y = tf.toScreen(at).y - 14f), 0.5f, 1f)) }
                }
            }
        }
        // terug naar het hele kader
        if (mz.isZoomed) {
            IconButton(
                onClick = { mz.reset() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(12.dp)).background(Night.surface.copy(alpha = 0.9f)),
            ) { Icon(Icons.Rounded.CenterFocusStrong, tr("Reset view", "Hele kaart"), tint = Night.text) }
        }
        if (hint && game.given == null) {
            Text(
                tr("Tap closer to a place on the map.", "Tik dichter bij een plek op de kaart."),
                style = MaterialTheme.typography.labelMedium, color = Night.text,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).clip(RoundedCornerShape(50)).background(Night.surface.copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

private fun labelPos(f: Feature): Offset? = when (f) {
    is Feature.Area -> f.labelAt
    is Feature.Point -> f.pos
    is Feature.Line -> f.shape.bounds.center
}

@Composable
private fun Tag(text: String, color: Color, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Night.skyDeep.copy(alpha = 0.85f))
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = Night.text) }
}

// ======================= uitslag =======================

@Composable
private fun Result(game: QuizGame, level: Int, outcome: RoundOutcome?, onAgain: () -> Unit, onBack: () -> Unit, onNextLevel: (() -> Unit)?) {
    val record = outcome?.record
    val pct = game.percent
    val color = when { pct >= Levels.MASTERED -> Night.glow; pct >= Levels.OK -> Night.mid; else -> Night.danger }
    NightCard(Modifier.fillMaxWidth()) {
        Eyebrow(tr("Round complete", "Ronde klaar"))
        Text("$pct%", style = MaterialTheme.typography.displayLarge, color = color)
        Text(
            tr("${game.correctFirstTry} of ${game.firstCount} right first time", "${game.correctFirstTry} van ${game.firstCount} in één keer goed") +
                if (record == true) tr(" · new record!", " · nieuw record!") else "",
            style = MaterialTheme.typography.bodyLarge, color = Night.text,
        )
        Text(
            when {
                pct == 100 -> tr("Flawless. Time for a higher level.", "Foutloos. Tijd voor een hoger niveau.")
                pct >= Levels.MASTERED -> tr("Strong. This cell now counts as mastered.", "Sterk. Dit vakje telt als gekend.")
                pct >= Levels.OK -> tr("Getting there. One more round and you'll pass ${Levels.MASTERED}%.", "Op weg. Nog een ronde en je zit boven de ${Levels.MASTERED}%.")
                else -> tr("Keep practising: what you miss comes back more often.", "Blijven oefenen: wat je mist komt vaker langs.")
            },
            style = MaterialTheme.typography.bodyMedium, color = Night.muted,
        )
        outcome?.let { Spacer(Modifier.height(12.dp)); PointsPanel(it) }
        if (game.misses.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Eyebrow(tr("Practise these", "Nog even oefenen"))
            Spacer(Modifier.height(6.dp))
            game.misses.forEach { m ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    val flag = when (val p = m.prompt) {
                        is Prompt.FlagPic -> p.country
                        is Prompt.Words -> p.flag
                        else -> null
                    }
                    if (flag != null) { FlagImage(flag, height = 18.dp); Spacer(Modifier.width(10.dp)) }
                    val subject = (m.prompt as? Prompt.Words)?.subject
                    Text(
                        if (subject != null && subject != m.answer) "${subject.text} → ${m.answer.text}" else m.answer.text,
                        style = MaterialTheme.typography.bodyMedium, color = Night.text,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton(tr("New round", "Nieuwe ronde"), onAgain, Modifier.fillMaxWidth())
            if (onNextLevel != null && pct >= Levels.MASTERED) {
                OutlinedButton(onClick = onNextLevel, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Night.gold), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.gold)) {
                    Text(tr("On to", "Naar") + " ${"★".repeat(level + 1)}", style = MaterialTheme.typography.labelLarge)
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.text)) {
                Text(tr("Back to options", "Terug naar de opties"), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
