package nl.topo.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.topo.app.data.content.Levels
import nl.topo.app.data.i18n.tr
import nl.topo.app.ui.theme.Figtree
import nl.topo.app.ui.theme.Night

/** Donkere kaart met dunne rand; het standaardblok van de app. */
@Composable
fun NightCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Night.surface.copy(alpha = 0.88f))
            .border(1.dp, Night.outline, MaterialTheme.shapes.medium)
            .padding(padding),
        content = content,
    )
}

/** ★★☆☆-weergave in goud. */
@Composable
fun StarsText(count: Int, modifier: Modifier = Modifier, size: Int = 13) {
    Text("★".repeat(count), modifier, color = Night.gold, fontSize = size.sp, letterSpacing = 1.sp)
}

/** Kleine kop in hoofdletters. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Night.muted) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

/** Ring die zich vult tot [fraction]; met percentage in het midden. */
@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, diameter: Dp = 56.dp, stroke: Dp = 5.dp, showLabel: Boolean = true) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val w = stroke.toPx()
            val tl = Offset(w / 2, w / 2)
            val sz = Size(size.width - w, size.height - w)
            drawArc(Night.outline, 0f, 360f, false, topLeft = tl, size = sz, style = Stroke(w))
            drawArc(
                Brush.sweepGradient(listOf(Night.glowDim, Night.glow, Night.glowDim)),
                -90f, 360f * f, false, topLeft = tl, size = sz, style = Stroke(w, cap = StrokeCap.Round),
            )
        }
        if (showLabel) Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = Night.text)
    }
}

/** Dun voortgangsbalkje. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Night.glow) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "bar")
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Night.outline)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(color))
    }
}

/**
 * Een vakje in de onderwerpen-tabel: leeg (nog niet gespeeld), rood/geel met percentage, of groen met vinkje.
 */
@Composable
fun ScoreDot(best: Int?, modifier: Modifier = Modifier, diameter: Dp = 34.dp) {
    val (fill, edge, fg) = when {
        best == null -> Triple(Color.Transparent, Night.outline, Night.faint)
        best >= Levels.MASTERED -> Triple(Night.glow, Night.glow, Night.skyDeep)
        best >= Levels.OK -> Triple(Night.mid.copy(alpha = 0.16f), Night.mid, Night.text)
        else -> Triple(Night.danger.copy(alpha = 0.16f), Night.danger, Night.text)
    }
    Box(
        modifier
            .size(diameter)
            .drawBehind {
                if (best != null && best >= Levels.MASTERED) {
                    drawCircle(Night.glow.copy(alpha = 0.25f), radius = size.minDimension * 0.75f, center = Offset(size.width / 2, size.height / 2))
                }
            }
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, edge, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            best == null -> Icon(Icons.Rounded.PlayArrow, null, tint = fg, modifier = Modifier.size(16.dp))
            best >= Levels.MASTERED -> Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(18.dp))
            else -> Text("$best%", color = fg, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Vlammetje met het aantal dagen op rij. */
@Composable
fun StreakChip(days: Int, modifier: Modifier = Modifier) {
    val active = days > 0
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Night.ember.copy(alpha = 0.14f) else Night.surface)
            .border(1.dp, if (active) Night.ember.copy(alpha = 0.6f) else Night.outline, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (active) Night.ember else Night.faint, modifier = Modifier.size(18.dp))
        Text(
            if (days == 1) tr("1 day", "1 dag") else tr("$days days", "$days dagen"),
            style = MaterialTheme.typography.labelMedium,
            color = if (active) Night.text else Night.muted,
        )
    }
}

/** De hoofdknop: gloeiend groen. */
@Composable
fun GlowButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.drawBehind {
            if (enabled) drawRoundRect(
                Night.glow.copy(alpha = 0.22f),
                topLeft = Offset(-6f, -4f),
                size = size.copy(width = size.width + 12f, height = size.height + 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Night.glow, contentColor = Night.skyDeep,
            disabledContainerColor = Night.surfaceHigh, disabledContentColor = Night.faint,
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
}
