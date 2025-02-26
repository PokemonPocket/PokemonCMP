package com.tcgptrading.tcgptrading.app.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.core.presentation.theme.PokemonTCGPTheme
import com.tcgptrading.tcgptrading.login.presentation.LoginScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = "304535122874-utm3ap2v4kjvcccg3jirprn8838e2p7d.apps.googleusercontent.com"))
    PokemonTCGPTheme {
        val rootNavController: NavHostController = rememberNavController()
        NavHost(navController = rootNavController, startDestination = "ROOT") {
            composable(route = "ROOT") {
                val snackbarHostState = remember { SnackbarHostState() }
                val navController: NavHostController = rememberNavController()

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) {
                    NavHost(navController = navController, startDestination = Screen.Home) {
                        composable<Screen.Login> {
                            LoginScreen()
                        }
                        composable<Screen.Home> {
                            val state =
                                rememberWebViewState("https://tcgp-trading.com/request-card")

                            WebView(
                                state = state,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }


            }
        }
    }
}