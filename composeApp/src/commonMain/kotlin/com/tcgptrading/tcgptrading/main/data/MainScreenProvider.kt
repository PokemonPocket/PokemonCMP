package com.tcgptrading.tcgptrading.main.data

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.app.presentation.AppIntent
import com.tcgptrading.tcgptrading.main.presentation.MainScreen

fun NavGraphBuilder.provideMainScene(onAcceptIntent: (AppIntent) -> Unit) {
    composable<Screen.Main> {
        MainScreen(onAcceptIntent = onAcceptIntent)
    }
}
