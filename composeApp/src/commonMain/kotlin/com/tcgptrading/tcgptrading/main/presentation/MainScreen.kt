package com.tcgptrading.tcgptrading.main.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.lexilabs.basic.ads.AdSize
import app.lexilabs.basic.ads.BannerAd
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import com.multiplatform.webview.cookie.Cookie
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.tcgptrading.tcgptrading.adBannerId
import com.tcgptrading.tcgptrading.app.presentation.AppIntent
import com.tcgptrading.tcgptrading.main.domain.LogoutInterfaceHandler
import kotlinx.coroutines.delay

@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun MainScreen(onAcceptIntent: (AppIntent) -> Unit, tokenId: String?) {
    val state =
        rememberWebViewState("https://tcgp-trading.com").apply {
            webSettings.isJavaScriptEnabled = true
        }

    val jsBridge = rememberWebViewJsBridge()
    Column(modifier = Modifier.fillMaxSize()) {
        BannerAd(adUnitId = adBannerId, adSize = AdSize.FULL_BANNER)

        WebView(
            state = state,
            modifier = Modifier.fillMaxSize(),
            captureBackPresses = true,
            webViewJsBridge = jsBridge
        )
    }



    when (state.loadingState) {
        LoadingState.Finished -> {
            LaunchedEffect(tokenId) {
                tokenId?.let {
                    state.cookieManager.setCookie(
                        "https://tcgp-trading.com", Cookie(
                            name = "firebase_token",
                            domain = "tcgp-trading.com",
                            value = it,
                        )
                    )
                }
                state.cookieManager.setCookie(
                    "https://tcgp-trading.com", Cookie(
                        name = "tcgpmobile",
                        domain = "tcgp-trading.com",
                        value = "tcgpmobile",
                    )
                )
            }

            LaunchedEffect(jsBridge) {
                delay(1000)
                jsBridge.register(LogoutInterfaceHandler(onAcceptIntent))
            }
        }

        LoadingState.Initializing -> {
            /* Do nothing */
        }

        is LoadingState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }


}