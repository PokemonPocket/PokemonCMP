package com.tcgptrading.tcgptrading.features.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tcgptrading.tcgptrading.core.presentation.components.CARD_RATIO
import com.tcgptrading.tcgptrading.core.presentation.components.CardFan
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors

private data class TourStep(val title: String, val text: String)

private val STEPS = listOf(
    TourStep(
        "Trade the cards you are missing",
        "TCGP Trading connects Pokémon TCG Pocket players. You list the cards you want, players who have them make offers, and you finish the trade in the game.",
    ),
    TourStep(
        "Mark the cards you have",
        "Open Collection and tap every card you own. Owned cards turn full colour with a tick; the ones you are missing stay grey.",
    ),
    TourStep(
        "Hold a card for more",
        "Press and hold any card in Collection. For a missing card, choose Add to my wants and the language you need. For a card you own, set how many copies you have so your spares can be traded.",
    ),
    TourStep(
        "Offer on other players' wants",
        "Have a card someone wants? Offer it, and pick up to 5 cards of the same rarity you would like in return. Pocket only trades matching rarities.",
    ),
    TourStep(
        "Pick the offer you like",
        "When players offer you a card you want, open My trades and review the offers. Pick which of their requested cards you will give and accept. The other offers are declined.",
    ),
    TourStep(
        "Finish the trade in the game",
        "A chat opens with the other player. Add each other in Pocket with your Friend IDs and make the trade there.",
    ),
)

/** First-time tour. onFinish(true) when the last step's "Start trading" was pressed. */
@Composable
fun TutorialDialog(onFinish: (goToWanted: Boolean) -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val isLast = index == STEPS.lastIndex

    Dialog(
        onDismissRequest = { onFinish(false) },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(16.dp).widthIn(max = 480.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = TcgColors.Surface,
        ) {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(200.dp).background(TcgColors.Ground),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(targetState = index, label = "tourScene") { step -> Scene(step) }
                }
                Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Step ${index + 1} of ${STEPS.size}", style = MaterialTheme.typography.labelMedium, color = TcgColors.Muted)
                    Text(STEPS[index].title, style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink)
                    Text(
                        STEPS[index].text, style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
                        modifier = Modifier.heightIn(min = 88.dp),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        STEPS.indices.forEach { i ->
                            Box(
                                Modifier.size(28.dp).clip(CircleShape)
                                    .semantics { contentDescription = "Step ${i + 1}: ${STEPS[i].title}" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.height(8.dp).width(if (i == index) 22.dp else 8.dp).clip(CircleShape)
                                        .background(if (i == index) TcgColors.Ink else TcgColors.Line)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TcgButton("Skip", onClick = { onFinish(false) }, style = TcgButtonStyle.Ghost)
                        Spacer(Modifier.weight(1f))
                        if (index > 0) TcgButton("Back", onClick = { index-- }, style = TcgButtonStyle.Secondary)
                        TcgButton(
                            if (isLast) "Start trading" else "Next",
                            onClick = { if (isLast) onFinish(true) else index++ },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Scene(step: Int) {
    val blue = Brush.verticalGradient(listOf(Color(0xFF9CC3F0), Color(0xFF5D8BD1)))
    val red = Brush.verticalGradient(listOf(Color(0xFFF6A08E), Color(0xFFD9544A)))
    val yellow = Brush.verticalGradient(listOf(Color(0xFFFBE08A), Color(0xFFE3B33A)))
    val green = Brush.verticalGradient(listOf(Color(0xFFA9DDB0), Color(0xFF4EA86A)))
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (step) {
            0 -> CardFan(Modifier.fillMaxWidth().height(160.dp), cardWidth = 84.dp)
            // Tap: a small collection grid, owned cards in colour with a tick, missing ones grey, one being tapped
            1 -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mini(blue, "◆◆", 54.dp, owned = true)
                    Box(contentAlignment = Alignment.Center) {
                        Mini(yellow, "◆◆◆", 54.dp, owned = true)
                        GestureRing(holding = false)
                    }
                    Mini(red, "◆◆◆", 54.dp, missing = true)
                    Mini(green, "◆", 54.dp, missing = true)
                }
                Pill("Tap to mark as owned", TcgColors.Ink)
            }
            // Hold: one missing card being pressed, and the options it opens
            2 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Mini(red, "◆◆◆", 84.dp, missing = true)
                    GestureRing(holding = true)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Hold for options", TcgColors.Ink)
                    Pill("Add to my wants", TcgColors.Red)
                    Text(
                        "Copies  −  2  +",
                        Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 14.dp, vertical = 7.dp),
                        color = TcgColors.Ink, style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            3 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Mini(red, "◆◆◆", 96.dp)
                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = TcgColors.Diamond, modifier = Modifier.size(32.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mini(green, "◆◆◆", 58.dp)
                    Mini(blue, "◆◆◆", 58.dp)
                }
            }
            4 -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Mini(blue, "◆◆◆◆", 60.dp)
                    Mini(yellow, "◆◆◆◆", 60.dp, picked = true)
                    Mini(green, "◆◆◆◆", 60.dp)
                }
                Pill("Accept and give this card", TcgColors.Red)
            }
            else -> Column(Modifier.widthIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Sent you a friend request",
                    Modifier.clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = TcgColors.Ink,
                )
                Text(
                    "Accepted, starting the trade",
                    Modifier.align(Alignment.End).clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp)).background(TcgColors.Ink).padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = Color.White,
                )
                Text(
                    "Friend ID 1190 4583 2201 7764",
                    Modifier.align(Alignment.CenterHorizontally).clip(CircleShape).background(Color.White).padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = TcgColors.Ink,
                )
            }
        }
    }
}

