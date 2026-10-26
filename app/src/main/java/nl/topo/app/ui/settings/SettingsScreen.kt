package nl.topo.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.topo.app.data.i18n.Lang
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.prefs.RoundLength
import nl.topo.app.data.prefs.Settings
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.daily.NicknameDialog
import nl.topo.app.ui.theme.Night

@Composable
fun SettingsScreen() {
    val app = LocalAppContainer.current
    val s by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    var confirmReset by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var confirmDeleteOnline by remember { mutableStateOf(false) }
    var onlineDeleted by remember { mutableStateOf(false) }
    val nickname by app.profile.nickname.collectAsStateWithLifecycle("")
    val pending by app.leaderboard.pending.collectAsStateWithLifecycle(emptyList())
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(tr("Settings", "Instellingen"), style = MaterialTheme.typography.displayMedium, color = Night.text)
        }

        Section(tr("Language", "Taal")) {
            SettingRow(Icons.Rounded.Translate, tr("App language", "Taal van de app"), tr("Menus, country names and capitals", "Menu's, landnamen en hoofdsteden")) {
                Segmented(Lang.entries.map { it.label }, s.language.ordinal) { i ->
                    scope.launch { app.settings.setLanguage(Lang.entries[i]) }
                }
            }
        }

        Section(tr("Playing", "Spelen")) {
            SettingRow(Icons.Rounded.FormatListNumbered, tr("Round length", "Rondelengte"), tr("10 questions, or every item of the level", "10 vragen, of alles van het niveau")) {
                Segmented(listOf(tr("10 questions", "10 vragen"), tr("Whole level", "Hele niveau")), s.roundLength.ordinal) { i ->
                    scope.launch { app.settings.setRoundLength(RoundLength.entries[i]) }
                }
            }
            Divider()
            SwitchRow(Icons.Rounded.Contrast, tr("High-contrast map", "Hoog contrast kaart"), tr("Brighter borders, easier to see outdoors", "Helderdere grenzen, beter te zien in daglicht"), s.highContrast) {
                scope.launch { app.settings.setHighContrast(it) }
            }
            Divider()
            SwitchRow(Icons.Rounded.Spellcheck, tr("Strict spelling", "Strikte spelling"), tr("Typed answers must be spelled exactly, accents included", "Getypte antwoorden moeten precies goed, met accenten"), s.strictTyping) {
                scope.launch { app.settings.setStrictTyping(it) }
            }
            Divider()
            SwitchRow(Icons.Rounded.TextFields, tr("Names on the map", "Namen op de kaart"), tr("Handy while you're learning", "Handig om eerst te leren"), s.mapLabels) {
                scope.launch { app.settings.setMapLabels(it) }
            }
        }

        Section("Feedback") {
            SwitchRow(Icons.AutoMirrored.Rounded.VolumeUp, tr("Sound", "Geluid"), tr("A sound for right and wrong", "Geluidje bij goed en fout"), s.sound) {
                scope.launch { app.settings.setSound(it) }
            }
            Divider()
            SwitchRow(Icons.Rounded.Vibration, tr("Vibration", "Trillen"), tr("A short tap with every answer", "Korte tik bij elk antwoord"), s.haptics) {
                if (it) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch { app.settings.setHaptics(it) }
            }
        }

        Section(tr("Ranking", "Ranglijst")) {
            SettingRow(
                Icons.Rounded.Badge, tr("Nickname", "Bijnaam"),
                nickname.ifBlank { tr("Not set yet, tap to choose", "Nog niet gekozen, tik om te kiezen") },
                onClick = { editName = true },
            )
            Divider()
            if (app.leaderboard.isOnline) {
                SettingRow(Icons.Rounded.CloudDone, tr("Online ranking", "Online ranglijst"),
                    if (pending.isEmpty()) tr("Connected, everything sent", "Verbonden, alles verstuurd")
                    else tr("Connected, ${pending.size} waiting to send", "Verbonden, ${pending.size} nog te versturen"))
            } else {
                SettingRow(Icons.Rounded.CloudOff, tr("Online ranking", "Online ranglijst"),
                    tr("Not switched on yet. ${pending.size} result(s) saved for later.", "Staat nog niet aan. ${pending.size} uitslag(en) bewaard voor later."),
                    tint = Night.muted)
            }
            if (app.leaderboard.isOnline) {
                Divider()
                SettingRow(
                    Icons.Rounded.DeleteForever, tr("Delete my online data", "Mijn online gegevens wissen"),
                    if (onlineDeleted) tr("Deleted", "Gewist") else tr("Removes your results from the online ranking", "Haalt je uitslagen uit de online ranglijst"),
                    tint = Night.danger, onClick = { confirmDeleteOnline = true },
                )
            }
        }

        Section(tr("Progress", "Voortgang")) {
            SettingRow(
                Icons.Rounded.DeleteForever, tr("Reset progress", "Voortgang wissen"),
                if (resetDone) tr("Reset", "Gewist") else tr("All scores and your streak back to zero", "Alle scores en je reeks op nul"),
                tint = Night.danger, onClick = { confirmReset = true },
            )
        }

        Section(tr("About Lodestar", "Over Lodestar")) {
            SettingRow(Icons.Rounded.Info, tr("Version $version", "Versie $version"), tr("Made for learning geography", "Gemaakt om topografie te leren"))
            Divider()
            SettingRow(Icons.Rounded.Policy, tr("Privacy policy", "Privacybeleid"), tr("No ads, no tracking; progress stays on your phone", "Geen reclame, geen tracking; je voortgang blijft op je telefoon"),
                onClick = { runCatching { uri.openUri("https://github.com/VvWijhe/topo_app/blob/main/PRIVACY.md") } })
            Divider()
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow(tr("Sources", "Bronnen"))
                Text(
                    tr(
                        "Maps: Natural Earth (public domain) and Statistics Netherlands boundaries via cartomap/nl. " +
                            "Country data: mledoze/countries (ODbL). Flags: hampusborgos/country-flags (public domain). " +
                            "Fonts: Young Serif and Figtree (SIL Open Font License).",
                        "Kaarten: Natural Earth (publiek domein) en CBS-gebiedsindelingen via cartomap/nl. " +
                            "Landgegevens: mledoze/countries (ODbL). Vlaggen: hampusborgos/country-flags (publiek domein). " +
                            "Lettertypen: Young Serif en Figtree (SIL Open Font License).",
                    ),
                    style = MaterialTheme.typography.bodySmall, color = Night.muted,
                )
            }
        }
    }

    if (editName) NicknameDialog(onDismiss = { editName = false })
    if (confirmDeleteOnline) {
        AlertDialog(
            onDismissRequest = { confirmDeleteOnline = false },
            containerColor = Night.surfaceHigh,
            title = { Text(tr("Delete online data?", "Online gegevens wissen?"), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(tr("All your results disappear from the online ranking. Your progress on this phone stays.", "Al je uitslagen verdwijnen uit de online ranglijst. Je voortgang op deze telefoon blijft."), color = Night.muted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteOnline = false
                    scope.launch { app.leaderboard.deleteMyData(); onlineDeleted = true }
                }) { Text(tr("Delete", "Wissen"), color = Night.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteOnline = false }) { Text(tr("Cancel", "Annuleren"), color = Night.text) } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Night.surfaceHigh,
            title = { Text(tr("Reset all progress?", "Alle voortgang wissen?"), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(tr("Your best scores, your streak and ‘Continue’ will be lost. This can't be undone.", "Je beste scores, je reeks en ‘Verder spelen’ gaan verloren. Dit kun je niet ongedaan maken."), color = Night.muted) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.progress.reset(); resetDone = true }
                    confirmReset = false
                }) { Text(tr("Reset", "Wissen"), color = Night.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(tr("Cancel", "Annuleren"), color = Night.text) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Eyebrow(title, Modifier.padding(start = 4.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Night.surface.copy(alpha = 0.9f))
                .border(1.dp, Night.outline, MaterialTheme.shapes.medium),
            content = content,
        )
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = Night.outlineSoft, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = Night.glow,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Night.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Night.muted)
        }
        if (trailing != null) { Spacer(Modifier.width(12.dp)); trailing() }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(icon, title, subtitle, onClick = { onChange(!checked) }) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Night.skyDeep, checkedTrackColor = Night.glow,
                uncheckedThumbColor = Night.muted, uncheckedTrackColor = Night.surfaceHigh, uncheckedBorderColor = Night.outline,
            ),
        )
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Night.outline, RoundedCornerShape(10.dp)),
    ) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .background(if (i == selected) Night.glow.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (i == selected) Night.glow else Night.muted)
            }
        }
    }
}
