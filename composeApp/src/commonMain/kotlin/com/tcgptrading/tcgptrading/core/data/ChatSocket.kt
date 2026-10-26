package com.tcgptrading.tcgptrading.core.data

import com.tcgptrading.tcgptrading.core.data.model.ChatMessage
import com.tcgptrading.tcgptrading.core.data.model.OutgoingChatMessage
import com.tcgptrading.tcgptrading.core.data.model.ReadReceipt
import com.tcgptrading.tcgptrading.core.data.network.AppJson
import com.tcgptrading.tcgptrading.core.data.network.ApiConfig
import com.tcgptrading.tcgptrading.core.data.network.currentIdToken
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

/**
 * Live chat over STOMP, same protocol as the website.
 * The server identifies the user from the Firebase ID token sent on CONNECT,
 * so this only connects while signed in, and reconnects with a fresh token when the link drops.
 */
class ChatSocket(httpClient: HttpClient) {

    private val stomp = StompClient(KtorWebSocketClient(httpClient))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var session: StompSession? = null

    private val _messages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    /** Every message to or from me while connected (my own sends are echoed back by the server). */
    val messages: SharedFlow<ChatMessage> = _messages.asSharedFlow()

    private val _receipts = MutableSharedFlow<ReadReceipt>(extraBufferCapacity = 16)
    /** "readBy" email when someone read my messages. */
    val receipts: SharedFlow<ReadReceipt> = _receipts.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /** Starts the connection loop. Safe to call more than once. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var backoff = 2_000L
            while (isActive) {
                val token = currentIdToken()
                if (token == null) {
                    delay(5_000)
                    continue
                }
                try {
                    val s = stomp.connect(
                        url = ApiConfig.CHAT_WS_URL,
                        customStompConnectHeaders = mapOf("Authorization" to "Bearer $token"),
                    )
                    session = s
                    _connected.value = true
                    backoff = 2_000L

                    val receiptsJob = launch {
                        s.subscribeText("/user/queue/read-receipt").collect { body ->
                            runCatching { AppJson.decodeFromString(ReadReceipt.serializer(), body) }
                                .onSuccess { _receipts.tryEmit(it) }
                        }
                    }
                    // Blocks until the connection ends
                    s.subscribeText("/user/queue/messages").collect { body ->
                        runCatching { AppJson.decodeFromString(ChatMessage.serializer(), body) }
                            .onSuccess { _messages.tryEmit(it) }
                    }
                    receiptsJob.cancel()
                } catch (e: Exception) {
                    if (!isActive) break
                    Napier.w("Chat connection lost", e, tag = "Chat")
                } finally {
                    _connected.value = false
                    session = null
                }
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }

    /** Sends a message. Returns false when not connected, so the screen can say so. */
    suspend fun send(sender: String, recipient: String, text: String): Boolean {
        val s = session ?: return false
        return runCatching {
            s.sendText(
                "/app/private-chat",
                AppJson.encodeToString(OutgoingChatMessage.serializer(), OutgoingChatMessage(sender, recipient, text)),
            )
        }.isSuccess
    }

    /** Stops the connection, e.g. on sign-out. */
    fun stop() {
        job?.cancel()
        job = null
        scope.launch { runCatching { session?.disconnect() } }
        session = null
        _connected.value = false
    }
}
