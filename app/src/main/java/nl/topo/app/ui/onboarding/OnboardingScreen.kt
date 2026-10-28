package nl.topo.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.launch
import nl.topo.app.data.i18n.Lang
import nl.topo.app.data.i18n.tr
import nl.topo.app.data.online.PlayerProfile
import nl.topo.app.data.prefs.RoundLength
import nl.topo.app.data.prefs.Settings
import nl.topo.app.data.prefs.ThemeMode
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.GlowButton
import nl.topo.app.ui.components.NightSky
import nl.topo.app.ui.theme.DayPalette
import nl.topo.app.ui.theme.Night
import nl.topo.app.ui.theme.NightPalette

private const val STEPS = 6

/**
 * Eerste keer opstarten: in een paar tikken de belangrijkste keuzes.
 * Elke keuze wordt meteen opgeslagen (en het thema meteen getoond); alles is later te wijzigen in Instellingen.
 */
@Composable
fun OnboardingScreen(settings: Settings) {
    val app = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }

    fun finish() {
        scope.launch {
            if (name.isNotBlank()) { app.profile.setNickname(name); app.scope.launch { app.leaderboard.syncNickname() } }
            app.settings.setOnboarded(true)
        }
    }
    BackHandler(enabled = step > 0) { step-- }

    NightSky {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            // kop: stapjes + overslaan
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(STEPS) { i ->
                        Box(Modifier.height(4.dp).width(if (i == step) 22.dp else 10.dp).clip(RoundedCornerShape(50)).background(if (i <= step) Night.glow else Night.outline))
                    }
                }
                TextButton(onClick = { finish() }) { Text(tr("Skip", "Overslaan"), color = Night.muted) }
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(step, modifier = Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { s ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (s) {
                        0 -> {
                            Title(tr("Welcome to Lodestar", "Welkom bij Lodestar"), tr("Learn the map of the world. First a few quick choices; you can change everything later in Settings.", "Leer de wereldkaart. Eerst een paar snelle keuzes; alles is later aan te passen in Instellingen."))
                            Eyebrow(tr("Language", "Taal"))
                            Option("English", tr("Menus and country names in English", "Menu's en landnamen in het Engels"), settings.language == Lang.EN) { scope.launch { app.settings.setLanguage(Lang.EN) } }
                            Option("Nederlands", tr("Menus and country names in Dutch", "Menu's en landnamen in het Nederlands"), settings.language == Lang.NL) { scope.launch { app.settings.setLanguage(Lang.NL) } }
                        }
                        1 -> {
                            Title(tr("Night or day?", "Nacht of dag?"), tr("Pick the look of the app.", "Kies hoe de app eruitziet."))
                            Option(tr("Night map", "Nachtkaart"), tr("Dark, easy on the eyes", "Donker, rustig voor je ogen"), settings.theme == ThemeMode.DARK, swatch = NightPalette.sky to NightPalette.land) { scope.launch { app.settings.setTheme(ThemeMode.DARK) } }
                            Option(tr("Day map", "Dagkaart"), tr("Light, easy to read outdoors", "Licht, goed leesbaar buiten"), settings.theme == ThemeMode.LIGHT, swatch = DayPalette.sky to DayPalette.land) { scope.launch { app.settings.setTheme(ThemeMode.LIGHT) } }
                            Option(tr("Follow my phone", "Zoals mijn telefoon"), tr("Switches with your phone's dark mode", "Wisselt mee met de donkere modus van je telefoon"), settings.theme == ThemeMode.SYSTEM) { scope.launch { app.settings.setTheme(ThemeMode.SYSTEM) } }
                        }
                        2 -> {
                            Title(tr("How long is a round?", "Hoe lang is een ronde?"), tr("When you practise a topic, like the countries of Europe.", "Als je een onderwerp oefent, zoals de landen van Europa."))
                            Option(
                                tr("10 questions", "10 vragen"),
                                tr("Quick practice. Earns points and keeps your streak, but does NOT count as completing a level.", "Snel oefenen. Levert punten op en houdt je reeks vast, maar telt NIET als een niveau gehaald."),
                                settings.roundLength == RoundLength.TEN,
                            ) { scope.launch { app.settings.setRoundLength(RoundLength.TEN) } }
                            Option(
                                tr("Whole level", "Hele niveau"),
                                tr("Every item of the level. This is how you complete levels and fill your progress.", "Alles van het niveau. Zo haal je niveaus en vul je je voortgang."),
                                settings.roundLength == RoundLength.ALL,
                            ) { scope.launch { app.settings.setRoundLength(RoundLength.ALL) } }
                        }
                        3 -> {
                            Title(tr("Typing answers", "Antwoorden typen"), tr("On the hardest levels you type the names yourself.", "Op de moeilijkste niveaus typ je de namen zelf."))
                            Option(tr("Forgiving", "Soepel"), tr("A small typo or a missing accent still counts (Curacao ≈ Curaçao)", "Een kleine tikfout of een ontbrekend accent telt nog goed (Curacao ≈ Curaçao)"), !settings.strictTyping) { scope.launch { app.settings.setStrictTyping(false) } }
                            Option(tr("Strict", "Streng"), tr("Spelling must be exactly right, accents included", "De spelling moet precies goed, met accenten"), settings.strictTyping) { scope.launch { app.settings.setStrictTyping(true) } }
                        }
                        4 -> {
                            Title(tr("Map and feedback", "Kaart en geluid"), tr("Small things that make it nicer.", "Kleine dingen die het fijner maken."))
                            Toggle(tr("Names on the map", "Namen op de kaart"), tr("Handy while you're still learning", "Handig zolang je nog aan het leren bent"), settings.mapLabels) { scope.launch { app.settings.setMapLabels(it) } }
                            Toggle(tr("High-contrast map", "Kaart met hoog contrast"), tr("Brighter borders, easier to see in sunlight", "Helderdere grenzen, beter te zien in de zon"), settings.highContrast) { scope.launch { app.settings.setHighContrast(it) } }
                            Toggle(tr("Sound", "Geluid"), tr("A sound for right and wrong", "Een geluidje bij goed en fout"), settings.sound) { scope.launch { app.settings.setSound(it) } }
                            Toggle(tr("Vibration", "Trillen"), tr("A short tap with every answer", "Een korte tik bij elk antwoord"), settings.haptics) { scope.launch { app.settings.setHaptics(it) } }
                        }
                        else -> {
                            Title(tr("Your nickname", "Je bijnaam"), tr("Shown in the daily ranking and to friends. Optional — no real name needed.", "Te zien in de dagelijkse ranglijst en bij vrienden. Mag ook leeg — je echte naam is niet nodig."))
                            OutlinedTextField(
                                value = name, onValueChange = { if (it.length <= PlayerProfile.MAX_NAME) name = it },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(tr("e.g. Mapmaster", "bijv. Kaartkoning"), color = Night.faint) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Night.glow, unfocusedBorderColor = Night.outline,
                                    focusedContainerColor = Night.surface, unfocusedContainerColor = Night.surface,
                                    cursorColor = Night.glow, focusedTextColor = Night.text, unfocusedTextColor = Night.text,
                                ),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = { step-- }) { Text(tr("Back", "Terug"), color = Night.text) }
                Spacer(Modifier.weight(1f))
                GlowButton(
                    if (step < STEPS - 1) tr("Next", "Volgende") else tr("Let's go", "Aan de slag"),
                    { if (step < STEPS - 1) step++ else finish() },
                )
            }
        }
    }
}

@Composable
private fun Title(title: String, sub: String) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Text(title, style = MaterialTheme.typography.displaySmall, color = Night.text)
        Spacer(Modifier.height(6.dp))
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = Night.muted)
    }
}

@Composable
private fun Option(title: String, sub: String, selected: Boolean, swatch: Pair<Color, Color>? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(if (selected) Night.glow.copy(alpha = 0.12f) else Night.surface.copy(alpha = 0.9f))
            .border(1.5.dp, if (selected) Night.glow else Night.outline, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (swatch != null) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(swatch.first).border(1.dp, Night.outline, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(50)).background(swatch.second))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Night.text)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Night.muted)
        }
        Spacer(Modifier.width(10.dp))
        Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (selected) Night.glow else Night.faint)
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Night.surface.copy(alpha = 0.9f))
            .border(1.dp, Night.outline, MaterialTheme.shapes.medium).clickable { onChange(!on) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Night.text)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Night.muted)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = on, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Night.onAccent, checkedTrackColor = Night.glow, uncheckedThumbColor = Night.muted, uncheckedTrackColor = Night.surfaceHigh, uncheckedBorderColor = Night.outline),
        )
    }
}
