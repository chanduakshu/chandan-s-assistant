package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiveAudioVisualizer(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    activeColor: Color = MaterialTheme.colorScheme.secondary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    maxHeight: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")

    val animations = (0 until barCount).map { index ->
        val duration = 300 + (index * 70) % 400
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = if (isSpeaking) 0.95f else 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        animations.forEach { animatedHeightFraction ->
            val height = if (isSpeaking) {
                (maxHeight.value * animatedHeightFraction.value).dp.coerceAtLeast(6.dp)
            } else {
                8.dp
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isSpeaking) activeColor else inactiveColor)
            )
        }
    }
}

@Composable
fun PlaybackWaveform(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    playedColor: Color = MaterialTheme.colorScheme.primary,
    unplayedColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
) {
    // Generate deterministic wave heights
    val heights = remember(barCount) {
        listOf(
            0.3f, 0.5f, 0.8f, 0.4f, 0.9f, 0.6f, 0.7f, 0.3f, 0.85f, 0.95f,
            0.5f, 0.65f, 0.4f, 0.8f, 0.7f, 0.9f, 0.35f, 0.6f, 0.75f, 0.85f,
            0.5f, 0.4f, 0.6f, 0.8f, 0.45f, 0.3f, 0.7f, 0.5f
        ).take(barCount)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, normalizedHeight ->
            val barFraction = index.toFloat() / barCount
            val isPlayed = barFraction <= progress
            val barColor = if (isPlayed) playedColor else unplayedColor

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((36 * normalizedHeight).dp.coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
                    .clickable {
                        onSeek(barFraction)
                    }
            )
        }
    }
}
