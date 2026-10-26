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
import androidx.navigation.compose.rememberNavController
import app.lexilabs.basic.ads.BasicAds
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.tcgptrading.tcgptrading.app.presentation.AppViewModel
import com.tcgptrading.tcgptrading.core.data.network.AuthConfig
import com.tcgptrading.tcgptrading.core.presentation.theme.PokemonTCGPTheme
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.localContext
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root of the app: theme, image loading, ads and Google sign-in setup,
 * then splash → sign-in → the tabbed app depending on the Firebase session.
 */
@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun App() {
    KoinContext {
        PokemonTCGPTheme {
            // Card art: cached in memory and on disk, so images load once and come back instantly
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .components { add(KtorNetworkFetcherFactory()) }
                    .crossfade(true)
                    .build()
            }

            val snackbarHostState = remember { SnackbarHostState() }
            val appViewModel = koinViewModel<AppViewModel>()
            val state by appViewModel.uiState.collectAsStateWithLifecycle()
            val navController: NavHostController = rememberNavController()

            BasicAds.initialize(localContext())
            LaunchedEffect(appViewModel) {
                GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = AuthConfig.GOOGLE_WEB_CLIENT_ID))
            }

            AppEffect(
                appViewModel = appViewModel,
                snackbarHostState = snackbarHostState,
                navController = navController,
            )

            Scaffold(
                containerColor = TcgColors.Ground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                AppNavHost(
                    navController = navController,
                    appViewModel = appViewModel,
                    paddingValues = paddingValues,
                    state = state,
                )
            }
        }
    }
}
