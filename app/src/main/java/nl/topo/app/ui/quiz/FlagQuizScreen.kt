package nl.topo.app.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import nl.topo.app.data.content.Levels
import nl.topo.app.data.content.Region
import nl.topo.app.data.geo.Country
import nl.topo.app.data.quiz.FlagGame
import nl.topo.app.data.quiz.FlagQuestion
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.ProgressBar
import nl.topo.app.ui.components.StarsText
import nl.topo.app.ui.home.formatPopulation
import nl.topo.app.ui.components.FlagImage
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.ui.theme.Night

@Composable
fun FlagQuizScreen(
    game: FlagGame,
    region: Region,
    haptics: Boolean,
    record: Boolean?,
    onBack: () -> Unit,
    onNextLevel: (() -> Unit)?,
) {
    val feel = LocalHapticFeedback.current
    val a = game.answer
    LaunchedEffect(a) {
        if (a == null) return@LaunchedEffect
        if (haptics) feel.performHapticFeedback(if (a.correct) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
        if (a.correct) { delay(1100); game.next() } // fout: wacht op 'Volgende'
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------- kop ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back", "Terug"), tint = Night.text) }
            Column(Modifier.weight(1f)) {
                Eyebrow(tr("Flags", "Vlaggen") + " · " + region.name.text)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarsText(game.level, size = 14)
                    Spacer(Modifier.width(8.dp))
                    Text("${Levels.all[game.level - 1].title.text} · " + tr("${game.poolSize} countries", "${game.poolSize} landen"), style = MaterialTheme.typography.bodySmall, color = Night.muted)
                }
            }
        }

        if (game.finished) {
            Result(game, record, onAgain = { game.restart() }, onBack = onBack, onNextLevel = onNextLevel)
            return@Column
        }
        val q = game.question ?: return@Column

        // ---------- voortgang ----------
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(tr("Question", "Vraag") + " ${game.index + 1} / ${game.total}", style = MaterialTheme.typography.labelMedium, color = Night.muted, modifier = Modifier.weight(1f))
                Text(tr("${game.correctTotal} correct", "${game.correctTotal} goed"), style = MaterialTheme.typography.labelMedium, color = Night.glow)
            }
            ProgressBar(game.index.toFloat() / game.total.coerceAtLeast(1))
        }

        AnimatedContent(game.index, transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(120)) }, label = "q") { _ ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Prompt(q)
                when {
                    q.options.isEmpty() -> TypeAnswer(game, q)
                    q.reverse -> FlagOptions(game, q)
                    else -> NameOptions(game, q)
                }
                Feedback(game, q)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------- vraag ----------

@Composable
private fun Prompt(q: FlagQuestion) {
    NightCard(Modifier.fillMaxWidth()) {
        if (q.reverse) {
            Text(tr("Which flag belongs to", "Welke vlag hoort bij"), style = MaterialTheme.typography.bodyMedium, color = Night.muted)
            Text(q.answer.name(LocalLang.current), style = MaterialTheme.typography.displayMedium, color = Night.text)
        } else {
            Text(
                if (q.options.isEmpty()) tr("Which country's flag is this? Type the name.", "Van welk land is deze vlag? Typ de naam.") else tr("Which country's flag is this?", "Van welk land is deze vlag?"),
                style = MaterialTheme.typography.bodyMedium, color = Night.muted,
            )
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 150.dp), contentAlignment = Alignment.Center) {
                FlagImage(q.answer, height = 150.dp, maxWidth = 290.dp, shadow = true)
            }
        }
    }
}

private enum class OptState { IDLE, RIGHT, WRONG, DIM }

@Composable
private fun optState(game: FlagGame, q: FlagQuestion, c: Country): OptState {
    val a = game.answer ?: return OptState.IDLE
    return when {
        c.code == q.answer.code -> OptState.RIGHT
        a.picked?.code == c.code -> OptState.WRONG
        else -> OptState.DIM
    }
}

