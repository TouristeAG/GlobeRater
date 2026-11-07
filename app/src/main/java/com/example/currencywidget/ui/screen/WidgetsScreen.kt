package com.example.currencywidget.ui.screen

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import com.example.currencywidget.ui.components.Rule
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.LaunchedEffect
import com.example.currencywidget.ui.components.Entrance
import com.example.currencywidget.ui.components.pressScale
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.currencywidget.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.currencywidget.ui.components.ScreenHeader
import com.example.currencywidget.util.WidgetCatalog
import com.example.currencywidget.util.WidgetGroup
import com.example.currencywidget.util.WidgetSpec
import kotlinx.coroutines.launch


@Composable
fun WidgetsScreen(
    version: Int,
    contentPadding: PaddingValues,
    snackbar: SnackbarHostState,
    openColorsFor: String? = null,
    onColorsRequestHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var group by rememberSaveable { mutableStateOf(WidgetGroup.CONVERTER) }
    var manualHelpFor by remember { mutableStateOf<WidgetSpec?>(null) }
    var colorsFor by remember { mutableStateOf<WidgetSpec?>(null) }
    var localVersion by remember { mutableStateOf(0) }
    val renderVersion = version * 1000 + localVersion

    // Opened from a tap on a home-screen widget.
    LaunchedEffect(openColorsFor) {
        WidgetCatalog.byProvider(openColorsFor)?.let { spec ->
            group = spec.group
            colorsFor = spec
            onColorsRequestHandled()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp, end = 28.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
    ) {
        item {
            Column {
                ScreenHeader(stringResource(R.string.widgets_header), stringResource(R.string.widgets_header_right))
                Text(
                    stringResource(R.string.widgets_intro),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 28.dp, bottom = 28.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    WidgetGroup.values().forEach { g -> GroupTab(stringResource(g.titleRes), group == g) { group = g } }
                }
                Rule(Modifier.padding(top = 2.dp))
            }
        }
        val specs = WidgetCatalog.all.filter { it.group == group }
        specs.forEachIndexed { i, spec ->
            // Keyed by group so switching category replays the cascade.
            item(key = "${group.name}-${spec.provider.name}") {
                Entrance(i) {
                    WidgetCard(spec, renderVersion, onColors = { colorsFor = spec }) {
                        val ok = WidgetCatalog.pin(context, spec)
                        if (!ok) manualHelpFor = spec
                        ok
                    }
                }
            }
        }
    }

    colorsFor?.let { spec ->
        PaletteSheet(
            spec = spec,
            version = renderVersion,
            onDismiss = { colorsFor = null },
            onApplied = {
                colorsFor = null
                localVersion++
                scope.launch { snackbar.showSnackbar(context.getString(R.string.widgets_colours_applied)) }
            }
        )
    }

    manualHelpFor?.let { spec ->
        AlertDialog(
            onDismissRequest = { manualHelpFor = null },
            confirmButton = { TextButton(onClick = { manualHelpFor = null }) { Text(stringResource(R.string.action_ok)) } },
            title = { Text(stringResource(R.string.widgets_manual_title)) },
            text = { Text(stringResource(R.string.widgets_manual_text, stringResource(spec.variantRes))) }
        )
    }
}

@Composable
private fun GroupTab(title: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val color by animateColorAsState(if (selected) cs.onSurface else cs.onSurfaceVariant, label = "tab")
    val underline by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "underline")
    Column(
        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
        Box(
            Modifier.padding(top = 8.dp).height(2.dp).width(28.dp * underline).background(cs.primary)
        )
    }
}

@Composable
private fun WidgetCard(spec: WidgetSpec, version: Int, onColors: () -> Unit, onAdd: () -> Boolean) {
    val context = LocalContext.current
    val paletteName = remember(version) {
        context.getString(com.example.currencywidget.widget.WidgetPaletteStore.get(context, spec.provider).nameRes)
    }
    val cs = MaterialTheme.colorScheme
    var requested by remember { mutableStateOf(false) }
    LaunchedEffect(requested) { if (requested) { delay(2500); requested = false } }
    val press = remember { MutableInteractionSource() }
    Column(Modifier.padding(top = 24.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cs.surfaceContainerHighest)
                .clickable(onClick = onColors)
                .padding(vertical = 28.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            WidgetPreview(spec, version, maxHeight = 260.dp, onClick = onColors)
        }
        Row(Modifier.padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(spec.variantRes), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(spec.descriptionRes), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Row(
                    Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).clickable(onClick = onColors).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  $paletteName", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(16.dp))
            Surface(
                onClick = { if (onAdd()) requested = true },
                shape = RoundedCornerShape(12.dp),
                color = cs.primary,
                contentColor = cs.onPrimary,
                interactionSource = press,
                modifier = Modifier.size(44.dp).pressScale(press)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = requested,
                        transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                        label = "add"
                    ) { done ->
                        Icon(if (done) Icons.Filled.Check else Icons.Filled.Add, contentDescription = stringResource(R.string.widgets_add_cd, stringResource(spec.variantRes)), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * The real RemoteViews, built by the same code as the home-screen widget, rendered at the
 * widget's natural size and scaled to fit.
 */
@Composable
internal fun WidgetPreview(
    spec: WidgetSpec, version: Int, maxHeight: Dp, paletteId: String? = null, onClick: (() -> Unit)? = null
) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val w = spec.widthDp.dp
        val h = spec.heightDp.dp
        val scale = minOf(1f, maxWidth / w, maxHeight / h)
        Box(Modifier.size(w * scale, h * scale), contentAlignment = Alignment.Center) {
            key(spec.provider, version, paletteId) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).also { parent ->
                            runCatching { WidgetCatalog.preview(ctx, spec, paletteId).apply(ctx, parent) }
                                .onSuccess {
                                    parent.addView(it, FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                                    ))
                                }
                                .onFailure { android.util.Log.e("GlobeRater", "Preview failed for ${spec.provider.simpleName}", it) }
                        }
                    },
                    modifier = Modifier
                        .requiredSize(w, h)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                )
            }
            // Previews are pictures: the keys must not react, the whole preview opens the colours.
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (onClick != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick
                        ) else Modifier.pointerInput(Unit) {
                            awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                        }
                    )
            )
        }
    }
}
