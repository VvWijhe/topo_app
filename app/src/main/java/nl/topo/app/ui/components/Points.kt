package nl.topo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import nl.topo.app.data.i18n.Lang
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.prefs.Award
import nl.topo.app.data.prefs.Ranks
import nl.topo.app.data.prefs.RoundOutcome
import nl.topo.app.ui.theme.Night
import java.text.NumberFormat

fun formatPoints(n: Int, lang: Lang): String = NumberFormat.getIntegerInstance(lang.locale).format(n)

/** Tekst bij een bonus. */
fun awardLabel(a: Award, lang: Lang): String = when (a.kind) {
    Award.Kind.ANSWERS -> lang.t("${a.n} correct answers", "${a.n} goede antwoorden")
    Award.Kind.PERFECT -> lang.t("Perfect round", "Foutloze ronde")
    Award.Kind.MASTERED -> lang.t("Level mastered for the first time", "Niveau voor het eerst gehaald")
    Award.Kind.RECORD -> lang.t("New personal record", "Nieuw persoonlijk record")
    Award.Kind.CASCADE -> lang.t("${"★".repeat(a.n)} also completed", "${"★".repeat(a.n)} ook gehaald")
    Award.Kind.CHALLENGE_FOUND -> lang.t("${a.n} countries found", "${a.n} landen gevonden")
    Award.Kind.CHALLENGE_DONE -> lang.t("Challenge complete", "Uitdaging voltooid")
    Award.Kind.HINTS -> lang.t("${a.n} hints used", "${a.n} hints gebruikt")
    Award.Kind.MISTAKES -> lang.t("${a.n} wrong answers", "${a.n} foute antwoorden")
    Award.Kind.DAILY -> if (a.n <= 1) lang.t("First round today", "Eerste ronde vandaag")
        else lang.t("Daily bonus · day ${a.n} of your streak", "Dagbonus · dag ${a.n} van je reeks")
}

/** Punten en rang, klein: voor de kop van het startscherm. */
@Composable
fun PointsChip(points: Int, modifier: Modifier = Modifier) {
    val lang = LocalLang.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Night.gold.copy(alpha = 0.12f))
            .border(1.dp, Night.gold.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Star, null, tint = Night.gold, modifier = Modifier.size(18.dp))
        Text(formatPoints(points, lang), style = MaterialTheme.typography.labelMedium, color = Night.text)
    }
}

/** Rang met balkje naar de volgende rang. */
@Composable
fun RankBar(points: Int, modifier: Modifier = Modifier) {
    val lang = LocalLang.current
    val rank = Ranks.of(points)
    val next = Ranks.next(points)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = Night.gold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(rank.name.text, style = MaterialTheme.typography.titleMedium, color = Night.text, modifier = Modifier.weight(1f))
            Text(tr("${formatPoints(points, lang)} pts", "${formatPoints(points, lang)} ptn"), style = MaterialTheme.typography.labelMedium, color = Night.gold)
        }
        ProgressBar(Ranks.fraction(points), color = Night.gold)
        Text(
            if (next == null) tr("Highest rank reached", "Hoogste rang bereikt")
            else tr("${formatPoints(next.min - points, lang)} to ${next.name.en}", "Nog ${formatPoints(next.min - points, lang)} tot ${next.name.nl}"),
            style = MaterialTheme.typography.bodySmall, color = Night.muted,
        )
    }
}

/** Uitslag van de punten na een ronde, met de bonussen en de rang. */
@Composable
fun PointsPanel(outcome: RoundOutcome, modifier: Modifier = Modifier) {
    val lang = LocalLang.current
    val shown = remember(outcome) { Animatable(0f) }
    LaunchedEffect(outcome) { shown.animateTo(outcome.gained.toFloat(), tween(900, easing = FastOutSlowInEasing)) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Night.gold.copy(alpha = 0.08f))
            .border(1.dp, Night.gold.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, tint = Night.gold, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("+${formatPoints(shown.value.toInt(), lang)}", style = MaterialTheme.typography.headlineMedium, color = Night.gold)
            Spacer(Modifier.width(6.dp))
            Text(tr("points", "punten"), style = MaterialTheme.typography.bodyMedium, color = Night.muted)
        }
        outcome.awards.forEach { a ->
            Row {
                Text(awardLabel(a, lang), style = MaterialTheme.typography.bodySmall, color = Night.text, modifier = Modifier.weight(1f))
                Text((if (a.points >= 0) "+" else "−") + formatPoints(kotlin.math.abs(a.points), lang), style = MaterialTheme.typography.labelMedium, color = if (a.points >= 0) Night.gold else Night.muted)
            }
        }
        if (outcome.cascadedLevels.isNotEmpty()) {
            Text(
                tr("100% on the whole level, so the easier levels count as completed too.", "100% op het hele niveau, dus de makkelijkere niveaus tellen ook als gehaald."),
                style = MaterialTheme.typography.bodySmall, color = Night.glow,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (outcome.rankUp) {
            Text(tr("New rank: ", "Nieuwe rang: ") + Ranks.of(outcome.pointsAfter).name.text + "!", style = MaterialTheme.typography.titleSmall, color = Night.gold)
        }
        RankBar(outcome.pointsAfter)
    }
}
