package com.tcgptrading.tcgptrading.core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import pokemoncmp.composeapp.generated.resources.Res
import pokemoncmp.composeapp.generated.resources.bricolage_bold
import pokemoncmp.composeapp.generated.resources.bricolage_extrabold
import pokemoncmp.composeapp.generated.resources.lexend_medium
import pokemoncmp.composeapp.generated.resources.lexend_regular
import pokemoncmp.composeapp.generated.resources.lexend_semibold

/** Body text: rounded and easy to read on small screens. */
val lexendFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(resource = Res.font.lexend_regular, weight = FontWeight.Normal),
        Font(resource = Res.font.lexend_medium, weight = FontWeight.Medium),
        Font(resource = Res.font.lexend_semibold, weight = FontWeight.SemiBold),
    )

/** Headings and card names. */
val bricolageFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(resource = Res.font.bricolage_bold, weight = FontWeight.Bold),
        Font(resource = Res.font.bricolage_extrabold, weight = FontWeight.ExtraBold),
    )
