package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCustomTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tactile 3D Stretch and Squish bounce animation modifier.
 * When pressed, the element squishes slightly and stretches on release with spring physics.
 */
fun Modifier.stretchBounceClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }

    this
        .graphicsLayer {
            this.scaleX = scaleX.value
            this.scaleY = scaleY.value
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    coroutineScope.launch {
                        scaleX.animateTo(1.05f, spring(dampingRatio = 0.5f, stiffness = 500f))
                        scaleY.animateTo(0.92f, spring(dampingRatio = 0.5f, stiffness = 500f))
                    }
                    val released = tryAwaitRelease()
                    if (released) {
                        coroutineScope.launch {
                            scaleX.animateTo(0.96f, spring(dampingRatio = 0.5f, stiffness = 500f))
                            scaleY.animateTo(1.04f, spring(dampingRatio = 0.5f, stiffness = 500f))
                            scaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                            scaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                        }
                        onClick()
                    } else {
                        coroutineScope.launch {
                            scaleX.animateTo(1f, spring())
                            scaleY.animateTo(1f, spring())
                        }
                    }
                }
            )
        }
}

/**
 * Ultra-premium continuous shimmer sweep modifier.
 */
fun Modifier.shimmerGlow(
    highlightColor: Color = Color.White.copy(alpha = 0.45f),
    durationMs: Int = 1800
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    this.drawBehind {
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                highlightColor,
                Color.Transparent
            ),
            start = Offset(translateAnim, 0f),
            end = Offset(translateAnim + 250f, size.height)
        )
        drawRect(brush = brush)
    }
}

/**
 * Breathing pulsating glow modifier for AI actions and highlight cards.
 */
fun Modifier.pulseGlow(
    glowColor: Color,
    minAlpha: Float = 0.15f,
    maxAlpha: Float = 0.45f,
    durationMs: Int = 1600
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    this.shadow(
        elevation = 10.dp,
        shape = RoundedCornerShape(18.dp),
        ambientColor = glowColor.copy(alpha = alpha),
        spotColor = glowColor.copy(alpha = alpha)
    )
}

/**
 * 3D Depth Card with Expandable / Collapsible Open-Close drawer.
 * - Top-to-bottom subtle gradient
 * - 3D Bevel border
 * - Deep shadow
 * - Spring expand/collapse
 */
@Composable
fun Card3D(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    isExpandable: Boolean = false,
    initiallyExpanded: Boolean = false,
    elevation: Dp = 6.dp,
    expandableContent: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(isExpanded: Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val customTheme = LocalCustomTheme.current

    val animatedElevation by animateFloatAsState(
        targetValue = if (isExpanded) 12f else 6f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "3d_elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = animatedElevation.dp,
                shape = shape,
                ambientColor = customTheme.shadowColor,
                spotColor = customTheme.shadowColor
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            customTheme.cardGradientTop,
                            customTheme.cardGradientBottom
                        )
                    )
                )
                .then(
                    if (isExpandable) {
                        Modifier.clickable { isExpanded = !isExpanded }
                    } else Modifier
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                content(isExpanded)

                if (isExpandable && expandableContent != null) {
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeIn(tween(250)),
                        exit = shrinkVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeOut(tween(200))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            expandableContent()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Staggered Entrance Animation Wrapper
 * Each child smoothly slides up with bouncy spring physics.
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    delayUnitMs: Int = 40,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val translateY = remember { Animatable(40f) }
    val scale = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        delay((index * delayUnitMs).toLong())
        coroutineScope {
            launch { alpha.animateTo(1f, tween(300)) }
            launch { translateY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 450f)) }
            launch { scale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 450f)) }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha.value
                this.translationY = translateY.value
                this.scaleX = scale.value
                this.scaleY = scale.value
            }
    ) {
        content()
    }
}
