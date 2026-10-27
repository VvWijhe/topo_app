package nl.topo.app.ui.puzzle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.North
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.topo.app.data.daily.DailyChallenges
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.prefs.PuzzleState
import nl.topo.app.data.prefs.RoundOutcome
import nl.topo.app.data.prefs.Settings
import nl.topo.app.data.puzzle.MysteryPuzzle
import nl.topo.app.data.puzzle.PuzzleKind
import nl.topo.app.data.puzzle.PuzzlePoints
import nl.topo.app.data.puzzle.RoutePuzzle
import nl.topo.app.data.quiz.Matching
import nl.topo.app.data.quiz.Names
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.FlagImage
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.MapTransform
import nl.topo.app.ui.components.NightMap
import nl.topo.app.ui.components.PointsPanel
import nl.topo.app.ui.components.anchorAt
import nl.topo.app.ui.components.mapZoomGestures
import nl.topo.app.ui.components.rememberMapZoom
import nl.topo.app.ui.theme.Night
import kotlin.random.Random

/** Uitslag van een puzzel. [points] = basispunten; de aanroeper schaalt (oefenen: de helft). */
data class PuzzleResult(
    val solved: Boolean,
    val gaveUp: Boolean,
    val guesses: List<String>,
    val hints: Int,
    /** Gokken die niet raak waren. */
    val wrong: Int,
    /** Wat je moest vinden (route: landen ertussen; mysterieland: 1). */
    val total: Int,
    /** Hoeveel daarvan je had. */
    val found: Int,
    val points: Int,
)

/** Oefenpuzzel: elke "Nieuwe puzzel" een nieuwe, voor halve punten. */
@Composable
fun PuzzleScreen(kind: PuzzleKind, hard: Boolean = false, onBack: () -> Unit) {
    val app = LocalAppContainer.current
    val world by produceState<WorldMap?>(null) { value = app.geo.world() }
    val w = world
    var seed by rememberSaveable { mutableLongStateOf(Random.nextLong()) }
    if (w == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Night.glow) }
        return
    }
    val save: suspend (List<String>, Int) -> Unit = { _, _ -> }
    val finish: suspend (PuzzleResult) -> RoundOutcome = { r -> app.progress.finishPuzzle(null, kind.name, r.solved, r.points / 2) }
    val footer: @Composable () -> Unit = { GlowButton(tr("New puzzle", "Nieuwe puzzel"), { seed = Random.nextLong() }, Modifier.fillMaxWidth()) }
    when (kind) {
        PuzzleKind.ROUTE -> {
            val p = remember(w, seed) { Random(seed).let { r -> RoutePuzzle.generate(w, r, if (hard) (7..10).random(r) else (4..6).random(r)) } }
            androidx.compose.runtime.key(seed) {
                RouteGame(w, p, PuzzleState(), tr("Route", "Route") + " · " + (if (hard) tr("hard", "moeilijk") else tr("easy", "makkelijk")), save, finish, onBack, footer)
            }
        }
        PuzzleKind.MYSTERY -> {
            val p = remember(w, seed) { MysteryPuzzle.generate(w, Random(seed)) }
            androidx.compose.runtime.key(seed) {
                MysteryGame(w, p, PuzzleState(), tr("Mystery country", "Mysterieland") + " · " + (if (hard) tr("hard", "moeilijk") else tr("easy", "makkelijk")), !hard, save, finish, onBack, footer)
            }
        }
    }
}

// ============================ route ============================

