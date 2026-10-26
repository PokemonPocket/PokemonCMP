package com.tcgptrading.tcgptrading.features.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.app.domain.Detail
import com.tcgptrading.tcgptrading.core.data.Session
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.model.Card
import com.tcgptrading.tcgptrading.core.data.model.OfferCreate
import com.tcgptrading.tcgptrading.core.data.runApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cards a bidder can ask for in return: at most this many. */
const val MAX_PICKS = 5

data class MakeOfferState(
    val wanted: Card? = null,
    val wantedLoading: Boolean = true,
    val wantedError: String? = null,
    val query: String = "",
    val cards: List<Card> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val loadMoreFailed: Boolean = false,
    /** Picked cards in the order they were picked. */
    val picked: List<Card> = emptyList(),
    val sending: Boolean = false,
) {
    val canLoadMore: Boolean get() = page + 1 < totalPages
}

sealed interface MakeOfferEvent {
    data class Message(val text: String) : MakeOfferEvent
    data object Sent : MakeOfferEvent
}

/**
 * Offer a trade on someone's want: the bidder gives the wanted card and picks up to 5 cards
 * of the same rarity they would like back. The collector then chooses one of them.
 */
@OptIn(FlowPreview::class)
class MakeOfferViewModel(
    private val route: Detail.MakeOffer,
    private val api: TradingApi,
    private val session: Session,
) : ViewModel() {

    private val _state = MutableStateFlow(MakeOfferState())
    val state: StateFlow<MakeOfferState> = _state.asStateFlow()

    private val _events = Channel<MakeOfferEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var cardsJob: Job? = null

    init {
        loadWanted()
        viewModelScope.launch {
            _state.map { it.query.trim() }
                .distinctUntilChanged()
                .drop(1)
                .debounce(350)
                .collect { loadCards(reset = true) }
        }
    }

    fun loadWanted() {
        _state.update { it.copy(wantedLoading = true, wantedError = null) }
        viewModelScope.launch {
            runApi { api.card(route.cardId) }
                .onSuccess { card ->
                    _state.update { it.copy(wanted = card, wantedLoading = false) }
                    loadCards(reset = true)
                }
                .onFailure { e -> _state.update { it.copy(wantedLoading = false, wantedError = e.message) } }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun retryCards() = loadCards(reset = true)

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.canLoadMore) return
        loadCards(reset = false)
    }

    private fun loadCards(reset: Boolean) {
        val wanted = _state.value.wanted ?: return
        // Same rarity only: Pocket only trades cards of matching rarity
        val rarity = wanted.rarityName ?: wanted.rarity ?: return
        if (reset) cardsJob?.cancel() else if (cardsJob?.isActive == true) return
        val s = _state.value
        val page = if (reset) 0 else s.page + 1
        _state.update {
            if (reset) it.copy(loading = true, error = null, loadMoreFailed = false)
            else it.copy(loadingMore = true, loadMoreFailed = false)
        }
        cardsJob = viewModelScope.launch {
            runApi { api.cardsOfRarity(rarity, s.query.trim(), page) }
                .onSuccess { result ->
                    // Asking for the card you give makes no sense, so it is left out
                    val content = result.content.filter { it.cardId != wanted.cardId }
                    _state.update {
                        it.copy(
                            cards = if (reset) content else it.cards + content,
                            page = page,
                            totalPages = result.page.totalPages,
                            loading = false, loadingMore = false,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        if (reset) it.copy(loading = false, error = e.message ?: "Could not load cards.")
                        else it.copy(loadingMore = false, loadMoreFailed = true)
                    }
                }
        }
    }

    fun toggle(card: Card) {
        val picked = _state.value.picked
        when {
            picked.any { it.cardId == card.cardId } ->
                _state.update { it.copy(picked = picked.filterNot { p -> p.cardId == card.cardId }) }
            picked.size >= MAX_PICKS ->
                _events.trySend(MakeOfferEvent.Message("You can pick up to $MAX_PICKS cards."))
            else -> _state.update { it.copy(picked = picked + card) }
        }
    }

    fun send() {
        val s = _state.value
        if (s.sending || s.picked.isEmpty()) return
        val bidder = session.friendId
        if (bidder == null) {
            _events.trySend(MakeOfferEvent.Message("Add your Friend ID first."))
            return
        }
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            runApi {
                api.makeOffer(
                    OfferCreate(
                        requesterFriendId = route.requesterFriendId,
                        bidderFriendId = bidder,
                        cardId = route.cardId,
                        requestId = route.requestId,
                        requestedCards = s.picked.map { it.name },
                        // Exact cards: names repeat across sets
                        requestedCardIds = s.picked.map { it.cardId },
                    )
                )
            }.onSuccess {
                _state.update { it.copy(sending = false) }
                _events.send(MakeOfferEvent.Message("Offer sent"))
                _events.send(MakeOfferEvent.Sent)
            }.onFailure { e ->
                _state.update { it.copy(sending = false) }
                _events.send(MakeOfferEvent.Message(e.message ?: "Could not send the offer. Try again."))
            }
        }
    }
}
