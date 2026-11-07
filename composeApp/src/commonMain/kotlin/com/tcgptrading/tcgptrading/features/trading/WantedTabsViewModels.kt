package com.tcgptrading.tcgptrading.features.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.core.data.Session
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.isConflict
import com.tcgptrading.tcgptrading.core.data.model.OfferCreate
import com.tcgptrading.tcgptrading.core.data.model.TradeLine
import com.tcgptrading.tcgptrading.core.data.model.WantedCard
import com.tcgptrading.tcgptrading.core.data.model.WantedTrader
import com.tcgptrading.tcgptrading.core.data.model.CardDemand
import com.tcgptrading.tcgptrading.core.data.model.CardMarket
import com.tcgptrading.tcgptrading.core.data.model.ForYou
import com.tcgptrading.tcgptrading.core.data.model.WantedTraders
import com.tcgptrading.tcgptrading.core.data.runApi
import com.tcgptrading.tcgptrading.core.data.serverMessage
import com.tcgptrading.tcgptrading.core.util.playerLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Coming back to a tab reloads it, unless it loaded this recently (e.g. it just opened). */
internal val RESUME_RELOAD_GAP = 5.seconds

/**
 * When a tab that reads the collection should load again: back on screen, or after an offer went out.
 * Skips a resume while a load is running or one just finished, unless an offer was sent since.
 */
internal class ReloadPolicy {
    private var loadedAt: TimeSource.Monotonic.ValueTimeMark? = null
    var stale: Boolean = false

    fun loaded() {
        loadedAt = TimeSource.Monotonic.markNow()
    }

    fun shouldReloadOnResume(loading: Boolean): Boolean = when {
        stale -> true
        loading -> false
        else -> loadedAt?.let { it.elapsedNow() >= RESUME_RELOAD_GAP } ?: true
    }
}

// ---------- For you ----------

data class ForYouState(
    val data: ForYou? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    /** Loading again in the background while the old matches stay on screen. */
    val reloading: Boolean = false,
    val error: String? = null,
)

/** Wanted › For you: wants I can fill with my spares, two-way swaps first. */
class ForYouViewModel(private val api: TradingApi, signals: TradingSignals) : ViewModel() {
    private val _state = MutableStateFlow(ForYouState())
    val state: StateFlow<ForYouState> = _state.asStateFlow()

    private var job: Job? = null
    private val reload = ReloadPolicy()

    init {
        load()
        // An offer sent from Traders or the offer page takes that want off this list
        viewModelScope.launch { signals.offerSent.drop(1).collect { reload.stale = true } }
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load()
    }

    fun retry() = load()

    /** The tab is on screen again (switching tabs, or back from another screen). */
    fun onResume() {
        if (reload.shouldReloadOnResume(job?.isActive == true)) load()
    }

    private fun load() {
        reload.stale = false
        job?.cancel()
        _state.update {
            it.copy(loading = !it.refreshing && it.data == null, reloading = !it.refreshing && it.data != null, error = null)
        }
        job = viewModelScope.launch {
            runApi { api.wantedForYou() }
                .onSuccess { d ->
                    reload.loaded()
                    _state.update { it.copy(data = d, loading = false, refreshing = false, reloading = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, refreshing = false, reloading = false, error = e.message ?: "Could not load your matches.")
                    }
                }
        }
    }
}

// ---------- Traders ----------

data class TradersState(
    val data: WantedTraders? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    /** Loading again in the background while the old list stays on screen. */
    val reloading: Boolean = false,
    val error: String? = null,
    /** Suggested cards left out, per trade line (requestId); everything suggested starts picked. */
    val unpicked: Map<String, Set<String>> = emptyMap(),
    val sending: Set<String> = emptySet(),
    /** Trade lines with an offer sent from here; the server leaves them out on the next load. */
    val sent: Set<String> = emptySet(),
    /** Of those, the ones the server refused, with what it said (e.g. an offer on that want already existed). */
    val refused: Map<String, String> = emptyMap(),
)

/** The suggested cards still picked on a trade line: at most [MAX_PICKS], in the server's order. */
fun pickedCards(line: TradeLine, unpicked: Set<String>): List<WantedCard> =
    line.suggest.orEmpty().take(MAX_PICKS).map { it.card }.filter { it.cardId !in unpicked }