@Composable
private fun NameOptions(game: FlagGame, q: FlagQuestion) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c ->
                    OptionBox(optState(game, q, c), Modifier.weight(1f).heightIn(min = 58.dp), { game.pick(c) }) {
                        Text(c.name(LocalLang.current), style = MaterialTheme.typography.titleSmall, color = Night.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FlagOptions(game: FlagGame, q: FlagQuestion) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.options.chunked(if (q.options.size > 4) 3 else 2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c ->
                    val st = optState(game, q, c)
                    OptionBox(st, Modifier.weight(1f).aspectRatio(1.3f), { game.pick(c) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FlagImage(c, height = if (q.options.size > 4) 44.dp else 62.dp, maxWidth = if (q.options.size > 4) 84.dp else 124.dp)
                            if (st == OptState.WRONG || (st == OptState.DIM && game.answer?.correct == false)) {
                                Text(c.name(LocalLang.current), style = MaterialTheme.typography.labelSmall, color = Night.muted, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
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
private fun TypeAnswer(game: FlagGame, q: FlagQuestion) {
    var text by remember(q) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(q) { runCatching { focus.requestFocus() } }
    val done = game.answer != null
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (!done) text = it },
            modifier = Modifier.weight(1f).focusRequester(focus),
            placeholder = { Text(tr("Name of the country", "Naam van het land"), color = Night.faint) },
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
private fun Feedback(game: FlagGame, q: FlagQuestion) {
    val a = game.answer ?: return
    val c = q.answer
    val lang = LocalLang.current
    val color = if (a.correct) Night.glow else Night.danger
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
            Icon(if (a.correct) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = color)
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    a.correct -> tr("Correct!", "Goed!")
                    a.picked != null && q.reverse -> tr("That's the flag of ${a.picked.name(lang)}.", "Dat is de vlag van ${a.picked.name(lang)}.")
                    a.picked != null -> tr("That wasn't ${a.picked.name(lang)}.", "Dat was niet ${a.picked.name(lang)}.")
                    else -> tr("You typed “${a.typed}”.", "Jij typte “${a.typed}”.")
                },
                style = MaterialTheme.typography.titleSmall, color = Night.text,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlagImage(c, height = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                c.name(lang) + (c.capital(lang)?.let { tr(" · capital $it", " · hoofdstad $it") } ?: "") +
                    tr(" · ${formatPopulation(c.population, lang)} people", " · ${formatPopulation(c.population, lang)} inwoners"),
                style = MaterialTheme.typography.bodyMedium, color = Night.muted,
            )
        }
        if (!a.correct) {
            Spacer(Modifier.height(6.dp))
            Row {
                Text(tr("This flag will come back once more at the end.", "Deze vlag komt aan het eind nog een keer terug."), style = MaterialTheme.typography.bodySmall, color = Night.faint, modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                Spacer(Modifier.width(8.dp))
                GlowButton(tr("Next", "Volgende"), { game.next() })
            }
        }
    }
}

// ---------- uitslag ----------

@Composable
private fun Result(game: FlagGame, record: Boolean?, onAgain: () -> Unit, onBack: () -> Unit, onNextLevel: (() -> Unit)?) {
    val pct = game.percent
    val color = when { pct >= Levels.MASTERED -> Night.glow; pct >= Levels.OK -> Night.mid; else -> Night.danger }
    NightCard(Modifier.fillMaxWidth()) {
        Eyebrow(tr("Round complete", "Ronde klaar"))
        Text("$pct%", style = MaterialTheme.typography.displayLarge, color = color)
        Text(
            tr("${game.correctFirstTry} of ${game.firstCount} right first time", "${game.correctFirstTry} van ${game.firstCount} in één keer goed") + when (record) {
                true -> tr(" · new record!", " · nieuw record!")
                else -> ""
            },
            style = MaterialTheme.typography.bodyLarge, color = Night.text,
        )
        Text(
            when {
                pct == 100 -> tr("Flawless. Time for a higher level.", "Foutloos. Tijd voor een hoger niveau.")
                pct >= Levels.MASTERED -> tr("Strong. This cell now counts as mastered.", "Sterk. Dit vakje telt als gekend.")
                pct >= Levels.OK -> tr("Getting there. One more round and you'll pass ${Levels.MASTERED}%.", "Op weg. Nog een ronde en je zit boven de ${Levels.MASTERED}%.")
                else -> tr("Keep practising: the flags you miss come back more often.", "Blijven oefenen: de vlaggen die je mist komen vaker langs.")
            },
            style = MaterialTheme.typography.bodyMedium, color = Night.muted,
        )
        if (game.misses.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Eyebrow(tr("Practise these", "Nog even oefenen"))
            Spacer(Modifier.height(6.dp))
            game.misses.forEach { c ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    FlagImage(c, height = 20.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(c.name(LocalLang.current), style = MaterialTheme.typography.bodyMedium, color = Night.text)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton(tr("New round", "Nieuwe ronde"), onAgain, Modifier.fillMaxWidth())
            if (onNextLevel != null && pct >= Levels.MASTERED) {
                OutlinedButton(onClick = onNextLevel, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Night.gold), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.gold)) {
                    Text(tr("On to", "Naar") + " ${"★".repeat(game.level + 1)}", style = MaterialTheme.typography.labelLarge)
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Night.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.text)) {
                Text(tr("Back to options", "Terug naar de opties"), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