@Composable
fun RouteGame(
    w: WorldMap, p: RoutePuzzle, stored: PuzzleState, eyebrow: String,
    onSave: suspend (List<String>, Int) -> Unit,
    onFinish: suspend (PuzzleResult) -> RoundOutcome?,
    onBack: () -> Unit,
    footer: @Composable () -> Unit,
    status: String? = null,
) {
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val guesses = remember { mutableStateListOf<String>().apply { addAll(stored.guesses) } }
    var hints by remember { mutableIntStateOf(stored.hints) }
    var finished by remember { mutableStateOf(stored.finished) }
    var solved by remember { mutableStateOf(stored.solved) }
    var outcome by remember { mutableStateOf<RoundOutcome?>(null) }
    var message by remember { mutableStateOf<Pair<String, Color>?>(null) }
    val from = w.countries.getValue(p.from)
    val to = w.countries.getValue(p.to)
    val left = p.maxGuesses - guesses.size

    fun finish(ok: Boolean, gaveUp: Boolean = false) {
        if (finished) return
        finished = true; solved = ok
        val g = guesses.toList()
        val onRoute = g.count { p.fit(it) == RoutePuzzle.Fit.ON_ROUTE }
        scope.launch {
            onSave(g, hints)
            outcome = onFinish(PuzzleResult(
                solved = ok, gaveUp = gaveUp, guesses = g, hints = hints, wrong = g.size - onRoute,
                total = p.needed, found = if (ok) p.needed else onRoute.coerceAtMost(p.needed),
                points = PuzzlePoints.route(p.needed, g.size, hints, ok),
            ))
        }
    }

    fun guess(c: Country) {
        when {
            c.code == p.from || c.code == p.to -> message = lang.t("That's where you start or finish.", "Daar begin of eindig je.") to Night.muted
            c.code in guesses -> message = lang.t("You already named ${c.name(lang)}.", "${c.name(lang)} had je al.") to Night.muted
            else -> {
                guesses += c.code
                message = when (p.fit(c.code)) {
                    RoutePuzzle.Fit.ON_ROUTE -> lang.t("${c.name(lang)} is on a shortest route.", "${c.name(lang)} ligt op een kortste route.") to Night.glow
                    RoutePuzzle.Fit.CLOSE -> lang.t("${c.name(lang)}: close, one detour.", "${c.name(lang)}: dichtbij, één omweg.") to Night.gold
                    RoutePuzzle.Fit.OFF -> lang.t("${c.name(lang)} isn't on the way.", "${c.name(lang)} ligt niet op de route.") to Night.danger
                }
                scope.launch { onSave(guesses.toList(), hints) }
                when {
                    p.solved(guesses) -> finish(true)
                    guesses.size >= p.maxGuesses -> finish(false)
                }
            }
        }
    }

    fun hint() {
        val h = p.hint(guesses) ?: return
        hints++
        guess(w.countries.getValue(h))
    }

    PuzzleLayout(
        eyebrow = eyebrow + " · " + when {
            p.needed <= 3 -> tr("short", "kort")
            p.needed <= 5 -> tr("medium", "middel")
            else -> tr("long", "lang")
        },
        title = tr("From ${from.name(lang)} to ${to.name(lang)}", "Van ${from.name(lang)} naar ${to.name(lang)}"),
        subtitle = tr(
            "Travel over land: name the ${p.needed} countries in between on the shortest route. $left guesses left.",
            "Reis over land: noem de ${p.needed} landen ertussen op de kortste route. Nog $left keer raden.",
        ) + (status?.let { " · $it" } ?: ""),
        onBack = onBack,
        map = { RouteMap(w, p, guesses, finished) },
    ) {
        if (guesses.isNotEmpty()) Chips(w, guesses.map { it to fitColor(p.fit(it)) })
        if (!finished) {
            CountryInput(w) { guess(it) }
            message?.let { (m, c) -> Text(m, style = MaterialTheme.typography.labelMedium, color = c) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { hint() }, border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.text)) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = Night.gold); Spacer(Modifier.width(6.dp)); Text(tr("Hint (uses a guess)", "Hint (kost een gok)"))
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { finish(false, gaveUp = true) }, border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.muted)) {
                    Text(tr("Give up", "Opgeven"))
                }
            }
            Legend()
        } else {
            val route = p.route(guesses)
            Text(
                if (solved) tr("You made it in ${guesses.size} guesses!", "Gelukt in ${guesses.size} keer raden!")
                else tr("Not this time. A shortest route:", "Deze keer niet. Een kortste route:"),
                style = MaterialTheme.typography.titleSmall, color = if (solved) Night.glow else Night.text,
            )
            Text(route.mapNotNull { w.countries[it]?.name(lang) }.joinToString(" → "), style = MaterialTheme.typography.bodyMedium, color = Night.text)
            Result(outcome, stored, footer)
        }
    }
}

