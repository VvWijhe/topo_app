package nl.topo.app.ui.puzzle

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.prefs.RoundOutcome
import nl.topo.app.data.puzzle.FlagShow
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.FlagImage
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.PointsPanel
import nl.topo.app.ui.theme.Night
import kotlin.random.Random

/** Pico: grasgroene papegaai, presentator van Vlaggenweetjes. Wiebelt zachtjes. */
@Composable
fun Pico(modifier: Modifier = Modifier, happy: Boolean? = null) {
    val wobble by rememberInfiniteTransition(label = "pico").animateFloat(-4f, 4f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "w")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val green = Color(0xFF3DBE4B); val dark = Color(0xFF248A33); val belly = Color(0xFFA8E06A)
        rotate(wobble, Offset(w / 2, h * 0.9f)) {
            // staart
            val tail = Path().apply { moveTo(w * 0.42f, h * 0.78f); lineTo(w * 0.5f, h * 1.0f); lineTo(w * 0.58f, h * 0.78f); close() }
            drawPath(tail, Color(0xFF2F7DE1))
            // lijf en buik
            drawOval(green, Offset(w * 0.22f, h * 0.32f), Size(w * 0.56f, h * 0.52f))
            drawOval(belly, Offset(w * 0.33f, h * 0.48f), Size(w * 0.34f, h * 0.32f))
            // vleugel
            drawOval(dark, Offset(w * 0.2f, h * 0.45f), Size(w * 0.18f, h * 0.3f))
            // kop
            drawCircle(green, w * 0.24f, Offset(w * 0.5f, h * 0.3f))
            // kuifje
            drawCircle(Color(0xFFFFC83D), w * 0.06f, Offset(w * 0.5f, h * 0.07f))
            // oog
            drawCircle(Color.White, w * 0.085f, Offset(w * 0.6f, h * 0.27f))
            drawCircle(Color(0xFF14202E), w * 0.042f, Offset(w * 0.62f, h * 0.27f))
            drawCircle(Color.White, w * 0.014f, Offset(w * 0.635f, h * 0.255f))
            // kromme snavel
            val beak = Path().apply {
                moveTo(w * 0.68f, h * 0.31f); quadraticTo(w * 0.86f, h * 0.31f, w * 0.8f, h * 0.45f)
                quadraticTo(w * 0.74f, h * 0.4f, w * 0.68f, h * 0.4f); close()
            }
            drawPath(beak, Color(0xFFFF8A3D))
            // wangen: blij of verbaasd
            if (happy == true) drawCircle(Color(0x55FF6B6B), w * 0.05f, Offset(w * 0.47f, h * 0.36f))
            // pootjes
            drawRect(Color(0xFF8A6A3D), Offset(w * 0.4f, h * 0.83f), Size(w * 0.05f, h * 0.06f))
            drawRect(Color(0xFF8A6A3D), Offset(w * 0.55f, h * 0.83f), Size(w * 0.05f, h * 0.06f))
        }
    }
}

/** Tekstballon van Pico. */
@Composable
private fun Speech(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(Night.surface.copy(alpha = 0.95f))
            .border(1.dp, Night.outline, RoundedCornerShape(16.dp)).padding(14.dp),
    ) { Text(text, style = MaterialTheme.typography.bodyMedium, color = Night.text) }
}

