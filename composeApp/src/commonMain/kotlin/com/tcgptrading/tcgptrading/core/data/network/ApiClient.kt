package com.tcgptrading.tcgptrading.core.data.network

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiConfig {
    /** Backend base URL (Azure). For a local backend on the Android emulator use "http://10.0.2.2:8000". */
    const val BASE_URL = "https://pokemon-d4ehczb2gngtfyd9.polandcentral-01.azurewebsites.net"

    /** STOMP endpoint (Spring registers it with SockJS; the raw WebSocket lives under /websocket). */
    val CHAT_WS_URL: String
        get() = BASE_URL.replaceFirst("http", "ws") + "/chat-websocket/websocket"

    const val WEBSITE_URL = "https://tcgp-trading.com"

    /** Card art on Cloudflare (Workers static assets): /s/<driveFileId>.webp (360px) and /l/<driveFileId>.webp (600px). */
    const val CARD_IMAGES_URL = "https://tcgp-cards.asar-innovations.workers.dev"
}

object AuthConfig {
    /**
     * OAuth "Web client" ID of the Firebase project used for sign-in (tcgp-trading-web).
     * Firebase console > Authentication > Sign-in method > Google > Web SDK configuration > Web client ID,
     * or client_type 3 in google-services.json. Must belong to the same project as the app config files.
     */
    const val GOOGLE_WEB_CLIENT_ID = "291535864511-omh6crtgs6kl3297klti60gdupcqdp0c.apps.googleusercontent.com"
}

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

/** Current Firebase ID token, refreshed by Firebase when it is about to expire; null when signed out. */
suspend fun currentIdToken(): String? = runCatching {
    Firebase.auth.currentUser?.getIdToken(false)
}.onFailure { Napier.w("Could not get ID token", it) }.getOrNull()

/** Adds "Authorization: Bearer <token>" to every request when signed in. */
private val FirebaseAuthPlugin = createClientPlugin("FirebaseAuth") {
    onRequest { request, _ ->
        if (!request.headers.contains(HttpHeaders.Authorization)) {
            currentIdToken()?.let { request.headers.append(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}

fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = true // non-2xx responses throw, repositories turn them into messages
    install(ContentNegotiation) { json(AppJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 15_000
    }
    install(WebSockets)
    install(Logging) {
        level = LogLevel.INFO
        logger = object : Logger {
            override fun log(message: String) = Napier.v(message, tag = "HTTP")
        }
    }
    install(FirebaseAuthPlugin)
    defaultRequest { url(ApiConfig.BASE_URL + "/") }
}
