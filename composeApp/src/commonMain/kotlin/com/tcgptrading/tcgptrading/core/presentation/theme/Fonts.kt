package com.tcgptrading.tcgptrading.core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import pokemoncmp.composeapp.generated.resources.Res
import pokemoncmp.composeapp.generated.resources.montserrat_black
import pokemoncmp.composeapp.generated.resources.montserrat_black_italic
import pokemoncmp.composeapp.generated.resources.montserrat_bold
import pokemoncmp.composeapp.generated.resources.montserrat_bold_italic
import pokemoncmp.composeapp.generated.resources.montserrat_extra_bold
import pokemoncmp.composeapp.generated.resources.montserrat_extra_bold_italic
import pokemoncmp.composeapp.generated.resources.montserrat_extra_light
import pokemoncmp.composeapp.generated.resources.montserrat_extra_light_italic
import pokemoncmp.composeapp.generated.resources.montserrat_italic
import pokemoncmp.composeapp.generated.resources.montserrat_light
import pokemoncmp.composeapp.generated.resources.montserrat_light_italic
import pokemoncmp.composeapp.generated.resources.montserrat_medium
import pokemoncmp.composeapp.generated.resources.montserrat_medium_italic
import pokemoncmp.composeapp.generated.resources.montserrat_regular
import pokemoncmp.composeapp.generated.resources.montserrat_semi_bold
import pokemoncmp.composeapp.generated.resources.montserrat_semi_bold_italic
import pokemoncmp.composeapp.generated.resources.montserrat_thin
import pokemoncmp.composeapp.generated.resources.montserrat_thin_italic

@OptIn(ExperimentalResourceApi::class)
val montserratFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(
            resource = Res.font.montserrat_regular,
            weight = FontWeight.Normal
        ),
        Font(
            resource = Res.font.montserrat_medium,
            weight = FontWeight.Medium
        ),
        Font(
            resource = Res.font.montserrat_semi_bold,
            weight = FontWeight.SemiBold,
        ),
        Font(
            resource = Res.font.montserrat_bold,
            weight = FontWeight.Bold
        ),
        Font(
            resource = Res.font.montserrat_extra_bold,
            weight = FontWeight.ExtraBold
        ),
        Font(
            resource = Res.font.montserrat_black,
            weight = FontWeight.Black
        ),
        Font(
            resource = Res.font.montserrat_light,
            weight = FontWeight.Light
        ),
        Font(
            resource = Res.font.montserrat_thin,
            weight = FontWeight.Thin
        ),
        Font(
            resource = Res.font.montserrat_extra_light,
            weight = FontWeight.ExtraLight
        ),
        Font(
            resource = Res.font.montserrat_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        ),
        Font(
            resource = Res.font.montserrat_medium_italic,
            weight = FontWeight.Medium,
            style = FontStyle.Italic
        ),
        Font(
            resource = Res.font.montserrat_semi_bold_italic,
            weight = FontWeight.SemiBold,
            style = FontStyle.Italic
        ),
        Font(
            resource = Res.font.montserrat_bold_italic,
            weight = FontWeight.Bold,
            style = FontStyle.Italic
        ),
        Font(
            resource = Res.font.montserrat_extra_bold_italic,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Italic
        ),
        Font(
            resource = Res.font.montserrat_black_italic,
            weight = FontWeight.Black,
            style = FontStyle.Italic
        ),
        Font(
            resource = Res.font.montserrat_light_italic,
            weight = FontWeight.Light,
            style = FontStyle.Italic
        ),
        Font(
            resource = Res.font.montserrat_thin_italic,
            weight = FontWeight.Thin,
            style = FontStyle.Italic
        ),
        Font(
            resource = Res.font.montserrat_extra_light_italic,
            weight = FontWeight.ExtraLight,
            style = FontStyle.Italic
        ),
    )
