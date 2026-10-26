package com.tcgptrading.tcgptrading.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.core.data.ChatSocket
import com.tcgptrading.tcgptrading.core.data.Session
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.model.ChatContact
import com.tcgptrading.tcgptrading.core.data.model.ChatMessage
import com.tcgptrading.tcgptrading.core.data.model.TradeSummary
import com.tcgptrading.tcgptrading.core.data.runApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

// ---------------------------------------------------------------------------
// Chat list
// ---------------------------------------------------------------------------

data class ChatListState(
    val loading: Boolean = true,
    val error: String? = null,
    val chats: List<ChatContact> = emptyList(),
    val query: String = "",
)

class ChatListViewModel(
    private val api: TradingApi,
    socket: ChatSocket,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        load()
        // New messages change previews and unread counts
        viewModelScope.launch { socket.messages.collect { load(silent = true) } }
    }

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.update { it.copy(loading = it.chats.isEmpty(), error = null) }
            runApi { api.chats() }
                .onSuccess { list -> _state.update { it.copy(loading = false, error = null, chats = list) } }
                .onFailure { e ->
                    if (!silent || _state.value.chats.isEmpty()) {
                        _state.update { it.copy(loading = false, error = e.message) }
                    }
                }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }
}

// ---------------------------------------------------------------------------
// Conversation
// ---------------------------------------------------------------------------

data class UiMessage(
    val key: String,
    val text: String,
    val time: String?,
    val mine: Boolean,
    val read: Boolean,
    /** Sent from this device, waiting for the server echo. */
    val pending: Boolean = false,
)

data class ConversationState(
    val loading: Boolean = true,
    val error: String? = null,
    val messages: List<UiMessage> = emptyList(),
    val trade: TradeSummary? = null,
    val moreTrades: Int = 0,
    val online: Boolean = false,
    val lastActive: String? = null,
    val completing: Boolean = false,
    val sendError: String? = null,
)

class ConversationViewModel(
    val peerEmail: String,
    private val api: TradingApi,
    private val socket: ChatSocket,
    private val session: Session,
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    val connected: StateFlow<Boolean> = socket.connected

    private var localCounter = 0

    private val me: String? get() = session.email

    init {
        load()
        loadTrade()
        viewModelScope.launch { socket.messages.collect(::onLive) }
        viewModelScope.launch {
            socket.receipts.collect { receipt ->
                if (receipt.readBy.equals(peerEmail, ignoreCase = true)) {
                    _state.update { s -> s.copy(messages = s.messages.map { if (it.mine) it.copy(read = true) else it }) }
                }
            }
        }
        // Online status while the conversation is open
        viewModelScope.launch {
            while (isActive) {
                runApi { api.status(peerEmail) }.onSuccess { st ->
                    _state.update { it.copy(online = st.online, lastActive = st.lastActiveAt.ifBlank { null }) }
                }
                delay(15_000)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            val myEmail = me ?: session.refresh().getOrNull()?.email
            if (myEmail.isNullOrBlank()) {
                _state.update { it.copy(loading = false, error = "Please sign in again.") }
                return@launch
            }
            _state.update { it.copy(loading = it.messages.isEmpty(), error = null) }
            runApi { api.messages(myEmail, peerEmail) }
                .onSuccess { rows ->
                    val list = rows.sortedBy { it.creationDate.orEmpty() }.mapIndexed { i, m -> m.toUi(myEmail, i) }
                    _state.update { it.copy(loading = false, messages = list) }
                    markRead()
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun loadTrade() {
        viewModelScope.launch {
            runApi { api.tradesWith(peerEmail) }.onSuccess { trades ->
                _state.update { it.copy(trade = trades.firstOrNull(), moreTrades = (trades.size - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun completeTrade() {
        val trade = _state.value.trade ?: return
        if (_state.value.completing) return
        viewModelScope.launch {
            _state.update { it.copy(completing = true) }
            runApi { api.completeTrade(trade.offerId) }
                .onSuccess { loadTrade() }
                .onFailure { e -> _state.update { it.copy(sendError = e.message) } }
            _state.update { it.copy(completing = false) }
        }
    }

    fun send(text: String): Boolean {
        val body = text.trim()
        val myEmail = me ?: return false
        if (body.isEmpty()) return false
        val local = UiMessage(
            key = "local-${localCounter++}", text = body, time = Clock.System.now().toString(),
            mine = true, read = false, pending = true,
        )
        _state.update { it.copy(messages = it.messages + local, sendError = null) }
        viewModelScope.launch {
            val ok = socket.send(myEmail, peerEmail, body)
            if (!ok) {
                _state.update { s ->
                    s.copy(
                        messages = s.messages.filterNot { it.key == local.key },
                        sendError = "Reconnecting… Your message was not sent. Try again in a moment.",
                    )
                }
            }
        }
        return true
    }

    fun clearSendError() = _state.update { it.copy(sendError = null) }

    private fun onLive(m: ChatMessage) {
        val myEmail = me ?: return
        val fromPeer = m.sender.equals(peerEmail, true) && m.recipient.equals(myEmail, true)
        val fromMe = m.sender.equals(myEmail, true) && m.recipient.equals(peerEmail, true)
        if (!fromPeer && !fromMe) return

        _state.update { s ->
            if (fromMe) {
                // Server echo of my own message: replace the optimistic copy
                val idx = s.messages.indexOfFirst { it.pending && it.text == m.message }
                val ui = m.toUi(myEmail, localCounter++)
                if (idx >= 0) s.copy(messages = s.messages.toMutableList().also { it[idx] = ui })
                else s.copy(messages = s.messages + ui)
            } else {
                s.copy(messages = s.messages + m.toUi(myEmail, localCounter++))
            }
        }
        if (fromPeer) markRead()
    }

    private fun markRead() {
        viewModelScope.launch { runApi { api.markRead(peerEmail) } }
    }

    private fun ChatMessage.toUi(myEmail: String, index: Int) = UiMessage(
        key = id ?: "m-$index-${creationDate.orEmpty()}",
        text = message,
        time = creationDate,
        mine = sender.equals(myEmail, true),
        read = isRead == true,
    )
}