/** Wanted › Traders: players who want 2+ of my spares, newest want first. Offers go out from here. */
class TradersViewModel(
    private val api: TradingApi,
    private val session: Session,
    private val signals: TradingSignals,
) : ViewModel() {
    private val _state = MutableStateFlow(TradersState())
    val state: StateFlow<TradersState> = _state.asStateFlow()

    private var job: Job? = null
    private val reload = ReloadPolicy()

    init {
        load()
        viewModelScope.launch { signals.offerSent.drop(1).collect { reload.stale = true } }
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load()
    }

    fun retry() = load()

    /** The tab is on screen again (switching tabs, or back from another screen). */
    fun onResume() {
        if (reload.shouldReloadOnResume(job?.isActive == true)) load()
    }

    fun togglePick(line: TradeLine, cardId: String) = _state.update { s ->
        val off = s.unpicked[line.requestId].orEmpty()
        s.copy(unpicked = s.unpicked + (line.requestId to if (cardId in off) off - cardId else off + cardId))
    }

    /**
     * The cards are already chosen, so the offer goes out from here instead of the offer page.
     * Call it through the Friend ID gate: the ID is read now, after the gate has set it.
     */
    fun send(trader: WantedTrader, line: TradeLine) {
        val s = _state.value
        val picked = pickedCards(line, s.unpicked[line.requestId].orEmpty())
        val bidder = session.friendId ?: return
        if (picked.isEmpty() || line.requestId in s.sending) return
        _state.update { it.copy(sending = it.sending + line.requestId) }
        viewModelScope.launch {
            runApi {
                api.makeOffer(
                    OfferCreate(
                        requesterFriendId = trader.userFriendId,
                        bidderFriendId = bidder,
                        cardId = line.give.cardId,
                        requestId = line.requestId,
                        requestedCards = picked.map { it.name },
                        requestedCardIds = picked.map { it.cardId },
                    )
                )
            }.onSuccess {
                _state.update { it.copy(sending = it.sending - line.requestId, sent = it.sent + line.requestId) }
                signals.offerSent()
                signals.notify(TradeNotice("Offer sent to ${playerLabel(trader.userFriendId)}.", seeOffers = true))
            }.onFailure { e ->
                if (e.isConflict()) {
                    // Already offered, or already traded: this line is done either way
                    val said = e.serverMessage ?: ALREADY_OFFERED
                    _state.update {
                        it.copy(sending = it.sending - line.requestId, sent = it.sent + line.requestId, refused = it.refused + (line.requestId to said))
                    }
                    signals.notify(TradeNotice(said, seeOffers = true))
                } else {
                    _state.update { it.copy(sending = it.sending - line.requestId) }
                    signals.notify(TradeNotice(e.message ?: "Could not send the offer. Try again."))
                }
            }
        }
    }

    private fun load() {
        reload.stale = false
        job?.cancel()
        _state.update {
            it.copy(loading = !it.refreshing && it.data == null, reloading = !it.refreshing && it.data != null, error = null)
        }
        job = viewModelScope.launch {
            runApi { api.wantedTraders() }
                .onSuccess { d ->
                    reload.loaded()
                    _state.update {
                        it.copy(data = d, loading = false, refreshing = false, reloading = false, sent = emptySet(), refused = emptyMap())
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, refreshing = false, reloading = false, error = e.message ?: "Could not load traders.") }
                }
        }
    }
}


// ---------- By card ----------

data class ByCardState(
    /** "newest" (latest want first) or "popular" (most players first). */
    val sort: String = "newest",
    val cards: List<CardDemand> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val loadMoreFailed: Boolean = false,
    /** The card whose players are open in the sheet. */
    val selected: CardDemand? = null,
    val market: CardMarket? = null,
    val marketLoading: Boolean = false,
    val marketError: String? = null,
) {
    val canLoadMore: Boolean get() = page + 1 < totalPages
}

/** Wanted › By card: one row per card. Opening one lists who wants it, newest first. */
class ByCardViewModel(private val api: TradingApi) : ViewModel() {
    private val _state = MutableStateFlow(ByCardState())
    val state: StateFlow<ByCardState> = _state.asStateFlow()

    private var filters: WantedFilters? = null
    private var job: Job? = null
    private var marketJob: Job? = null

    /** Called with the Wanted screen's applied filters; reloads only when they changed. */
    fun apply(filters: WantedFilters) {
        if (this.filters == filters) return
        this.filters = filters
        load(reset = true)
    }

    fun setSort(sort: String) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort) }
        load(reset = true)
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load(reset = true)
    }

    fun retry() = load(reset = true)

    fun loadMore() {
        val s = _state.value
        // After a failed reload the list still shows the old filter: a next page would mix the two
        if (s.loading || s.loadingMore || !s.canLoadMore || s.error != null) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        val f = filters ?: return
        if (reset) job?.cancel() else if (job?.isActive == true) return
        val s = _state.value
        val page = if (reset) 0 else s.page + 1
        _state.update {
            // A reset also ends a cancelled "load more", so its skeleton can't stay on screen
            if (reset) it.copy(loading = !it.refreshing, loadingMore = false, error = null, loadMoreFailed = false)
            else it.copy(loadingMore = true, loadMoreFailed = false)
        }
        job = viewModelScope.launch {
            runApi {
                api.wantedByCard(
                    page = page, sort = s.sort, cardName = f.query.ifBlank { null },
                    rarity = f.rarity, expansion = f.expansion, language = f.language, spareOnly = f.spareOnly,
                )
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        cards = (if (reset) result.content else it.cards + result.content).distinctBy { c -> c.cardId },
                        page = page,
                        totalPages = result.totalPages,
                        totalElements = result.totalElements,
                        loading = false, refreshing = false, loadingMore = false, error = null,
                    )
                }
            }.onFailure { e ->
                _state.update {
                    // With cards on screen they stay, and the error shows above them
                    if (reset) it.copy(loading = false, refreshing = false, error = e.message ?: "Could not load the cards.")
                    else it.copy(loadingMore = false, loadMoreFailed = true)
                }
            }
        }
    }

    fun open(card: CardDemand) {
        _state.update { it.copy(selected = card, market = null, marketLoading = true, marketError = null) }
        marketJob?.cancel()
        marketJob = viewModelScope.launch {
            runApi { api.cardMarket(card.cardId) }
                .onSuccess { m -> _state.update { it.copy(market = m, marketLoading = false) } }
                .onFailure { e -> _state.update { it.copy(marketLoading = false, marketError = e.message ?: "Could not load who wants it.") } }
        }
    }

    fun retryMarket() = _state.value.selected?.let(::open)

    fun close() {
        marketJob?.cancel()
        _state.update { it.copy(selected = null, market = null, marketLoading = false, marketError = null) }
    }
}
