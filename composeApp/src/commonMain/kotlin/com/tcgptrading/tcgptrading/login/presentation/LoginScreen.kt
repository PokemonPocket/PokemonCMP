package com.tcgptrading.tcgptrading.login.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexilabs.basic.ads.AdSize
import app.lexilabs.basic.ads.BannerAd
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import com.mmk.kmpauth.firebase.apple.AppleButtonUiContainer
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.apple.AppleButtonMode
import com.mmk.kmpauth.uihelper.apple.AppleSignInButton
import com.mmk.kmpauth.uihelper.google.GoogleButtonMode
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import com.tcgptrading.tcgptrading.adBannerId
import com.tcgptrading.tcgptrading.core.presentation.theme.PokemonTCGPTheme
import com.tcgptrading.tcgptrading.core.presentation.theme.montserratFontFamily
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import pokemoncmp.composeapp.generated.resources.Res
import pokemoncmp.composeapp.generated.resources.img_background

@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun LoginScreen() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BannerAd(
            adUnitId = adBannerId,
            adSize = AdSize.FULL_BANNER
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = PokemonTCGPTheme.dimensions.size05),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TCGP-Trading",
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = montserratFontFamily,
                fontWeight = FontWeight.Bold
            )

            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(Res.drawable.img_background),
                contentDescription = "",
            )

            Spacer(modifier = Modifier.height(PokemonTCGPTheme.dimensions.size05))

            GoogleButtonUiContainerFirebase(linkAccount = true, onResult = { result ->
                Napier.d { result.toString() }
            }) {
                GoogleSignInButton(
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    mode = if (isSystemInDarkTheme()) GoogleButtonMode.Dark else GoogleButtonMode.Light
                ) { this.onClick() }
            }

            Spacer(modifier = Modifier.height(PokemonTCGPTheme.dimensions.size04))

            AppleButtonUiContainer(linkAccount = true, onResult = { result ->
                Napier.d { result.toString() }
            }) {
                AppleSignInButton(
                    mode = if (isSystemInDarkTheme()) AppleButtonMode.White else AppleButtonMode.Black,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { this.onClick() }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@Preview
fun LoginScreenPreview() {
    PokemonTCGPTheme {
        LoginScreen()
    }
}