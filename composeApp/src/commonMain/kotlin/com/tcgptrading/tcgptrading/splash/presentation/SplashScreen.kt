package com.tcgptrading.tcgptrading.splash.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tcgptrading.tcgptrading.core.presentation.components.BrandWordmark
import com.tcgptrading.tcgptrading.core.presentation.theme.PokemonTCGPTheme
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Shown while the app checks whether the player is signed in. */
@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            BrandWordmark(markSize = 40.dp, fontSize = 28)
            CircularProgressIndicator(color = TcgColors.Red, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    PokemonTCGPTheme { SplashScreen() }
}
