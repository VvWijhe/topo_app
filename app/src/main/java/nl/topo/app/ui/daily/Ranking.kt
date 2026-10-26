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
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import nl.topo.app.data.online.Friend
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
    val problem by app.leaderboard.problem.collectAsStateWithLifecycle(null)
    var friendsOnly by rememberSaveable { mutableStateOf(false) }
    val friends by app.friends.friends.collectAsStateWithLifecycle(emptyList())
    val only = if (friendsOnly) friends.map { it.playerId }.toSet() else null
    val entries by produceState<List<LeaderboardEntry>?>(null, day, diff, period, refreshKey, nickname, only) {
        value = app.leaderboard.ranking(day, diff.name, period, only)
    }
    var askName by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }

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
        if (app.leaderboard.isOnline) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(tr("Everyone", "Iedereen"), !friendsOnly) { friendsOnly = false }
                Pill(tr("Friends", "Vrienden") + if (friends.isNotEmpty()) " ${friends.size}" else "", friendsOnly) {
                    if (friends.isEmpty()) showFriends = true else friendsOnly = true
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).clickable { showFriends = true }.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PersonAdd, null, tint = Night.glow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Add", "Toevoegen"), style = MaterialTheme.typography.labelMedium, color = Night.glow)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val list = entries
        when {
            list == null -> Unit
            friendsOnly && list.none { !it.isMe } -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("None of your friends has a result here yet.", "Nog geen van je vrienden heeft hier een uitslag."), style = MaterialTheme.typography.bodySmall, color = Night.muted)
                list.forEachIndexed { i, e -> EntryRow(i + 1, e, nickname) }
            }
            list.isEmpty() -> Text(
                if (period == Period.DAY) tr("No result yet. Finish today's challenge to get on the list.", "Nog geen uitslag. Rond de uitdaging van vandaag af om op de lijst te komen.")
                else tr("No results this week yet.", "Deze week nog geen uitslagen."),
                style = MaterialTheme.typography.bodySmall, color = Night.muted,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                list.forEachIndexed { i, e -> EntryRow(i + 1, e, nickname) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { askName = true }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nickname.isBlank()) {
                Text(tr("Choose a nickname →", "Kies een bijnaam →"), style = MaterialTheme.typography.labelLarge, color = Night.glow)
            } else {
                Text(tr("Playing as ", "Je speelt als "), style = MaterialTheme.typography.bodySmall, color = Night.muted)
                Text(nickname, style = MaterialTheme.typography.labelLarge, color = Night.text)
                Spacer(Modifier.width(8.dp))
                Text(tr("Change", "Wijzigen"), style = MaterialTheme.typography.labelLarge, color = Night.glow)
            }
        }
        if (app.leaderboard.isOnline && pending.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                tr("${pending.size} result(s) not sent yet", "${pending.size} uitslag(en) nog niet verstuurd") + (problem?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = Night.ember,
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
    if (showFriends) FriendsDialog(onDismiss = { showFriends = false })
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
                (if (e.isMe) e.nickname.ifBlank { myName }.ifBlank { tr("You", "Jij") } else e.nickname.ifBlank { tr("Anonymous", "Anoniem") }) + if (e.isMe) tr(" (you)", " (jij)") else "",
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
            TextButton(enabled = text.isNotBlank(), onClick = { scope.launch { app.profile.setNickname(text); app.scope.launch { app.leaderboard.syncNickname() }; onDismiss() } }) {
                Text(tr("Save", "Opslaan"), color = Night.glow)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Annuleren"), color = Night.text) } },
    )
}


/** Vrienden: je eigen code delen, een code invoeren, en je lijst beheren. */
@Composable
fun FriendsDialog(onDismiss: () -> Unit) {
    val app = LocalAppContainer.current
    val lang = LocalLang.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val friends by app.friends.friends.collectAsStateWithLifecycle(emptyList())
    val myCode by produceState<String?>(null) { value = app.leaderboard.myFriendCode() ?: "" }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun add() {
        if (busy) return
        busy = true; message = null
        scope.launch {
            val r = app.leaderboard.findFriend(input)
            r.onSuccess { f ->
                when {
                    f.code == myCode -> message = lang.t("That's your own code.", "Dat is je eigen code.")
                    friends.any { it.playerId == f.playerId } -> message = lang.t("Already in your list.", "Staat al in je lijst.")
                    else -> { app.friends.add(f); input = ""; message = lang.t("Added ${f.nickname.ifBlank { "your friend" }}!", "${f.nickname.ifBlank { "Je vriend" }} toegevoegd!") }
                }
            }.onFailure {
                message = if (it is NoSuchElementException || it is IllegalArgumentException) lang.t("No player with that code.", "Geen speler met die code.")
                else lang.t("Couldn't reach the server. Try again.", "Geen verbinding. Probeer het nog eens.")
            }
            busy = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Night.surfaceHigh,
        title = { Text(tr("Friends", "Vrienden"), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow(tr("Your friend code", "Jouw vriendcode"))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (val c = myCode) {
                        null -> CircularProgressIndicator(Modifier.size(22.dp), color = Night.glow, strokeWidth = 2.dp)
                        "" -> Text(tr("Not available right now (no internet?)", "Nu niet beschikbaar (geen internet?)"), color = Night.muted, style = MaterialTheme.typography.bodySmall)
                        else -> {
                            Text(c, style = MaterialTheme.typography.headlineMedium, color = Night.gold, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, lang.t(
                                        "Add me as a friend in Lodestar: $c",
                                        "Voeg me toe als vriend in Lodestar: $c",
                                    ))
                                }
                                context.startActivity(android.content.Intent.createChooser(send, null))
                            }) { Icon(Icons.Rounded.Share, tr("Share", "Delen"), tint = Night.glow) }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Eyebrow(tr("Add a friend", "Vriend toevoegen"))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { if (it.length <= 8) input = it.uppercase() },
                        singleLine = true, placeholder = { Text(tr("Their code", "Hun code"), color = Night.faint) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { add() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Night.glow, unfocusedBorderColor = Night.outline,
                            cursorColor = Night.glow, focusedTextColor = Night.text, unfocusedTextColor = Night.text,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(enabled = input.trim().length >= 6 && !busy, onClick = { add() }) { Text(tr("Add", "Toevoegen"), color = Night.glow) }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Night.muted) }
                if (friends.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Eyebrow(tr("Your friends", "Je vrienden"))
                    friends.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(f.nickname.ifBlank { tr("Anonymous", "Anoniem") }, style = MaterialTheme.typography.titleSmall, color = Night.text)
                                Text(f.code, style = MaterialTheme.typography.bodySmall, color = Night.faint)
                            }
                            IconButton(onClick = { scope.launch { app.friends.remove(f.playerId) } }) {
                                Icon(Icons.Rounded.Close, tr("Remove", "Verwijderen"), tint = Night.muted)
                            }
                        }
                    }
                }
                Text(
                    tr("You see a friend once you've added their code; they see you when they add yours.", "Je ziet een vriend in je lijst zodra je zijn code hebt toegevoegd; hij ziet jou als hij jouw code toevoegt."),
                    style = MaterialTheme.typography.bodySmall, color = Night.faint,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Done", "Klaar"), color = Night.glow) } },
    )
}
