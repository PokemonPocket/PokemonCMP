package com.tcgptrading.tcgptrading.core.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * TCGP Trading palette ("binder page"), shared with the website.
 * Pale sky ground, white card sleeves, navy ink. Poké-red is reserved for the main action of a screen.
 * Rarity is shown with the game's own glyphs (◆ ★ ♛), each with its own colour.
 */
object TcgColors {
    val Ground = Color(0xFFEAF1FA)      // page background
    val Surface = Color(0xFFFFFFFF)     // sleeves, panels
    val Soft = Color(0xFFF4F8FC)        // fills inside panels
    val Ink = Color(0xFF15233F)         // text, dark controls
    val Muted = Color(0xFF4A5872)       // secondary text
    val Faint = Color(0xFF5B6880)       // disabled text
    val Line = Color(0xFFC5D3E6)        // borders
    val LineSoft = Color(0xFFE3E9F2)    // hairlines, disabled fills
    val Chip = Color(0xFFDCE7F5)        // avatars, quiet fills
    val Red = Color(0xFFC8302E)         // primary action
    val RedInk = Color(0xFFA82422)      // red text
    val RedSoft = Color(0xFFFFF1F0)     // selected fill
    val RedTag = Color(0xFFFFE4E2)      // "New" tag
    val Diamond = Color(0xFF5D7698)     // ◆ rarity
    val Star = Color(0xFFB98A12)        // ★ rarity
    val Crown = Color(0xFF6B4FB8)       // ♛ rarity
    val Online = Color(0xFF1F9D55)
    val OkText = Color(0xFF1F7A45)
}
