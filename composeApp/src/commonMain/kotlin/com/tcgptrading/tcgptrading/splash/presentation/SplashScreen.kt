package com.tcgptrading.tcgptrading.splash.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import pokemoncmp.composeapp.generated.resources.Res
import pokemoncmp.composeapp.generated.resources.img_splash


@Composable
fun SplashScreen() {
    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(Res.drawable.img_splash),
        contentDescription = "Splash Screen"
    )

}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}