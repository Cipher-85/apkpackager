package com.apkpackager.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Shimmer Effect ──

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateAnim, 0f),
                    end = Offset(translateAnim + 300f, 0f),
                ),
            ),
    )
}

@Composable
fun ShimmerListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(widthFraction = 0.6f, height = 18.dp)
            Spacer(Modifier.height(8.dp))
            ShimmerBox(widthFraction = 0.35f, height = 14.dp)
        }
        Spacer(Modifier.width(16.dp))
        ShimmerBox(widthFraction = 0f, height = 20.dp, modifier = Modifier.width(20.dp))
    }
}

@Composable
fun ShimmerCardItem(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(widthFraction = 0f, height = 24.dp, modifier = Modifier.width(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(widthFraction = 0.5f, height = 18.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBox(widthFraction = 0.7f, height = 14.dp)
            }
        }
        Spacer(Modifier.height(8.dp))
        ShimmerBox(widthFraction = 0.4f, height = 12.dp)
    }
}

@Composable
fun ShimmerLoadingList(
    itemCount: Int = 6,
    modifier: Modifier = Modifier,
    useCardStyle: Boolean = false,
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        repeat(itemCount) {
            if (useCardStyle) {
                ShimmerCardItem()
            } else {
                ShimmerListItem()
            }
        }
    }
}

// ── Button Press Scale ──

@Composable
fun rememberPressInteraction(): Pair<MutableInteractionSource, Modifier> {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.97f else 1f
    val modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    return interactionSource to modifier
}
