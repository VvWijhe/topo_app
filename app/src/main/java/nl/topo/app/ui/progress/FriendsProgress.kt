package nl.topo.app.ui.progress

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.topo.app.data.content.Content
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.text
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.online.PlayerStats
import nl.topo.app.data.prefs.Progress
import nl.topo.app.data.prefs.Ranks
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.RankBar
import nl.topo.app.ui.components.formatPoints
import nl.topo.app.ui.daily.FriendsDialog
import nl.topo.app.ui.theme.Night

/** Vrienden en jijzelf, op punten: rang, punten en reeks; tik voor details. */
@Composable
fun FriendsProgressCard(myProgress: Progress) {
    val app = LocalAppContainer.current
    if (!app.leaderboard.isOnline) return
    val lang = LocalLang.current
    val friends by app.friends.friends.collectAsStateWithLifecycle(emptyList())
    val myName by app.profile.nickname.collectAsStateWithLifecycle("")
    val stats by produceState<Map<String, PlayerStats>?>(null, friends) {
        value = app.leaderboard.stats(friends.map { it.playerId })
    }
    var manage by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<PlayerStats?>(null) }

    val regions = Content.regions.filter { it.available }
    val me = PlayerStats(
        playerId = "me", nickname = myName.ifBlank { tr("You", "Jij") }, points = myProgress.points,
        streak = myProgress.streak, rounds = myProgress.rounds, mastered = regions.sumOf { myProgress.mastered(it) },
        regions = regions.associate { it.id.name to myProgress.mastered(it) }, updatedAt = System.currentTimeMillis(),
    )

    NightCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Group, null, tint = Night.glow, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Friends", "Vrienden"), style = MaterialTheme.typography.titleMedium, color = Night.text, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(50)).clickable { manage = true }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PersonAdd, null, tint = Night.glow, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(tr("Add", "Toevoegen"), style = MaterialTheme.typography.labelMedium, color = Night.glow)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (friends.isEmpty()) {
            Text(
                tr("Add friends with their friend code to compare points and progress.", "Voeg vrienden toe met hun vriendcode om punten en voortgang te vergelijken."),
                style = MaterialTheme.typography.bodySmall, color = Night.muted,
            )
        } else {
            val loaded = stats
            val rows = buildList {
                add(me to true)
                friends.forEach { f ->
                    val st = loaded?.get(f.playerId)
                    add((st ?: PlayerStats(f.playerId, f.nickname, -1, 0, 0, 0, emptyMap(), 0L)).let { it.copy(nickname = it.nickname.ifBlank { f.nickname }) } to false)
                }
            }.sortedByDescending { it.first.points }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEachIndexed { i, (st, isMe) ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                            .background(if (isMe) Night.glow.copy(alpha = 0.10f) else Night.surfaceHigh.copy(alpha = 0.5f))
                            .clickable(enabled = st.points >= 0) { open = st }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = Night.faint, modifier = Modifier.width(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text(st.nickname.ifBlank { tr("Anonymous", "Anoniem") } + if (isMe) tr(" (you)", " (jij)") else "", style = MaterialTheme.typography.titleSmall, color = Night.text)
                            Text(
                                if (st.points < 0) (if (loaded == null) tr("Loading…", "Laden…") else tr("Hasn't shared progress yet", "Nog geen voortgang gedeeld"))
                                else Ranks.of(st.points).name.text + " · " + tr("${st.mastered} mastered", "${st.mastered} gekend"),
                                style = MaterialTheme.typography.bodySmall, color = Night.muted,
                            )
                        }
                        if (st.points >= 0) {
                            if (st.streak > 0) {
                                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Night.ember, modifier = Modifier.size(14.dp))
                                Text("${st.streak}", style = MaterialTheme.typography.labelMedium, color = Night.ember)
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(formatPoints(st.points, lang), style = MaterialTheme.typography.labelLarge, color = Night.gold)
                        }
                    }
                }
            }
        }
    }
    if (manage) FriendsDialog(onDismiss = { manage = false })
    open?.let { st -> FriendDetail(st, me) { open = null } }
}

/** Details van één speler, naast die van jou. */
@Composable
private fun FriendDetail(st: PlayerStats, me: PlayerStats, onDismiss: () -> Unit) {
    val regions = Content.regions.filter { it.available }
    val isMe = st.playerId == "me"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Night.surfaceHigh,
        title = { Text(st.nickname, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RankBar(st.points)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Stat("${st.streak}", tr("days in a row", "dagen op rij"))
                    Stat("${st.rounds}", tr("rounds", "rondes"))
                    Stat("${st.mastered}", tr("mastered", "gekend"))
                }
                Eyebrow(if (isMe) tr("Per area", "Per gebied") else tr("Per area · you in green", "Per gebied · jij in groen"))
                regions.forEach { r ->
                    val n = st.regions[r.id.name] ?: 0
                    val mine = me.regions[r.id.name] ?: 0
                    val total = r.cellCount.coerceAtLeast(1)
                    Column {
                        Row {
                            Text(r.name.text, style = MaterialTheme.typography.bodySmall, color = Night.text, modifier = Modifier.weight(1f))
                            Text("$n / ${r.cellCount}", style = MaterialTheme.typography.bodySmall, color = Night.muted)
                        }
                        Spacer(Modifier.height(3.dp))
                        Bar(n.toFloat() / total, Night.gold, Night.outline, 5.dp)
                        if (!isMe) {
                            Spacer(Modifier.height(2.dp))
                            Bar(mine.toFloat() / total, Night.glow, Night.outline.copy(alpha = 0.4f), 3.dp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close", "Sluiten"), color = Night.glow) } },
    )
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Night.text)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Night.muted)
    }
}


/** Eenvoudige voortgangsbalk (zonder het eindpuntje en de opening van de Material-balk). */
@Composable
private fun Bar(fraction: Float, color: androidx.compose.ui.graphics.Color, track: androidx.compose.ui.graphics.Color, height: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(50)).background(color))
    }
}
