package nl.topo.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import nl.topo.app.data.geo.Country
import nl.topo.app.data.i18n.Lang
import nl.topo.app.data.i18n.LocalLang
import nl.topo.app.data.i18n.tr
import nl.topo.app.ui.components.FlagImage
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.theme.Night
import java.text.Normalizer

private fun fold(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().replace("’", "'")

/** Onderblad met zoekveld en een alfabetische lijst landen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPicker(title: String, countries: List<Country>, onDismiss: () -> Unit, onPick: (Country) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    val lang = LocalLang.current
    val sorted = remember(countries, lang) { countries.sortedWith(compareBy(lang.collator) { it.name(lang) }) }
    val shown = remember(query, sorted, lang) {
        val q = fold(query.trim())
        // Zoeken werkt in beide talen: "Germany" en "Duitsland" vinden allebei Duitsland.
        fun hit(s: String?) = s != null && fold(s).contains(q)
        if (q.isEmpty()) sorted
        else sorted.filter { c -> Lang.entries.any { l -> hit(c.name(l)) || hit(c.capital(l)) } }
            .sortedBy { if (fold(it.name(lang)).startsWith(q)) 0 else 1 }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = Night.surface,
        contentColor = Night.text,
        scrimColor = Night.skyDeep.copy(alpha = 0.6f),
    ) {
        Column(Modifier.fillMaxHeight(0.88f).imePadding()) {
            Row(Modifier.padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(tr("${countries.size} countries", "${countries.size} landen"))
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = Night.text)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, tr("Close", "Sluiten"), tint = Night.muted) }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text(tr("Search a country or capital", "Zoek een land of hoofdstad"), color = Night.faint) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Night.muted) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, tr("Clear", "Wissen"), tint = Night.muted) }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Night.glow, unfocusedBorderColor = Night.outline,
                    focusedContainerColor = Night.surfaceHigh, unfocusedContainerColor = Night.surfaceHigh,
                    cursorColor = Night.glow, focusedTextColor = Night.text, unfocusedTextColor = Night.text,
                ),
            )
            if (shown.isEmpty()) {
                Text(
                    tr("No country found for “$query”.", "Geen land gevonden voor “$query”."),
                    style = MaterialTheme.typography.bodyMedium, color = Night.muted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(shown, key = { it.code }) { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(c) }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { FlagImage(c, height = 22.dp, maxWidth = 36.dp) }
                        Column(Modifier.weight(1f)) {
                            Text(c.name(lang), style = MaterialTheme.typography.titleSmall, color = Night.text)
                            c.capital(lang)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Night.muted) }
                        }
                        Text(formatPopulation(c.population, lang), style = MaterialTheme.typography.labelMedium, color = Night.faint)
                        Spacer(Modifier.width(2.dp))
                    }
                    HorizontalDivider(color = Night.outlineSoft, modifier = Modifier.padding(start = 70.dp))
                }
            }
        }
    }
}
