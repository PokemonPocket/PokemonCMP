package com.tcgptrading.tcgptrading.app.presentation.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tcgptrading.tcgptrading.app.presentation.AppViewModel
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.tcgptrading.tcgptrading.core.presentation.theme.PokemonTCGPTheme
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

/**
 * The `App` function is the main composable within the application.
 * It is annotated with `OptIn` and `Composable` indicating that experimental APIs are used and that it is a piece of UI.
 *
 * Functionality:
 *     - It initializes the application within `PreComposeApp`.
 *     - It includes functionality of the InfoTraffic Theme, which is defined within `InfoTrafficTheme`.
 *     - It initializes various application elements like SnackbarHostState, Navigator and AppViewModel.
 *     - Utilizes the `AppEffect` function which takes AppViewModel, SnackbarHostState, and Navigator as parameters,
 *       and facilitates side effects within the app.
 *     - Determines the current route for the navigation flow.
 *     - Defines the visual design and behavior of major sections of the application,
 *       such as the bottom navigation, Snackbar, and FloatingActionButton via `Scaffold`.
 *     - Defines the navigation host for the application via `AppNavHost`, responsible for switching destinations.
 *
 * Note: Optimization features such as 'remember' and 'collectAsStateWithLifecycle' are used throughout,
 * significantly improving performance for the application.
 */
@Composable
fun App() {
    KoinContext {
        PokemonTCGPTheme {
            val rootNavController: NavHostController = rememberNavController()
            NavHost(navController = rootNavController, startDestination = "ROOT") {
                composable(route = "ROOT") {

                    val snackbarHostState = remember { SnackbarHostState() }
                    val appViewModel = koinViewModel<AppViewModel>()
                    val state by appViewModel.uiState.collectAsStateWithLifecycle()
                    val navController: NavHostController = rememberNavController()

                    LaunchedEffect(appViewModel) {
                        GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = "304535122874-utm3ap2v4kjvcccg3jirprn8838e2p7d.apps.googleusercontent.com"))
                    }

                    AppEffect(
                        appViewModel = appViewModel,
                        snackbarHostState = snackbarHostState,
                        navController = navController,
                    )

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { paddingValues ->
                        AppNavHost(
                            navController = navController,
                            appViewModel = appViewModel,
                            paddingValues = paddingValues,
                            state = state
                        )

                    }
                }
            }
        }
    }
}
