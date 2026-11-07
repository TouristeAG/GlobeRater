package com.example.currencywidget.ui.screen

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.currencywidget.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.currencywidget.util.WidgetSpec
import com.example.currencywidget.widget.WidgetPalette
import com.example.currencywidget.widget.WidgetPaletteStore
import com.example.currencywidget.widget.WidgetPalettes

/**
 * Colour panel for one widget type: live preview, palette swatches, system colours,
 * and "apply to every widget".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaletteSheet(spec: WidgetSpec, version: Int, onDismiss: () -> Unit, onApplied: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val initial = remember(spec) { WidgetPaletteStore.idFor(context, spec.provider) }
    var selected by remember(spec) { mutableStateOf(initial) }
    var forAll by remember { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val systemAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = cs.surface,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(36.dp, 4.dp).clip(CircleShape).background(cs.outlineVariant))
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.colours_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(stringResource(spec.variantRes), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }

            // Live preview: re-rendered by the real widget code with the chosen palette.
            Box(
                Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.surfaceContainerHighest)
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                WidgetPreview(spec, version, maxHeight = 220.dp, paletteId = selected)
            }

            Text(stringResource(R.string.palettes_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = 4
            ) {
                WidgetPalettes.all.filter { it.id != WidgetPalettes.SYSTEM }.forEach { palette ->
                    Swatch(stringResource(palette.nameRes), swatchColors(palette), selected == palette.id) { selected = palette.id }
                }
            }

            Text(stringResource(R.string.system_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dark = isSystemInDarkTheme()
                val colors = if (systemAvailable) {
                    val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    listOf(scheme.surfaceContainerLow, scheme.secondaryContainer, scheme.primary)
                } else listOf(cs.surfaceContainer, cs.surfaceContainerHigh, cs.outline)
                Swatch(stringResource(R.string.material_you), colors, selected == WidgetPalettes.SYSTEM, enabled = systemAvailable) {
                    selected = WidgetPalettes.SYSTEM
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    if (systemAvailable) stringResource(R.string.system_follows)
                    else stringResource(R.string.system_needs_12),
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant
                )
            }

            Row(Modifier.padding(top = 24.dp).clickable { forAll = !forAll }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.use_for_all), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.use_for_all_hint, stringResource(spec.variantRes)), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                Switch(
                    checked = forAll, onCheckedChange = { forAll = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = cs.primary, checkedThumbColor = cs.onPrimary)
                )
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Text(
                    stringResource(R.string.palette_old_android),
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = cs.onSurfaceVariant) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (forAll) WidgetPaletteStore.setAll(context, selected)
                        else WidgetPaletteStore.set(context, spec.provider, selected)
                        onApplied()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.action_apply)) }
            }
        }
    }
}

/** Light or dark swatch colours (bg, key, accent) matching the app theme. */
@Composable
private fun swatchColors(p: WidgetPalette): List<Color> {
    val ints = if (isSystemInDarkTheme()) p.swatchDark else p.swatchLight
    return ints.map { Color(it) }
}

/** A tile split like the widget: background, a key-coloured strip and an accent dot. */
@Composable
private fun Swatch(name: String, colors: List<Color>, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ring by animateColorAsState(if (selected) cs.onSurface else Color.Transparent, label = "ring")
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, spring(dampingRatio = 0.55f), label = "scale")
    val dot by animateDpAsState(if (selected) 18.dp else 14.dp, label = "dot")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .scale(scale)
                .border(2.dp, ring, RoundedCornerShape(20.dp))
                .padding(4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors[0])
                .border(0.5.dp, cs.outlineVariant, RoundedCornerShape(16.dp))
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().fillMaxHeight(0.38f).background(colors[1]))
            Box(Modifier.align(Alignment.Center).size(dot).clip(CircleShape).background(colors[2]))
        }
        Text(
            name, style = MaterialTheme.typography.labelMedium,
            color = if (enabled) cs.onSurface else cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
