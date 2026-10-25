package nl.topo.app.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.topo.app.data.daily.Difficulty
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.online.LeaderboardEntry
import nl.topo.app.data.online.Period
import nl.topo.app.data.online.PlayerProfile
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.formatPoints
import nl.topo.app.ui.theme.Night

/** mm:ss (of u:mm:ss). */
fun formatTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

/**
 * Ranglijst van de dagelijkse uitdaging: vandaag (per moeilijkheid) of deze week (totaal).
 * Werkt nu offline (alleen jouw resultaten); met een online ranglijst staan alle spelers erin.
 */
@Composable
fun RankingCard(day: Long, difficulty: Difficulty? = null, refreshKey: Any? = null, compact: Boolean = false) {
    val app = LocalAppContainer.current
    val lang = LocalLang.current
    var period by rememberSaveable { mutableStateOf(Period.DAY) }
    var diff by rememberSaveable { mutableStateOf(difficulty ?: Difficulty.EASY) }
    val nickname by app.profile.nickname.collectAsStateWithLifecycle("")
    val pending by app.leaderboard.pending.collectAsStateWithLifecycle(emptyList())
    val entries by produceState<List<LeaderboardEntry>?>(null, day, diff, period, refreshKey, nickname) {
        value = app.leaderboard.ranking(day, diff.name, period)
    }
    var askName by remember { mutableStateOf(false) }

    NightCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = Night.gold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Ranking", "Ranglijst"), style = MaterialTheme.typography.titleMedium, color = Night.text, modifier = Modifier.weight(1f))
            if (!app.leaderboard.isOnline) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Night.surfaceHigh).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CloudOff, null, tint = Night.muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Offline", "Offline"), style = MaterialTheme.typography.labelSmall, color = Night.muted)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(tr("Today", "Vandaag"), period == Period.DAY) { period = Period.DAY }
            Pill(tr("This week", "Deze week"), period == Period.WEEK) { period = Period.WEEK }
            if (!compact && period == Period.DAY) {
                Spacer(Modifier.weight(1f))
                Pill(tr("Easy", "Makkelijk"), diff == Difficulty.EASY, Night.glow) { diff = Difficulty.EASY }
                Pill(tr("Hard", "Moeilijk"), diff == Difficulty.HARD, Night.ember) { diff = Difficulty.HARD }
            }
        }
        Spacer(Modifier.height(10.dp))
        val list = entries
        when {
            list == null -> Unit
            list.isEmpty() -> Text(
                if (period == Period.DAY) tr("No result yet. Finish today's challenge to get on the list.", "Nog geen uitslag. Rond de uitdaging van vandaag af om op de lijst te komen.")
                else tr("No results this week yet.", "Deze week nog geen uitslagen."),
                style = MaterialTheme.typography.bodySmall, color = Night.muted,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                list.forEachIndexed { i, e -> EntryRow(i + 1, e, nickname) }
            }
        }
        if (nickname.isBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Choose a nickname →", "Kies een bijnaam →"), style = MaterialTheme.typography.labelLarge, color = Night.glow,
                modifier = Modifier.clickable { askName = true }.padding(vertical = 4.dp),
            )
        }
        if (!app.leaderboard.isOnline) {
            Spacer(Modifier.height(8.dp))
            Text(
                tr(
                    "The online ranking isn't switched on yet. Your results are saved" + (if (pending.isNotEmpty()) " (${pending.size} waiting)" else "") + " and will be sent automatically once it is.",
                    "De online ranglijst staat nog niet aan. Je uitslagen worden bewaard" + (if (pending.isNotEmpty()) " (${pending.size} klaar)" else "") + " en straks automatisch verstuurd.",
                ),
                style = MaterialTheme.typography.bodySmall, color = Night.faint,
            )
        }
    }
    if (askName) NicknameDialog(onDismiss = { askName = false })
}

@Composable
private fun EntryRow(rank: Int, e: LeaderboardEntry, myName: String) {
    val lang = LocalLang.current
    val medal = when (rank) { 1 -> Night.gold; 2 -> Color(0xFFC9D3DD); 3 -> Color(0xFFD49A6A); else -> Night.faint }
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .background(if (e.isMe) Night.glow.copy(alpha = 0.10f) else Night.surfaceHigh.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp).clip(RoundedCornerShape(50)).border(1.5.dp, medal, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            Text("$rank", style = MaterialTheme.typography.labelSmall, color = Night.text)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (e.nickname.ifBlank { myName }.ifBlank { tr("You", "Jij") }) + if (e.isMe) tr(" (you)", " (jij)") else "",
                style = MaterialTheme.typography.titleSmall, color = Night.text,
            )
            Text(tr("${e.found}/${e.total} found · ${formatTime(e.timeMs)}", "${e.found}/${e.total} gevonden · ${formatTime(e.timeMs)}"),
                style = MaterialTheme.typography.bodySmall, color = Night.muted)
        }
        Text(formatPoints(e.points, lang), style = MaterialTheme.typography.labelLarge, color = Night.gold)
    }
}

@Composable
private fun Pill(text: String, active: Boolean, color: Color = Night.glow, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (active) color.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, if (active) color else Night.outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = if (active) Night.text else Night.muted) }
}

/** Bijnaam kiezen of wijzigen (zichtbaar in de ranglijst). */
@Composable
fun NicknameDialog(onDismiss: () -> Unit) {
    val app = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val current by app.profile.nickname.collectAsStateWithLifecycle("")
    var text by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Night.surfaceHigh,
        title = { Text(tr("Your nickname", "Je bijnaam"), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Shown in the ranking. No real name needed.", "Te zien in de ranglijst. Je echte naam is niet nodig."), color = Night.muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = text, onValueChange = { if (it.length <= PlayerProfile.MAX_NAME) text = it },
                    singleLine = true, placeholder = { Text(tr("e.g. Mapmaster", "bijv. Kaartkoning"), color = Night.faint) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Night.glow, unfocusedBorderColor = Night.outline,
                        cursorColor = Night.glow, focusedTextColor = Night.text, unfocusedTextColor = Night.text,
                    ),
                )
                Eyebrow("${text.trim().length}/${PlayerProfile.MAX_NAME}")
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { scope.launch { app.profile.setNickname(text) }; onDismiss() }) {
                Text(tr("Save", "Opslaan"), color = Night.glow)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Annuleren"), color = Night.text) } },
    )
}
