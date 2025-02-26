package com.tcgptrading.tcgptrading.app.presentation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.tcgptrading.tcgptrading.core.presentation.theme.PokemonTCGPTheme
import com.tcgptrading.tcgptrading.login.presentation.LoginScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = "304535122874-utm3ap2v4kjvcccg3jirprn8838e2p7d.apps.googleusercontent.com"))

    PokemonTCGPTheme {
        Scaffold {
            LoginScreen()
        }
    }
}