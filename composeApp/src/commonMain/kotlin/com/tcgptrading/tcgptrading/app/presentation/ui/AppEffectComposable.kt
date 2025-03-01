package com.tcgptrading.tcgptrading.app.presentation.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.navOptions
import com.tcgptrading.tcgptrading.app.presentation.AppViewModel
import com.tcgptrading.tcgptrading.app.presentation.AppEffect
import kotlinx.coroutines.launch

/**
 * `AppEffect` is a composable function that renders UI based on the state changes emitted by `appViewModel.effect`.
 * It displays snackbar when the app effect is `ShowSnackbar` and calls navigation operations when the effect is `NavigateToScreen`.
 *
 * @param appViewModel     The ViewModel that exposes the state changes to be consumed by the UI.
 * @param snackbarHostState The HostState used to present Snackbar to the user.
 * @param navController        Used for carrying out navigation in the application.
 */

@Composable
fun AppEffect(
    appViewModel: AppViewModel,
    snackbarHostState: SnackbarHostState,
    navController: NavHostController
) {
    LaunchedEffect(appViewModel) {
        appViewModel.effect.collect {
            when (it) {
                is AppEffect.ShowSnackbar -> {
                    launch {
                        val snackbarResult =
                            snackbarHostState.showSnackbar(message = it.snackbarUiModel.message)
                        when (snackbarResult) {
                            SnackbarResult.Dismissed -> {
                                //TODO()
                            }

                            SnackbarResult.ActionPerformed -> {
                                //TODO()
                            }
                        }
                    }
                }

                is AppEffect.NavigateToScreen -> {
                    navController.navigate(
                        route = it.route,
                        navOptions {
                            launchSingleTop = true
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        },
                    )
                }

            }
        }
    }
}
