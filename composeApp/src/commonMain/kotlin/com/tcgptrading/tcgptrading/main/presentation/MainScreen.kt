package com.tcgptrading.tcgptrading.main.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.multiplatform.webview.cookie.Cookie
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.tcgptrading.tcgptrading.app.presentation.AppIntent
import com.tcgptrading.tcgptrading.main.domain.LogoutInterfaceHandler
import io.github.aakira.napier.Napier

@Composable
fun MainScreen(onAcceptIntent: (AppIntent) -> Unit, tokenId: String?) {
    val state =
        rememberWebViewState("https://tcgp-trading.com").apply {
            webSettings.isJavaScriptEnabled = true
        }

    val jsBridge = rememberWebViewJsBridge()

    WebView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        captureBackPresses = true,
    )

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

                    Napier.d(message = "cookie: ${state.cookieManager.getCookies("https://tcgp-trading.com")}")
                }
            }

            LaunchedEffect(jsBridge) {
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