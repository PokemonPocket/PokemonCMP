package com.tcgptrading.tcgptrading.login.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmk.kmpauth.firebase.apple.AppleButtonUiContainer
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.apple.AppleButtonMode
import com.mmk.kmpauth.uihelper.apple.AppleSignInButton
import com.mmk.kmpauth.uihelper.google.GoogleButtonMode
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import com.tcgptrading.tcgptrading.core.presentation.components.BrandWordmark
import com.tcgptrading.tcgptrading.core.presentation.components.CardFan
import com.tcgptrading.tcgptrading.core.presentation.theme.PokemonTCGPTheme
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import io.github.aakira.napier.Napier
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Sign-in: what the app is for, then Google or Apple. The app opens by itself once signed in. */
@Composable
fun LoginScreen() {
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandWordmark()
            Spacer(Modifier.height(36.dp))
            CardFan(Modifier.fillMaxWidth().height(170.dp))
            Spacer(Modifier.height(36.dp))

            Text(
                "Trade the cards you are missing",
                style = MaterialTheme.typography.headlineLarge,
                color = TcgColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "List the Pokémon TCG Pocket cards you want, get offers from players who have them, and finish the trade in the game.",
                style = MaterialTheme.typography.bodyLarge,
                color = TcgColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                GoogleButtonUiContainerFirebase(linkAccount = false, onResult = { result ->
                    result.onFailure {
                        Napier.w("Google sign-in failed", it)
                        error = "Google sign-in didn't finish. Try again."
                    }
                }) {
                    GoogleSignInButton(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        mode = GoogleButtonMode.Light,
                    ) { error = null; this.onClick() }
                }

                AppleButtonUiContainer(linkAccount = false, onResult = { result ->
                    result.onFailure {
                        Napier.w("Apple sign-in failed", it)
                        error = "Apple sign-in didn't finish. Try again."
                    }
                }) {
                    AppleSignInButton(
                        mode = AppleButtonMode.Black,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { error = null; this.onClick() }
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TcgColors.RedInk, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "TCGP Trading is an independent fan app, not affiliated with The Pokémon Company or Nintendo.",
                style = MaterialTheme.typography.bodySmall,
                color = TcgColors.Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
@Preview
fun LoginScreenPreview() {
    PokemonTCGPTheme {
        LoginScreen()
    }
}
