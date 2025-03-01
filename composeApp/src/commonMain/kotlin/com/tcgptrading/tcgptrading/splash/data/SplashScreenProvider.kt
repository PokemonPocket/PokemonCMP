package com.tcgptrading.tcgptrading.splash.data

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.app.presentation.AppIntent
import com.tcgptrading.tcgptrading.splash.presentation.SplashScreen

fun NavGraphBuilder.provideSplashScene(onAcceptIntent: (AppIntent) -> Unit) {
    composable<Screen.Splash> {
        SplashScreen()
    }
}
