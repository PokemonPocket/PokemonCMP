package com.tcgptrading.tcgptrading.app.presentation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.app.presentation.AppState
import com.tcgptrading.tcgptrading.app.presentation.AppViewModel
import com.tcgptrading.tcgptrading.login.data.di.provideLoginScene
import com.tcgptrading.tcgptrading.main.data.provideMainScene
import com.tcgptrading.tcgptrading.splash.data.provideSplashScene

/**
 * This Kotlin function 'AppNavHost' is a composable function that sets up the navigation host for a mobile application.
 * It configures the initial navigation stack, and applies navigation transitions to navigation actions.
 *
 * @param navController        The Navigator object drives navigation by keeping a back stack of Composables.
 * @param appViewModel     The AppViewModel dictates the behaviour of the application based on the user's interaction.
 */

@Composable
fun AppNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel,
    paddingValues: PaddingValues,
    state: AppState,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash,
        modifier = Modifier.padding(paddingValues)
            .consumeWindowInsets(paddingValues)
            .imePadding(),
    ) {
        provideSplashScene(onAcceptIntent = appViewModel::acceptIntent)
        provideLoginScene(onAcceptIntent = appViewModel::acceptIntent)
        provideMainScene(onAcceptIntent = appViewModel::acceptIntent, tokenId = state.tokenId)
    }
}
