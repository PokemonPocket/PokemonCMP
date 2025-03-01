package com.tcgptrading.tcgptrading.login.data.di

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.app.presentation.AppIntent
import com.tcgptrading.tcgptrading.login.presentation.LoginScreen

fun NavGraphBuilder.provideLoginScene(onAcceptIntent: (AppIntent) -> Unit) {
    composable<Screen.Login> {
        LoginScreen()
    }
}
