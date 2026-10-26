package nl.topo.app.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import nl.topo.app.data.online.ScoreSubmission
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.topo.app.data.daily.Challenge
import nl.topo.app.data.daily.DailyChallenges
import nl.topo.app.data.daily.Difficulty
import nl.topo.app.data.geo.WorldMap
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.prefs.DailyState
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.ProgressBar
import nl.topo.app.ui.components.formatPoints
import nl.topo.app.ui.theme.Night
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Tab met de twee uitdagingen van vandaag. */
@Composable
fun DailyScreen(onPlay: (Difficulty) -> Unit) {
    val app = LocalAppContainer.current
    val lang = LocalLang.current
    val world by produceState<WorldMap?>(null) { value = app.geo.world() }
    val day = remember { DailyChallenges.today() }
    val easyState by app.progress.daily(day, Difficulty.EASY.name).collectAsStateWithLifecycle(DailyState())
    val hardState by app.progress.daily(day, Difficulty.HARD.name).collectAsStateWithLifecycle(DailyState())
    var rankingKey by remember { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(tr("Daily challenge", "Dagelijkse uitdaging"), style = MaterialTheme.typography.displayMedium, color = Night.text)
            Text(
                LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(lang.locale)) +
                    " · " + nextIn(lang),
                style = MaterialTheme.typography.bodyMedium, color = Night.muted,
            )
        }
        val w = world
        if (w == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Night.glow) }
            return@Column
        }
        val easy = remember(w, day, lang) { DailyChallenges.forDay(w, day, Difficulty.EASY, lang) }
        val hard = remember(w, day, lang) { DailyChallenges.forDay(w, day, Difficulty.HARD, lang) }
        ChallengeCard(easy, easyState, Night.glow, Icons.Rounded.Lightbulb,
            tr("Easy", "Makkelijk"), tr("5 lives · hints · 10 points per country", "5 levens · hints · 10 punten per land")) { onPlay(Difficulty.EASY) }
        ChallengeCard(hard, hardState, Night.ember, Icons.Rounded.Whatshot,
            tr("Hard", "Moeilijk"), tr("3 lives · no hints · 25 points per country", "3 levens · geen hints · 25 punten per land")) { onPlay(Difficulty.HARD) }
        // Uitgespeeld maar (nog) niet in de ranglijst, bijv. gespeeld vóór de ranglijst bestond? Alsnog insturen.
        LaunchedEffect(easyState.finished, hardState.finished) {
            for ((ch, st) in listOf(easy to easyState, hard to hardState)) {
                if (!st.finished || app.leaderboard.hasResult(day, ch.difficulty.name)) continue
                app.leaderboard.submit(
                    ScoreSubmission(
                        playerId = app.profile.playerId(), nickname = app.profile.currentNickname(),
                        day = day, difficulty = ch.difficulty.name, points = st.points.coerceAtLeast(0),
                        found = st.found.count { it in ch.targets }, total = ch.targets.size,
                        mistakes = st.mistakes, hints = st.hints, timeMs = st.timeMs,
                        completed = ch.targets.all { it in st.found }, submittedAt = System.currentTimeMillis(),
                    ),
                )
                rankingKey++
            }
        }
        RankingCard(day = day, refreshKey = Triple(easyState, hardState, rankingKey))
        Text(
            tr(
                "Everyone gets the same challenges today. The map shows no borders: countries appear as you name them.",
                "Iedereen krijgt vandaag dezelfde uitdagingen. De kaart toont geen grenzen: landen verschijnen als je ze noemt.",
            ),
            style = MaterialTheme.typography.bodySmall, color = Night.faint, modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private fun nextIn(lang: nl.topo.app.data.i18n.Lang): String {
    val now = LocalDateTime.now()
    val mins = java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMinutes()
    return lang.t("new ones in ${mins / 60}h ${mins % 60}m", "nieuwe over ${mins / 60}u ${mins % 60}m")
}

@Composable
private fun ChallengeCard(c: Challenge, s: DailyState, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, rules: String, onPlay: () -> Unit) {
    val lang = LocalLang.current
    val total = c.targets.size
    val found = s.found.count { it in c.targets }
    NightCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = Night.text)
            }
            Spacer(Modifier.weight(1f))
            if (s.finished) {
                Icon(Icons.Rounded.CheckCircle, null, tint = if (found == total) Night.glow else Night.muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("+${formatPoints(s.points, lang)}", style = MaterialTheme.typography.labelMedium, color = Night.gold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(c.title.text, style = MaterialTheme.typography.headlineSmall, color = Night.text)
        Text(tr("$total countries", "$total landen") + " · " + rules, style = MaterialTheme.typography.bodySmall, color = Night.muted)
        Spacer(Modifier.height(12.dp))
        ProgressBar(if (total == 0) 0f else found.toFloat() / total, color = color)
        Spacer(Modifier.height(6.dp))
        Eyebrow(
            when {
                s.finished && found == total -> tr("Completed · all $total found", "Voltooid · alle $total gevonden")
                s.lost -> tr("Out of lives · $found of $total found", "Geen levens meer · $found van $total gevonden")
                s.finished -> tr("Finished · $found of $total found", "Afgerond · $found van $total gevonden")
                found > 0 -> tr("$found of $total found", "$found van $total gevonden")
                else -> tr("Not started", "Nog niet begonnen")
            },
        )
        Spacer(Modifier.height(12.dp))
        GlowButton(
            when {
                s.finished -> tr("View result", "Bekijk uitslag")
                found > 0 -> tr("Continue", "Verder")
                else -> tr("Start", "Begin")
            },
            onPlay, Modifier.fillMaxWidth(),
        )
    }
}
