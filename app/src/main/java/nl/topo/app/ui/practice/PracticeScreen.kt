package nl.topo.app.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import nl.topo.app.data.daily.Challenge
import nl.topo.app.data.daily.Difficulty
import nl.topo.app.data.i18n.Txt
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.theme.Night

private data class PracticeType(val kind: Challenge.Kind, val icon: ImageVector, val title: Txt, val about: Txt, val levels: Boolean = true)

private val types = listOf(
    PracticeType(Challenge.Kind.NEIGHBOURS, Icons.Rounded.Hub, Txt("Neighbours", "Buurlanden"), Txt("Name every country that borders a given country.", "Noem alle buurlanden van een land.")),
    PracticeType(Challenge.Kind.LETTER, Icons.Rounded.SortByAlpha, Txt("Starting letter", "Beginletter"), Txt("Name every country that starts with a letter.", "Noem alle landen die met een letter beginnen.")),
    PracticeType(Challenge.Kind.TOP, Icons.Rounded.FormatListNumbered, Txt("Top lists", "Toplijsten"), Txt("The biggest countries of a continent, by people or by area.", "De grootste landen van een werelddeel, naar inwoners of oppervlakte.")),
    PracticeType(Challenge.Kind.LINE, Icons.Rounded.Public, Txt("Lines on the globe", "Lijnen over de wereld"), Txt("Every country on the equator, a tropic, the Arctic Circle or Greenwich.", "Alle landen op de evenaar, een keerkring, de poolcirkel of Greenwich.")),
    PracticeType(Challenge.Kind.ROUTE, Icons.Rounded.Route, Txt("Route", "Route"), Txt("The shortest route over land between two countries. Easy: 3–5 countries in between. Hard: 6–9.", "De kortste route over land tussen twee landen. Makkelijk: 3–5 landen ertussen. Moeilijk: 6–9."),
    PracticeType(Challenge.Kind.MYSTERY, Icons.Rounded.TravelExplore, Txt("Mystery country", "Mysterieland"), Txt("Guess the country from distance and direction. Hard: no hints.", "Raad het land aan de hand van afstand en richting. Moeilijk: geen hints.")),
)

/** Tabblad Puzzels: elk type dagelijkse uitdaging, zo vaak als je wilt (halve punten). */
@Composable
fun PracticeScreen(onPlay: (Challenge.Kind, Difficulty) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(tr("Puzzles", "Puzzels"), style = MaterialTheme.typography.displayMedium, color = Night.text)
                Text(
                    tr("Every kind of daily challenge, as often as you like. Half points, and it keeps your streak going.",
                        "Elk soort dagelijkse uitdaging, zo vaak als je wilt. Halve punten, en het telt voor je reeks."),
                    style = MaterialTheme.typography.bodyMedium, color = Night.muted,
                )
            }
        }
        items(types, key = { it.kind.name }) { t ->
            NightCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(t.icon, null, tint = Night.gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.title.text, style = MaterialTheme.typography.titleMedium, color = Night.text)
                }
                Spacer(Modifier.height(4.dp))
                Text(t.about.text, style = MaterialTheme.typography.bodySmall, color = Night.muted)
                Spacer(Modifier.height(12.dp))
                if (t.levels) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlowButton(tr("Easy", "Makkelijk"), { onPlay(t.kind, Difficulty.EASY) }, Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { onPlay(t.kind, Difficulty.HARD) }, modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Night.ember), colors = ButtonDefaults.outlinedButtonColors(contentColor = Night.ember),
                        ) { Text(tr("Hard", "Moeilijk"), style = MaterialTheme.typography.labelLarge) }
                    }
                } else {
                    GlowButton(tr("Start", "Begin"), { onPlay(t.kind, Difficulty.EASY) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
