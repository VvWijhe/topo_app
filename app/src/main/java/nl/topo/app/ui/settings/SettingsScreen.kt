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
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TextFields
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.topo.app.data.prefs.RoundLength
import nl.topo.app.data.prefs.Settings
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.theme.Night

@Composable
fun SettingsScreen() {
    val app = LocalAppContainer.current
    val s by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text("Instellingen", style = MaterialTheme.typography.displayMedium, color = Night.text)
        }

        Section("Spelen") {
            SettingRow(Icons.Rounded.FormatListNumbered, "Rondelengte", "Hoeveel vragen per ronde") {
                Segmented(RoundLength.entries.map { it.label }, s.roundLength.ordinal) { i ->
                    scope.launch { app.settings.setRoundLength(RoundLength.entries[i]) }
                }
            }
            Divider()
            SwitchRow(Icons.Rounded.TextFields, "Namen op de kaart", "Handig om eerst te leren", s.mapLabels) {
                scope.launch { app.settings.setMapLabels(it) }
            }
        }

        Section("Feedback") {
            SwitchRow(Icons.AutoMirrored.Rounded.VolumeUp, "Geluid", "Geluidje bij goed en fout", s.sound) {
                scope.launch { app.settings.setSound(it) }
            }
            Divider()
            SwitchRow(Icons.Rounded.Vibration, "Trillen", "Korte tik bij elk antwoord", s.haptics) {
                if (it) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch { app.settings.setHaptics(it) }
            }
        }

        Section("Voortgang") {
            SettingRow(
                Icons.Rounded.DeleteForever, "Voortgang wissen",
                if (resetDone) "Gewist" else "Alle scores en je reeks op nul",
                tint = Night.danger, onClick = { confirmReset = true },
            )
        }

        Section("Over Topo") {
            SettingRow(Icons.Rounded.Info, "Versie $version", "Gemaakt om topografie te leren")
            Divider()
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("Bronnen")
                Text(
                    "Kaarten: Natural Earth (publiek domein) en CBS-gebiedsindelingen via cartomap/nl. " +
                        "Landgegevens: mledoze/countries (ODbL). Vlaggen: flag-icons (MIT). " +
                        "Lettertypen: Young Serif en Figtree (SIL Open Font License).",
                    style = MaterialTheme.typography.bodySmall, color = Night.muted,
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Night.surfaceHigh,
            title = { Text("Alle voortgang wissen?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Je beste scores, je reeks en ‘Verder spelen’ gaan verloren. Dit kun je niet ongedaan maken.", color = Night.muted) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.progress.reset(); resetDone = true }
                    confirmReset = false
                }) { Text("Wissen", color = Night.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Annuleren", color = Night.text) } },
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
