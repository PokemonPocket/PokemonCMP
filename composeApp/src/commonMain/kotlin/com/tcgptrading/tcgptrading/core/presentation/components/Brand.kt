package com.tcgptrading.tcgptrading.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors

/** The app mark: two stacked cards with a plus, same as the website. */
@Composable
fun BrandMark(size: Dp = 30.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 30f
        drawRoundRect(TcgColors.Red, topLeft = Offset(3 * u, 5 * u), size = Size(16 * u, 22 * u), cornerRadius = CornerRadius(3 * u))
        drawRoundRect(Color.White, topLeft = Offset(11 * u, 3 * u), size = Size(16 * u, 22 * u), cornerRadius = CornerRadius(3 * u))
        drawRoundRect(TcgColors.Ink, topLeft = Offset(11 * u, 3 * u), size = Size(16 * u, 22 * u), cornerRadius = CornerRadius(3 * u), style = Stroke(2 * u))
        drawLine(TcgColors.Ink, Offset(15 * u, 14 * u), Offset(23 * u, 14 * u), strokeWidth = 2 * u, cap = StrokeCap.Round)
        drawLine(TcgColors.Ink, Offset(19 * u, 10 * u), Offset(19 * u, 18 * u), strokeWidth = 2 * u, cap = StrokeCap.Round)
    }
}

@Composable
fun BrandWordmark(modifier: Modifier = Modifier, markSize: Dp = 30.dp, fontSize: Int = 21) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BrandMark(markSize)
        Text("TCGP Trading", style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize.sp), color = TcgColors.Ink)
    }
}

/**
 * A fan of three stylised cards showing the rarity glyphs (◆◆ ★ ♛).
 * Drawn in code, so it needs no image assets and stays sharp at any size.
 */
@Composable
fun CardFan(modifier: Modifier = Modifier, cardWidth: Dp = 96.dp) {
    Box(modifier, contentAlignment = Alignment.Center) {
        MiniCard("◆◆", Brush.verticalGradient(listOf(Color(0xFF9CC3F0), Color(0xFF5D8BD1))), TcgColors.Diamond, cardWidth,
            Modifier.offset(x = -cardWidth * 0.72f, y = 10.dp).rotate(-10f))
        MiniCard("♛", Brush.verticalGradient(listOf(Color(0xFFC5B3F0), Color(0xFF7B5CC9))), TcgColors.Crown, cardWidth,
            Modifier.offset(x = cardWidth * 0.72f, y = 10.dp).rotate(10f))
        MiniCard("★", Brush.verticalGradient(listOf(Color(0xFFF6A08E), Color(0xFFD9544A))), TcgColors.Star, cardWidth, Modifier)
    }
}

@Composable
private fun MiniCard(glyph: String, art: Brush, glyphColor: Color, width: Dp, modifier: Modifier) {
    Box(
        modifier.width(width).aspectRatio(CARD_RATIO)
            .shadow(10.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)).background(Color.White).padding(6.dp),
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(art))
        Text(
            glyph,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                .clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 8.dp, vertical = 1.dp),
            color = glyphColor, fontSize = 13.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center,
        )
    }
}
