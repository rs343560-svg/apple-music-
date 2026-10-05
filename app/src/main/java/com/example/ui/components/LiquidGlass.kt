package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.util.PerformanceMode

/**
 * Applies a hardware-efficient Liquid Glass visual style.
 * Uses linear gradients to emulate light refraction and specular border highlights
 * without requiring expensive multi-pass blur shaders on low-end GPUs.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color(0xFF202024).copy(alpha = 0.72f),
    specularHighlight: Boolean = true,
    elevation: Dp = 8.dp,
    performanceMode: PerformanceMode = PerformanceMode.BALANCED
): Modifier {
    val isLowEnd = performanceMode == PerformanceMode.BATTERY_SAVER
    val effectiveElevation = if (isLowEnd) 0.dp else elevation

    val borderBrush = if (specularHighlight && !isLowEnd) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    return this
        .then(
            if (effectiveElevation > 0.dp) {
                Modifier.shadow(
                    elevation = effectiveElevation,
                    shape = shape,
                    spotColor = Color.Black.copy(alpha = 0.45f),
                    ambientColor = Color.Black.copy(alpha = 0.2f)
                )
            } else Modifier
        )
        .clip(shape)
        .background(
            if (isLowEnd) {
                Brush.verticalGradient(
                    listOf(
                        backgroundColor.copy(alpha = 0.88f),
                        backgroundColor.copy(alpha = 0.88f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        backgroundColor.copy(alpha = 0.82f),
                        backgroundColor.copy(alpha = 0.65f),
                        backgroundColor.copy(alpha = 0.75f)
                    )
                )
            }
        )
        .border(
            width = if (isLowEnd) 0.5.dp else 1.dp,
            brush = borderBrush,
            shape = shape
        )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color(0xFF1E1E22).copy(alpha = 0.75f),
    elevation: Dp = 10.dp,
    performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                backgroundColor = backgroundColor,
                elevation = elevation,
                performanceMode = performanceMode
            )
            .then(clickModifier),
        content = content
    )
}

@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.16f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        backgroundColor.copy(alpha = 0.25f),
                        backgroundColor.copy(alpha = 0.12f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
