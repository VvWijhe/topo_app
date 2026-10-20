package nl.topo.app.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.topo.app.data.content.Content
import nl.topo.app.data.content.Levels
import nl.topo.app.data.content.Region
import nl.topo.app.data.content.RegionId
import nl.topo.app.data.content.RegionKind
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.prefs.RoundLength
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.quiz.FlagGame
import nl.topo.app.data.prefs.Settings
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.StarsText
import nl.topo.app.ui.theme.Night

/** Kiest het juiste quizscherm bij een onderwerp. */
@Composable
fun QuizScreen(regionId: RegionId, topicId: String, level: Int, onBack: () -> Unit) {
    val region = Content.region(regionId)
    if (topicId == "vlag" && region.kind != RegionKind.COUNTRY) {
        FlagQuizRoute(region, level, onBack)
    } else {
        PlaceholderQuiz(regionId, topicId, level, onBack)
    }
}

@Composable
private fun FlagQuizRoute(region: Region, startLevel: Int, onBack: () -> Unit) {
    val app = LocalAppContainer.current
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val world by produceState<WorldMap?>(null) { value = app.geo.world() }
    var level by rememberSaveable { mutableIntStateOf(startLevel) }
    var record by remember { mutableStateOf<Boolean?>(null) }
    val w = world
    if (w == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Night.glow) }
        return
    }
    val roundLength = if (settings.roundLength == RoundLength.ALL) null else 10
    val game = remember(level, w, roundLength) { FlagGame(w, region.continent, level, roundLength) }
    // Uitslag opslaan zodra de ronde klaar is.
    LaunchedEffect(game, game.finished) {
        if (game.finished) record = app.progress.recordRound(region.id, "vlag", level, game.percent)
        else record = null
    }
    FlagQuizScreen(
        game = game,
        region = region,
        haptics = settings.haptics,
        record = record,
        onBack = onBack,
        onNextLevel = if (level < 4) ({ level += 1 }) else null,
    )
}

/**
 * Tijdelijk scherm voor onderwerpen die nog niet speelbaar zijn; met testknoppen
 * om een ronde na te bootsen, zodat de voortgang en de vakjes al werken.
 */
@Composable
private fun PlaceholderQuiz(regionId: RegionId, topicId: String, level: Int, onBack: () -> Unit) {
    val app = LocalAppContainer.current
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val region = Content.region(regionId)
    val topic = Content.topic(regionId, topicId)
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    val lang = LocalLang.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back", "Terug"), tint = Night.text) }
        Column(Modifier.padding(horizontal = 4.dp)) {
            Eyebrow(region.name.text)
            Text(topic?.name?.text ?: topicId, style = MaterialTheme.typography.displayMedium, color = Night.text)
            Row { StarsText(level, size = 16) }
            Text(
                "${Levels.all[level - 1].title.text} · ${region.levelScope[level - 1].text} · ${Levels.all[level - 1].how.text}",
                style = MaterialTheme.typography.bodyMedium, color = Night.muted,
            )
        }

        NightCard(Modifier.fillMaxWidth()) {
            Text(tr("This topic is coming", "Dit onderwerp komt nog"), style = MaterialTheme.typography.headlineSmall, color = Night.text)
            Spacer(Modifier.height(4.dp))
            Text(
                tr("Playable now: Flag, for the world and every continent. The other topics will follow.", "Speelbaar nu: Vlag, voor de wereld en elk werelddeel. De andere onderwerpen volgen."),
                style = MaterialTheme.typography.bodyMedium, color = Night.muted,
            )
        }

        NightCard(Modifier.fillMaxWidth()) {
            Eyebrow(tr("For testing only", "Alleen om te testen"), color = Night.ember)
            Spacer(Modifier.height(6.dp))
            Text(tr("Pretend you played a round, and see what happens to the cells and your progress.", "Doe alsof je een ronde speelde, en kijk wat er gebeurt met de vakjes en je voortgang."), style = MaterialTheme.typography.bodySmall, color = Night.muted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(45, 75, 100).forEach { pct ->
                    OutlinedButton(onClick = {
                        scope.launch {
                            val record = app.progress.recordRound(regionId, topicId, level, pct)
                            message = if (record) lang.t("$pct% saved — new record", "$pct% opgeslagen — nieuw record") else lang.t("$pct% played; your record stands", "$pct% gespeeld; je record blijft staan")
                        }
                    }) { Text("$pct%", color = Night.text) }
                }
            }
            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.labelMedium, color = Night.glow)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
