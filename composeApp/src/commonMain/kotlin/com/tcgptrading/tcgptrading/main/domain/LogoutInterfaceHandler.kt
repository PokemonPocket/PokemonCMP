package com.tcgptrading.tcgptrading.main.domain

import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.web.WebViewNavigator
import com.tcgptrading.tcgptrading.app.presentation.AppIntent

class LogoutInterfaceHandler(val onAcceptIntent: (AppIntent) -> Unit) : IJsMessageHandler {
    override fun handle(
        message: JsMessage,
        navigator: WebViewNavigator?,
        callback: (String) -> Unit
    ) {
        onAcceptIntent(AppIntent.LogoutUser)
    }

    override fun methodName(): String = "MobileLogout"

}