package nl.topo.app.ui.puzzle

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.topo.app.data.geo.Country
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.prefs.PuzzleState
import nl.topo.app.data.prefs.RoundOutcome
import nl.topo.app.data.puzzle.FlagCluesPuzzle
import nl.topo.app.data.puzzle.FlagPoints
import nl.topo.app.data.puzzle.FlagZoomPuzzle
import nl.topo.app.ui.components.FlagImage
import nl.topo.app.ui.theme.Night

/** Vlag als bitmap (voor uitsneden). */
@Composable
private fun rememberFlagBitmap(country: Country): ImageBitmap? {
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, country.code) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(country.flagAsset).use { BitmapFactory.decodeStream(it).asImageBitmap() } }.getOrNull()
        }
    }
    return bmp
}

/** Gemeenschappelijk: raden met invoerveld, foute gokken als rode chips, opgeven, uitslag. */
@Composable
private fun GuessGame(
    w: WorldMap, target: Country, maxGuesses: Int, eyebrow: String, title: String, subtitle: String,
    onFinish: suspend (PuzzleResult) -> RoundOutcome?,
    points: (wrong: Int, solved: Boolean) -> Int,
    onBack: () -> Unit, footer: @Composable () -> Unit,
    visual: @Composable (wrong: Int, finished: Boolean) -> Unit,
    extra: @Composable (wrong: Int, finished: Boolean) -> Unit = { _, _ -> },
) {
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val wrongGuesses = remember { mutableStateListOf<String>() }
    var finished by remember { mutableStateOf(false) }
    var solved by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<RoundOutcome?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val wrong = wrongGuesses.size

    fun finish(ok: Boolean, gaveUp: Boolean = false) {
        if (finished) return
        finished = true; solved = ok
        val g = wrongGuesses.toList() + if (ok) listOf(target.code) else emptyList()
        scope.launch {
            outcome = onFinish(PuzzleResult(ok, gaveUp, g, 0, wrongGuesses.size, 1, if (ok) 1 else 0, points(wrongGuesses.size, ok)))
        }
    }

    fun guess(c: Country) {
        when {
            c.code == target.code -> { message = null; finish(true) }
            c.code in wrongGuesses -> message = lang.t("You already tried ${c.name(lang)}.", "${c.name(lang)} had je al geprobeerd.")
            else -> {
                wrongGuesses += c.code
                message = lang.t("Not ${c.name(lang)}.", "Niet ${c.name(lang)}.")
                if (wrongGuesses.size >= maxGuesses) finish(false)
            }
        }
    }

    PuzzleLayout(
        eyebrow = eyebrow, title = title,
        subtitle = subtitle + " " + tr("${(maxGuesses - wrong).coerceAtLeast(0)} guesses left.", "Nog ${(maxGuesses - wrong).coerceAtLeast(0)} keer raden."),
        onBack = onBack,
        map = { Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { visual(wrong, finished) } },
    ) {
        extra(wrong, finished)
        if (wrongGuesses.isNotEmpty()) Chips(w, wrongGuesses.map { it to Night.danger })
        if (!finished) {
            CountryInput(w) { guess(it) }
            message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Night.muted) }
            Row {
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { finish(false, gaveUp = true) }, border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.muted)) {
                    Text(tr("Give up", "Opgeven"))
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlagImage(target, height = 22.dp); Spacer(Modifier.width(10.dp))
                Text(
                    if (solved) tr("${target.name(lang)}! Found with ${wrong} wrong guesses.", "${target.name(lang)}! Gevonden met ${wrong} foute gokken.")
                    else tr("It was ${target.name(lang)}.", "Het was ${target.name(lang)}."),
                    style = MaterialTheme.typography.titleSmall, color = if (solved) Night.glow else Night.text,
                )
            }
            Result(outcome, PuzzleState(), footer)
        }
    }
}

