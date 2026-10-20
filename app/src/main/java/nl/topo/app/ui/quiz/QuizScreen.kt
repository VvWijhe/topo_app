package nl.topo.app.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.topo.app.data.content.Content
import nl.topo.app.data.content.Levels
import nl.topo.app.data.content.RegionId
import nl.topo.app.data.prefs.Settings
import nl.topo.app.ui.LocalAppContainer
import nl.topo.app.ui.components.Eyebrow
import nl.topo.app.ui.components.NightCard
import nl.topo.app.ui.components.StarsText
import nl.topo.app.ui.theme.Night

/**
 * Plek voor het quizscherm. Dit is de volgende bouwstap; nu staat hier een testknop
 * waarmee je een ronde kunt nabootsen, zodat de voortgang en de vakjes al werken.
 */
@Composable
fun QuizScreen(regionId: RegionId, topicId: String, level: Int, onBack: () -> Unit) {
    val app = LocalAppContainer.current
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val region = Content.region(regionId)
    val topic = Content.topic(regionId, topicId)
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Terug", tint = Night.text) }
        Column(Modifier.padding(horizontal = 4.dp)) {
            Eyebrow(region.name)
            Text(topic?.name ?: topicId, style = MaterialTheme.typography.displayMedium, color = Night.text)
            Row { StarsText(level, size = 16) }
            Text(
                "${Levels.all[level - 1].title} · ${region.levelScope[level - 1]} · ${Levels.all[level - 1].how} · ${settings.roundLength.label}",
                style = MaterialTheme.typography.bodyMedium, color = Night.muted,
            )
        }

        NightCard(Modifier.fillMaxWidth()) {
            Text("Hier komt de quiz", style = MaterialTheme.typography.headlineSmall, color = Night.text)
            Spacer(Modifier.height(4.dp))
            Text(
                "Het fundament staat: menu, navigatie, opslag en voortgang. Het quizscherm met de interactieve kaart bouwen we in de volgende stap.",
                style = MaterialTheme.typography.bodyMedium, color = Night.muted,
            )
        }

        NightCard(Modifier.fillMaxWidth()) {
            Eyebrow("Alleen om te testen", color = Night.ember)
            Spacer(Modifier.height(6.dp))
            Text("Doe alsof je een ronde speelde, en kijk wat er gebeurt met de vakjes en je voortgang.", style = MaterialTheme.typography.bodySmall, color = Night.muted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(45, 75, 100).forEach { pct ->
                    OutlinedButton(onClick = {
                        scope.launch {
                            val record = app.progress.recordRound(regionId, topicId, level, pct)
                            message = if (record) "$pct% opgeslagen — nieuw record" else "$pct% gespeeld; je record blijft staan"
                        }
                    }) { Text("$pct%", color = Night.text) }
                }
            }
            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.labelMedium, color = Night.glow)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