private fun fitColor(f: RoutePuzzle.Fit) = when (f) {
    RoutePuzzle.Fit.ON_ROUTE -> Night.glow
    RoutePuzzle.Fit.CLOSE -> Night.gold
    RoutePuzzle.Fit.OFF -> Night.danger
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(Night.glow to tr("on the route", "op de route"), Night.gold to tr("one detour", "één omweg"), Night.danger to tr("off the route", "niet op de route")).forEach { (c, t) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(c)); Spacer(Modifier.width(4.dp))
                Text(t, style = MaterialTheme.typography.labelSmall, color = Night.muted)
            }
        }
    }
}

@Composable
private fun RouteMap(w: WorldMap, p: RoutePuzzle, guesses: List<String>, finished: Boolean) {
    val lang = LocalLang.current
    val base = remember(p) { p.viewport(w) }
    val route = if (finished) p.route(guesses) else emptyList()
    val shown = buildSet { add(p.from); add(p.to); addAll(guesses); addAll(route) }
    val fills = buildList {
        add(p.from to Night.ember); add(p.to to Night.ember)
        guesses.forEach { add(it to fitColor(p.fit(it))) }
        route.filter { it !in guesses && it != p.from && it != p.to }.forEach { add(it to Night.muted) }
    }
    PuzzleMap(w, base, shown, fills) { tf ->
        listOf(p.from, p.to).forEach { code ->
            w.countries[code]?.let { c ->
                Text(c.name(lang), style = MaterialTheme.typography.labelMedium, color = Night.ember,
                    modifier = Modifier.anchorAt(tf.toScreen(c.label), 0.5f, 0.5f).clip(RoundedCornerShape(4.dp)).background(Night.skyDeep.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        (guesses + route).distinct().filter { it != p.from && it != p.to }.forEach { code ->
            w.countries[code]?.let { c ->
                Text(c.name(lang), style = MaterialTheme.typography.labelSmall, color = Night.text,
                    modifier = Modifier.anchorAt(tf.toScreen(c.label), 0.5f, 0.5f).clip(RoundedCornerShape(4.dp)).background(Night.skyDeep.copy(alpha = 0.6f)).padding(horizontal = 4.dp, vertical = 1.dp))
            }
        }
    }
}

// ============================ mysterieland ============================

@Composable
fun MysteryGame(
    w: WorldMap, p: MysteryPuzzle, stored: PuzzleState, eyebrow: String, hintsAllowed: Boolean,
    onSave: suspend (List<String>, Int) -> Unit,
    onFinish: suspend (PuzzleResult) -> RoundOutcome?,
    onBack: () -> Unit,
    footer: @Composable () -> Unit,
    status: String? = null,
) {
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val feel = LocalHapticFeedback.current
    val guesses = remember { mutableStateListOf<String>().apply { addAll(stored.guesses) } }
    var hints by remember { mutableIntStateOf(stored.hints) }
    var finished by remember { mutableStateOf(stored.finished) }
    var solved by remember { mutableStateOf(stored.solved) }
    var outcome by remember { mutableStateOf<RoundOutcome?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val target = w.countries.getValue(p.target)
    val clues = guesses.mapNotNull { p.clue(w, it) }

    fun finish(ok: Boolean, gaveUp: Boolean = false) {
        if (finished) return
        finished = true; solved = ok
        val g = guesses.toList()
        scope.launch {
            onSave(g, hints)
            outcome = onFinish(PuzzleResult(
                solved = ok, gaveUp = gaveUp, guesses = g, hints = hints, wrong = if (ok) g.size - 1 else g.size,
                total = 1, found = if (ok) 1 else 0, points = PuzzlePoints.mystery(g.size, hints, ok),
            ))
        }
    }

    fun guess(c: Country) {
        if (c.code in guesses) { message = lang.t("You already tried ${c.name(lang)}.", "${c.name(lang)} had je al geprobeerd."); return }
        if (c.capitalLatLon == null) { message = lang.t("No capital known for ${c.name(lang)}; try another.", "Geen hoofdstad bekend voor ${c.name(lang)}; probeer een ander."); return }
        guesses += c.code
        message = null
        feel.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch { onSave(guesses.toList(), hints) }
        if (c.code == p.target) finish(true)
    }

    PuzzleLayout(
        eyebrow = eyebrow,
        title = tr("Which country is it?", "Welk land is het?"),
        subtitle = tr(
            "Guess any country. You'll see how far its capital is from the mystery capital, and in which direction.",
            "Raad een land. Je ziet hoe ver de hoofdstad van de mysterie-hoofdstad ligt, en in welke richting.",
        ) + (status?.let { " · $it" } ?: ""),
        onBack = onBack,
        map = { MysteryMap(w, p, clues, finished) },
    ) {
        if (!finished) {
            CountryInput(w) { guess(it) }
            message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Night.muted) }
            if (hints >= 1) target.continents.firstOrNull()?.let { cont ->
                w.continents[cont]?.let { Text(tr("Hint: it's in ${it.name(lang)}.", "Hint: het ligt in ${it.name(lang)}."), style = MaterialTheme.typography.bodySmall, color = Night.gold) }
            }
            if (hints >= 2) Text(tr("Hint: it starts with “${target.name(lang).take(1)}”.", "Hint: het begint met “${target.name(lang).take(1)}”."), style = MaterialTheme.typography.bodySmall, color = Night.gold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hintsAllowed && hints < 2) OutlinedButton(onClick = {
                    hints++
                    scope.launch { onSave(guesses.toList(), hints) }
                }, border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.text)) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = Night.gold); Spacer(Modifier.width(6.dp)); Text(tr("Hint (−20)", "Hint (−20)"))
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { finish(false, gaveUp = true) }, border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.muted)) {
                    Text(tr("Give up", "Opgeven"))
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlagImage(target, height = 22.dp); Spacer(Modifier.width(10.dp))
                Text(
                    if (solved) tr("${target.name(lang)}! Found in ${guesses.size} guesses.", "${target.name(lang)}! Gevonden in ${guesses.size} keer.")
                    else tr("It was ${target.name(lang)}.", "Het was ${target.name(lang)}."),
                    style = MaterialTheme.typography.titleSmall, color = if (solved) Night.glow else Night.text,
                )
            }
            Result(outcome, stored, footer)
        }
        // Gokken, dichtstbijzijnde bovenaan
        clues.sortedByDescending { it.closeness }.forEach { c -> ClueRow(w, c) }
    }
}

