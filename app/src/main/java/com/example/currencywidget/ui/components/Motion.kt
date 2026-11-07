package com.example.currencywidget.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/** Cards slide up and fade in one after the other the first time a screen shows. */
@Composable
fun Entrance(index: Int, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(420, delayMillis = 60 * index)) +
            slideInVertically(tween(480, delayMillis = 60 * index, easing = FastOutSlowInEasing)) { it / 5 } +
            scaleIn(tween(480, delayMillis = 60 * index), initialScale = 0.97f)
    ) { content() }
}

/** Slight squash while pressed: feedback that feels physical. */
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Text that rolls up (or down) when its value changes, like a flip counter. */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    up: Boolean = true
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            val dir = if (up) 1 else -1
            (slideInVertically(tween(260)) { h -> dir * h / 2 } + fadeIn(tween(260))) togetherWith
                (slideOutVertically(tween(200)) { h -> -dir * h / 2 } + fadeOut(tween(160)))
        },
        label = "rolling"
    ) { value ->
        Text(value, style = style, color = color, fontWeight = fontWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
