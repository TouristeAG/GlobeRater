package com.example.currencywidget.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.currencywidget.widget.CurrencySymbols


/** Small left label + secondary right label, like a magazine header. */
@Composable
fun ScreenHeader(left: String, right: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(left, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(right, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Section title in bold, small. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.padding(top = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

/** Strong rule, same weight as the widgets' outlines. */
@Composable
fun Rule(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 2.5.dp, color = MaterialTheme.colorScheme.primary)

/** Hairline between rows. */
@Composable
fun Hairline(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

/** Currency symbol on a solid key, like the widget keypad. [shade] is kept for callers. */
@Composable
fun CurrencyBadge(code: String, @Suppress("UNUSED_PARAMETER") shade: Int = 0, size: Dp = 44.dp) {
    val cs = MaterialTheme.colorScheme
    val symbol = CurrencySymbols.of(code)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.2f)).background(cs.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            color = cs.onPrimary,
            fontSize = if (symbol.length > 2) 11.sp else if (symbol.length == 2) 14.sp else 18.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun IconBadge(icon: ImageVector, size: Dp = 36.dp) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(cs.primary), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(size * 0.55f))
    }
}

/** Box with the widgets' 2.5dp outline (their result box). */
@Composable
fun OutlinedPanel(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .border(2.5.dp, cs.primary, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .padding(padding)
    ) { content() }
}

/** Budget bar like the widget's: thick, rounded, glides to its value. */
@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val value by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1100, easing = FastOutSlowInEasing), label = "line")
    Box(
        modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(value).clip(RoundedCornerShape(7.dp)).background(color))
    }
}
