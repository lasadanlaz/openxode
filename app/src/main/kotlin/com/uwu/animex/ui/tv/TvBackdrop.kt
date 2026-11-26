package com.uwu.animex.ui.tv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val stars =
    List(90) { i ->
        val x = ((i * 733) % 1000) / 1000f
        val y = ((i * 419) % 1000) / 1000f
        val a = 0.18f + ((i * 37) % 42) / 100f
        val r = 0.7f + ((i * 13) % 10) / 8f
        Triple(x, y, r to a)
    }

/**
 * Latar TV yang sepenuhnya statis. Tidak ada partikel/aurora yang bergerak,
 * tidak ada sensor parallax, dan tidak ada frame loop.
 */
@Composable
fun TvBackdrop(
    style: TvBackdropStyle,
    modifier: Modifier = Modifier,
) {
    val visibleStars = remember(style) { style == TvBackdropStyle.STARS || style == TvBackdropStyle.AURORA }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(TvColors.Background, TvColors.BackgroundLift, Color(0xFF08070B)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        if (style == TvBackdropStyle.OFF) return@Canvas

        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0x335D7A4C), Color(0x145D7A4C), Color.Transparent),
                center = Offset(size.width * 0.74f, size.height * 0.02f),
                radius = size.width * 0.56f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0x2E5B447E), Color(0x0F5B447E), Color.Transparent),
                center = Offset(size.width * 0.02f, size.height * 1.02f),
                radius = size.width * 0.52f,
            ),
        )
        if (style == TvBackdropStyle.AURORA) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color(0x1F4C6E7A), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.2f),
                    radius = size.width * 0.35f,
                ),
            )
        }
        if (visibleStars) {
            stars.forEach { (x, y, ra) ->
                val (r, a) = ra
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = r,
                    center = Offset(size.width * x, size.height * y),
                )
            }
        }
    }
}