/** Vlag van dichtbij: een stukje van de vlag; elke foute gok zoomt verder uit. */
@Composable
fun FlagZoomGame(
    w: WorldMap, p: FlagZoomPuzzle, eyebrow: String, hard: Boolean,
    onFinish: suspend (PuzzleResult) -> RoundOutcome?, onBack: () -> Unit, footer: @Composable () -> Unit,
) {
    val target = w.countries.getValue(p.target)
    val bmp = rememberFlagBitmap(target)
    GuessGame(
        w, target, p.maxGuesses, eyebrow,
        title = tr("Whose flag is this?", "Van wie is deze vlag?"),
        subtitle = tr("You see a small piece. Every wrong guess zooms out a little.", "Je ziet een klein stukje. Elke foute gok zoomt iets verder uit."),
        onFinish = onFinish, points = { wrong, ok -> FlagPoints.zoom(wrong, ok, hard) }, onBack = onBack, footer = footer,
        visual = { wrong, finished ->
            // Stukje: deel van de vlaghoogte; na de laatste stap (of na afloop) de hele vlag.
            val target01 = if (finished || wrong >= p.stages.size) 1f else p.stages[wrong]
            val f by animateFloatAsState(target01, tween(600), label = "zoom")
            val b = bmp
            if (b == null) CircularProgressIndicator(color = Night.glow)
            else Box(
                Modifier.fillMaxHeight(0.92f).aspectRatio(if (f >= 0.999f) b.width / b.height.toFloat() else 1f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(14.dp)).border(1.dp, Night.outline, RoundedCornerShape(14.dp)),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val bw = b.width; val bh = b.height
                    // Vierkant uitsnijden rond het gekozen punt; bij f = 1 de hele vlag.
                    val (sw, sh) = if (f >= 0.999f) bw to bh else {
                        val side = (bh * f).toInt().coerceIn(8, minOf(bw, bh)); side to side
                    }
                    val cx = (p.spotX * bw).toInt(); val cy = (p.spotY * bh).toInt()
                    val sx = (cx - sw / 2).coerceIn(0, bw - sw); val sy = (cy - sh / 2).coerceIn(0, bh - sh)
                    drawImage(
                        b, srcOffset = IntOffset(sx, sy), srcSize = IntSize(sw, sh),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Medium,
                    )
                }
            }
        },
    )
}

/** Vlag-omschrijving: aanwijzingen over de vlag; elke foute gok geeft er een bij. */
@Composable
fun FlagCluesGame(
    w: WorldMap, p: FlagCluesPuzzle, eyebrow: String, hard: Boolean,
    onFinish: suspend (PuzzleResult) -> RoundOutcome?, onBack: () -> Unit, footer: @Composable () -> Unit,
) {
    val target = w.countries.getValue(p.target)
    GuessGame(
        w, target, p.maxGuesses, eyebrow,
        title = tr("Which flag is described?", "Welke vlag wordt beschreven?"),
        subtitle = tr("Every wrong guess gives you another clue.", "Elke foute gok geeft je een nieuwe aanwijzing."),
        onFinish = onFinish, points = { wrong, ok -> FlagPoints.clues(wrong, ok, hard) }, onBack = onBack, footer = footer,
        visual = { wrong, finished ->
            if (finished) FlagImage(target, height = 150.dp)
            else {
                val shown = (p.shownAtStart + wrong).coerceAtMost(p.clues.size)
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.clues.take(shown).forEachIndexed { i, c ->
                        Row(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                                .background(if (i == shown - 1 && wrong > 0) Night.gold.copy(alpha = 0.14f) else Night.surface.copy(alpha = 0.9f))
                                .border(1.dp, Night.outline, MaterialTheme.shapes.small).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = Night.gold, modifier = Modifier.width(22.dp))
                            Text(c.text, style = MaterialTheme.typography.bodyMedium, color = Night.text)
                        }
                    }
                    if (shown < p.clues.size) Text(
                        tr("${p.clues.size - shown} more clues to come", "Nog ${p.clues.size - shown} aanwijzingen te gaan"),
                        style = MaterialTheme.typography.labelSmall, color = Night.faint, modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
    )
}