/**
 * A finger press on a card: a dot with a ring. When holding, the ring pulses outwards
 * so "press and hold" reads differently from a tap.
 */
@Composable
private fun GestureRing(holding: Boolean) {
    val pulse = if (holding) {
        val t by rememberInfiniteTransition(label = "hold").animateFloat(
            initialValue = 0.85f, targetValue = 1.25f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "holdPulse",
        )
        t
    } else 1f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.semantics { contentDescription = if (holding) "Press and hold" else "Tap" }) {
        Box(
            Modifier.size(44.dp).scale(pulse).clip(CircleShape)
                .background(TcgColors.Ink.copy(alpha = if (holding) 0.18f else 0.12f))
                .border(2.dp, TcgColors.Ink.copy(alpha = 0.55f), CircleShape)
        )
        Box(Modifier.size(14.dp).clip(CircleShape).background(TcgColors.Ink.copy(alpha = 0.8f)))
    }
}

/** A small drawn card with a rarity pill. owned adds a tick; missing greys it out like in Collection. */
@Composable
private fun Mini(art: Brush, glyph: String, width: Dp, picked: Boolean = false, owned: Boolean = false, missing: Boolean = false) {
    val grey = Brush.verticalGradient(listOf(Color(0xFFDDE3EA), Color(0xFFC3CBD5)))
    Box(
        Modifier.width(width).aspectRatio(CARD_RATIO)
            .then(if (picked) Modifier.border(3.dp, TcgColors.Red, RoundedCornerShape(10.dp)) else Modifier)
            .then(if (missing) Modifier.border(1.dp, TcgColors.Line, RoundedCornerShape(10.dp)) else Modifier)
            .clip(RoundedCornerShape(10.dp)).background(if (missing) TcgColors.Ground else Color.White).padding(5.dp),
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(if (missing) grey else art))
        if (owned) {
            Box(
                Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(TcgColors.Ink),
                contentAlignment = Alignment.Center,
            ) { Text("✓", color = Color.White, fontSize = 9.sp) }
        }
        Text(
            glyph,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 6.dp, vertical = 1.dp),
            color = TcgColors.Diamond, fontSize = 9.sp, letterSpacing = 1.sp,
        )
        if (picked) {
            Box(
                Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(TcgColors.Red),
                contentAlignment = Alignment.Center,
            ) { Text("✓", color = Color.White, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text,
        Modifier.clip(CircleShape).background(color).padding(horizontal = 14.dp, vertical = 7.dp),
        color = Color.White, style = MaterialTheme.typography.labelMedium,
    )
}