/** Koud (blauw) → warm (rood) naar nabijheid. */
private fun heat(closeness: Int): Color {
    val t = (closeness / 100f).let { it * it }
    return if (t < 0.5f) lerp(Color(0xFF4C8DFF), Night.gold, t * 2) else lerp(Night.gold, Color(0xFFFF5A4E), (t - 0.5f) * 2)
}

@Composable
private fun ClueRow(w: WorldMap, c: MysteryPuzzle.Clue) {
    val lang = LocalLang.current
    val country = w.countries[c.code] ?: return
    val col = if (c.km == 0 || c.closeness == 100) Night.glow else heat(c.closeness)
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Night.surfaceHigh.copy(alpha = 0.6f))
            .border(1.dp, col.copy(alpha = 0.6f), MaterialTheme.shapes.small).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagImage(country, height = 14.dp); Spacer(Modifier.width(8.dp))
        Text(country.name(lang), style = MaterialTheme.typography.labelLarge, color = Night.text, modifier = Modifier.weight(1f))
        if (c.closeness == 100) {
            Text("✓", style = MaterialTheme.typography.labelLarge, color = Night.glow)
        } else {
            Text("%,d km".format(c.km), style = MaterialTheme.typography.labelMedium, color = Night.muted)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.North, null, tint = col, modifier = Modifier.size(18.dp).rotate(c.bearing.toFloat()))
            Spacer(Modifier.width(8.dp))
            Text("${c.closeness}%", style = MaterialTheme.typography.labelLarge, color = col, modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
private fun MysteryMap(w: WorldMap, p: MysteryPuzzle, clues: List<MysteryPuzzle.Clue>, finished: Boolean) {
    val lang = LocalLang.current
    val shown = buildSet { clues.forEach { add(it.code) }; if (finished) add(p.target) }
    val fills = clues.map { it.code to (if (it.code == p.target) Night.glow else heat(it.closeness)) } +
        (if (finished && clues.none { it.code == p.target }) listOf(p.target to Night.glow) else emptyList())
    PuzzleMap(w, w.worldViewport, shown, fills) { tf ->
        if (finished) w.countries[p.target]?.let { c ->
            Text(c.name(lang), style = MaterialTheme.typography.labelMedium, color = Night.glow,
                modifier = Modifier.anchorAt(tf.toScreen(c.label), 0.5f, 0.5f).clip(RoundedCornerShape(4.dp)).background(Night.skyDeep.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

// ============================ gedeeld ============================

@Composable
private fun PuzzleLayout(
    eyebrow: String, title: String, subtitle: String, onBack: () -> Unit,
    map: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back", "Terug"), tint = Night.text) }
                Eyebrow(eyebrow, color = Night.gold)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Night.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Night.muted)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f).fillMaxWidth().heightIn(min = 180.dp)) { map() }
        Column(
            Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 10.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content() }
    }
}

/** Kaart zonder verraderlijke grenzen: alleen [shown] krijgt een rand; [fills] kleurt landen in. */
@Composable
private fun PuzzleMap(
    w: WorldMap, base: Rect, shown: Set<String>, fills: List<Pair<String, Color>>,
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.(MapTransform) -> Unit,
) {
    val mz = rememberMapZoom(base)
    val paths = remember(w) { HashMap<String, Path>() }
    Box(Modifier.fillMaxSize().mapZoomGestures(mz) { base }) {
        NightMap(map = w.geo, viewport = mz.viewport(base), modifier = Modifier.fillMaxSize(), borders = shown) { tf ->
            SideEffect { mz.scale = tf.scale }
            Canvas(Modifier.fillMaxSize()) {
                val s = tf.scale
                withTransform({ translate(tf.dx, tf.dy); scale(s, s, Offset.Zero) }) {
                    for ((code, col) in fills) {
                        val path = paths.getOrPut(code) { w.geo.countries[code]?.toPath() ?: Path() }
                        drawPath(path, col.copy(alpha = 0.32f))
                        drawPath(path, col, style = Stroke(1.8f / s, join = StrokeJoin.Round))
                    }
                }
            }
            overlay(tf)
        }
        if (mz.isZoomed) {
            IconButton(
                onClick = { mz.reset() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(12.dp)).background(Night.surface.copy(alpha = 0.9f)),
            ) { Icon(Icons.Rounded.CenterFocusStrong, tr("Reset view", "Hele kaart"), tint = Night.text) }
        }
    }
}

/** Invoerveld: typ een land; bekende landen gaan naar [onCountry], onbekende namen geven een melding. */
@Composable
private fun CountryInput(w: WorldMap, onCountry: (Country) -> Unit) {
    val app = LocalAppContainer.current
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    var text by remember { mutableStateOf("") }
    var unknown by remember { mutableStateOf<String?>(null) }
    fun submit() {
        val t = text.trim()
        if (t.isEmpty()) return
        val c = w.all.firstOrNull { Names.country(it).let { n -> if (settings.strictTyping) Matching.matchesStrict(t, n) else Matching.matches(t, n) } }
        if (c == null) unknown = t else { unknown = null; onCountry(c) }
        text = ""
    }
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
        GlowButton(tr("Go", "Raad"), { submit() }, enabled = text.isNotBlank())
    }
    unknown?.let { Text(tr("“$it” isn't a country we know — check the spelling.", "“$it” kennen we niet als land — check de spelling."), style = MaterialTheme.typography.labelMedium, color = Night.muted) }
}

@Composable
private fun Chips(w: WorldMap, items: List<Pair<String, Color>>) {
    val lang = LocalLang.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.reversed().forEach { (code, col) ->
            val c = w.countries[code] ?: return@forEach
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Night.surfaceHigh).border(1.dp, col.copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlagImage(c, height = 14.dp); Spacer(Modifier.width(6.dp))
                Text(c.name(lang), style = MaterialTheme.typography.labelMedium, color = Night.text)
            }
        }
    }
}

@Composable
private fun Result(outcome: RoundOutcome?, stored: PuzzleState, footer: @Composable () -> Unit) {
    outcome?.let { PointsPanel(it) } ?: run {
        if (stored.finished) Text(tr("Points earned: +${stored.points}", "Punten verdiend: +${stored.points}"), style = MaterialTheme.typography.bodyMedium, color = Night.gold)
    }
    footer()
}