/** Vlaggenweetjes: een aflevering van 8 vragen met Pico. */
@Composable
fun FlagShowScreen(onBack: () -> Unit) {
    val app = LocalAppContainer.current
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()
    val world by produceState<WorldMap?>(null) { value = app.geo.world() }
    val w = world
    var seed by rememberSaveable { mutableLongStateOf(Random.nextLong()) }
    var index by rememberSaveable(seed) { mutableIntStateOf(-1) } // -1 = intro
    var correct by rememberSaveable(seed) { mutableIntStateOf(0) }
    var picked by rememberSaveable(seed, index) { mutableStateOf<String?>(null) }
    var outcome by remember(seed) { mutableStateOf<RoundOutcome?>(null) }
    if (w == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Night.glow) }
        return
    }
    val episode = remember(w, seed) { FlagShow.episode(w, Random(seed)) }
    val total = episode.size

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back", "Terug"), tint = Night.text) }
            Eyebrow(tr("Flag Facts with Pico", "Vlaggenweetjes met Pico"), color = Night.gold, modifier = Modifier.weight(1f))
            if (index in 0 until total) Text("${index + 1}/$total", style = MaterialTheme.typography.labelLarge, color = Night.muted)
        }
        when {
            index < 0 -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Pico(Modifier.size(110.dp))
                    Spacer(Modifier.width(8.dp))
                    Speech(tr(
                        "Hello! I'm Pico, and I know everything about flags. Well, almost everything. $total questions, and after each one I'll tell you something you didn't know. Ready?",
                        "Hallo! Ik ben Pico, en ik weet alles van vlaggen. Nou ja, bijna alles. $total vragen, en na elke vraag vertel ik je iets wat je nog niet wist. Klaar?",
                    ), Modifier.weight(1f))
                }
                GlowButton(tr("Let's go", "Beginnen"), { index = 0 }, Modifier.fillMaxWidth())
            }
            index < total -> {
                val (q, options) = episode[index]
                Text(q.q.text, style = MaterialTheme.typography.headlineSmall, color = Night.text)
                options.forEach { code ->
                    val c = w.countries[code] ?: return@forEach
                    val state = when {
                        picked == null -> 0
                        code == q.answer -> 1
                        code == picked -> 2
                        else -> 3
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                            .background(when (state) { 1 -> Night.glow.copy(alpha = 0.16f); 2 -> Night.danger.copy(alpha = 0.14f); else -> Night.surface.copy(alpha = 0.9f) })
                            .border(1.5.dp, when (state) { 1 -> Night.glow; 2 -> Night.danger; else -> Night.outline }, MaterialTheme.shapes.medium)
                            .clickable(enabled = picked == null) {
                                picked = code
                                if (code == q.answer) correct++
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FlagImage(c, height = 26.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(c.name(lang), style = MaterialTheme.typography.titleMedium, color = if (state == 3) Night.muted else Night.text)
                    }
                }
                if (picked != null) {
                    val right = picked == q.answer
                    Row(verticalAlignment = Alignment.Bottom) {
                        Pico(Modifier.size(90.dp), happy = right)
                        Spacer(Modifier.width(8.dp))
                        val opener = if (right) listOf(tr("Correct! Naturally.", "Juist! Uiteraard."), tr("Exactly right. I'm impressed. A little.", "Precies goed. Ik ben onder de indruk. Een beetje."), tr("Yes! You've been studying.", "Ja! Jij hebt opgelet."))[index % 3]
                        else listOf(tr("Ah, a classic mistake. Allow me…", "Ach, een klassieke vergissing. Sta me toe…"), tr("Not quite. Listen carefully now.", "Net niet. Let nu goed op."), tr("Wrong, but don't worry, I'll explain.", "Fout, maar geen zorgen, ik leg het uit."))[index % 3]
                        Speech("$opener ${q.fact.text}", Modifier.weight(1f))
                    }
                    GlowButton(if (index + 1 < total) tr("Next question", "Volgende vraag") else tr("See my score", "Bekijk mijn score"), {
                        val next = index + 1
                        index = next
                        if (next >= total) scope.launch {
                            outcome = app.progress.finishPuzzle(null, "FLAG_SHOW", correct == total, FlagShow.points(correct, total))
                        }
                    }, Modifier.fillMaxWidth())
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Pico(Modifier.size(110.dp), happy = correct >= total / 2)
                    Spacer(Modifier.width(8.dp))
                    Speech(when {
                        correct == total -> tr("All $total right! You could almost present this show yourself. Almost.", "Alle $total goed! Jij zou deze show bijna zelf kunnen presenteren. Bijna.")
                        correct >= total * 3 / 4 -> tr("$correct out of $total. Very respectable.", "$correct van de $total. Zeer verdienstelijk.")
                        correct >= total / 2 -> tr("$correct out of $total. Not bad, but I expect more next time.", "$correct van de $total. Niet slecht, maar volgende keer verwacht ik meer.")
                        else -> tr("$correct out of $total. Don't worry, that's why you have me.", "$correct van de $total. Geen zorgen, daar heb je mij voor.")
                    }, Modifier.weight(1f))
                }
                outcome?.let { PointsPanel(it) }
                GlowButton(tr("New episode", "Nieuwe aflevering"), { seed = Random.nextLong() }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
