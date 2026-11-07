package com.tcgptrading.tcgptrading.features.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.model.FeedGroup
import com.tcgptrading.tcgptrading.core.data.model.TradeRequest
import com.tcgptrading.tcgptrading.core.data.runApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The Wanted screen's tabs. Every list in them is newest first. */
enum class WantedTab(val label: String) {
    ForYou("For you"),
    ByCard("By card"),
    Traders("Traders"),
    All("All wants"),
}

/** Search and filters shared by All wants and By card. */
data class WantedFilters(
    val query: String = "",
    /** Backend Rarity code (C, U, R, RR, AR, SR, IM, UR), null = all. */
    val rarity: String? = null,
    /** Backend Expansion code (A1, A1a…), null = all. */
    val expansion: String? = null,
    /** Backend Language name, null = any. */
    val language: String? = null,
    /** Only cards I have a spare of. */
    val spareOnly: Boolean = false,
) {
    val active: Boolean get() = query.isNotBlank() || rarity != null || expansion != null || language != null || spareOnly
}

/** One tile on All wants: a want, or the rest of a player's same-day wants folded into one. */
sealed interface FeedTile {
    val key: String

    data class Want(val request: TradeRequest) : FeedTile {
        override val key: String get() = request.requestId
    }

    data class More(val friendId: String?, val latest: String?, val more: Int) : FeedTile {
        override val key: String get() = "more-$friendId-$latest"
    }
}

data class WantedState(
    /** By card first: it is useful right away, even before a player has filled in their collection. */
    val tab: WantedTab = WantedTab.ByCard,
    /** What the filter controls show (the search as typed). */
    val filters: WantedFilters = WantedFilters(),
    /** What the lists were loaded with: the search is applied once typing stops. */
    val applied: WantedFilters = WantedFilters(),
    /** Set when All wants shows one player's list. */
    val friendId: String? = null,
    /** My collection (cardId -> copies), to mark the wants I can fill. */
    val counts: Map<String, Int> = emptyMap(),
    val tiles: List<FeedTile> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val loadedOnce: Boolean = false,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val loadMoreFailed: Boolean = false,
) {
    val hasFilters: Boolean get() = applied.active || friendId != null
    val canLoadMore: Boolean get() = page + 1 < totalPages

    /** Copies of this card I can give away (beyond the one I keep). */
    fun sparesOf(cardId: String): Int = ((counts[cardId] ?: 0) - 1).coerceAtLeast(0)
}

/** The Wanted screen: which tab is open, the shared filters, and the All wants list. */
@OptIn(FlowPreview::class)
class WantedViewModel(private val api: TradingApi, private val searchRequests: WantedSearchRequests) : ViewModel() {

    private val _state = MutableStateFlow(WantedState())
    val state: StateFlow<WantedState> = _state.asStateFlow()

    private var job: Job? = null
    // Declared before init, which loads the collection
    private var collectionJob: Job? = null
    private val collectionReload = ReloadPolicy()

    init {
        loadCollection()
        // Search waits until typing stops, then starts again from the first page
        viewModelScope.launch {
            _state.map { it.filters.query.trim() }
                .distinctUntilChanged()
                .drop(1)
                .debounce(350)
                .collect { q ->
                    if (_state.value.applied.query == q) return@collect
                    _state.update { it.copy(applied = it.applied.copy(query = q)) }
                    reloadAllIfOpen()
                }
        }
        // Another tab asked to show wants for a specific card: All wants, searched, no other filters
        viewModelScope.launch {
            searchRequests.pending.filterNotNull().collect { search ->
                searchRequests.consume()
                val filters = WantedFilters(query = search)
                _state.update { it.copy(tab = WantedTab.All, filters = filters, applied = filters, friendId = null) }
                load(reset = true)
            }
        }
    }

    fun refreshCollection() = loadCollection()

    /** Back on the Wanted screen: the collection may have changed in the Collection tab, so the spare marks reload. */
    fun onResume() {
        if (collectionReload.shouldReloadOnResume(collectionJob?.isActive == true)) loadCollection()
    }

    private fun loadCollection() {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            runApi { api.collection() }.onSuccess { c ->
                collectionReload.loaded()
                _state.update { it.copy(counts = c.cards) }
            }
        }
    }

    // ---------- Tabs ----------

    fun setTab(tab: WantedTab) {
        if (_state.value.tab == tab) return
        _state.update { it.copy(tab = tab) }
        if (tab == WantedTab.All && !_state.value.loadedOnce) load(reset = true)
    }

    /** From Traders: All wants on one player's list. */
    fun showPlayer(friendId: String) {
        _state.update { it.copy(tab = WantedTab.All, friendId = friendId) }
        load(reset = true)
    }

    fun clearPlayer() {
        _state.update { it.copy(friendId = null) }
        load(reset = true)
    }

    // ---------- Filters ----------

    fun setQuery(query: String) = _state.update { it.copy(filters = it.filters.copy(query = query)) }

    fun setRarity(rarity: String?) = applyNow { it.copy(rarity = rarity) }

    fun setExpansion(expansion: String?) = applyNow { it.copy(expansion = expansion) }

    fun setLanguage(language: String?) = applyNow { it.copy(language = language) }

    fun toggleSpareOnly() = applyNow { it.copy(spareOnly = !it.spareOnly) }

    fun clearFilters() {
        _state.update { it.copy(filters = WantedFilters(), applied = WantedFilters(), friendId = null) }
        reloadAllIfOpen()
    }

    private fun applyNow(change: (WantedFilters) -> WantedFilters) {
        val s = _state.value
        val next = change(s.filters)
        if (next == s.filters) return
        _state.update { it.copy(filters = next, applied = next.copy(query = next.query.trim())) }
        reloadAllIfOpen()
    }

    private fun reloadAllIfOpen() {
        // By card watches `applied` itself; All wants loads here, or the first time it opens
        if (_state.value.tab == WantedTab.All) load(reset = true) else _state.update { it.copy(loadedOnce = false) }
    }

    // ---------- All wants ----------

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        loadCollection()
        load(reset = true)
    }

    fun retry() = load(reset = true)

    /** Next page near the end of the list. A failed page is retried, never skipped. */
    fun loadMore() {
        val s = _state.value
        // After a failed reload the list still shows the old filter: a next page would mix the two
        if (s.loading || s.loadingMore || !s.canLoadMore || s.error != null) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        // A new search or filter cancels the request still running for the old one
        if (reset) job?.cancel() else if (job?.isActive == true) return
        val s = _state.value
        val page = if (reset) 0 else s.page + 1
        _state.update {
            // A reset also ends a cancelled "load more", so its skeleton can't stay on screen
            if (reset) it.copy(loading = !it.refreshing, loadingMore = false, error = null, loadMoreFailed = false, loadedOnce = true)
            else it.copy(loadingMore = true, loadMoreFailed = false)
        }
        val f = s.applied
        job = viewModelScope.launch {
            runApi {
                api.wantedFeed(
                    page = page,
                    cardName = f.query.ifBlank { null },
                    rarity = f.rarity,
                    expansion = f.expansion,
                    language = f.language,
                    friendId = s.friendId,
                    spareOnly = f.spareOnly,
                )
            }.onSuccess { result ->
                val tiles = result.content.flatMap { it.toTiles() }
                _state.update {
                    it.copy(
                        tiles = (if (reset) tiles else it.tiles + tiles).distinctBy { t -> t.key },
                        page = page,
                        totalPages = result.totalPages,
                        totalElements = result.totalRequests,
                        loading = false, refreshing = false, loadingMore = false, error = null,
                    )
                }
            }.onFailure { e ->
                _state.update {
                    // With cards on screen they stay, and the error shows above them
                    if (reset) it.copy(loading = false, refreshing = false, error = e.message ?: "Could not load the wanted cards.")
                    else it.copy(loadingMore = false, loadMoreFailed = true)
                }
            }
        }
    }

    private fun FeedGroup.toTiles(): List<FeedTile> {
        val wants = requests.map { FeedTile.Want(it) }
        val more = total - requests.size
        return if (more > 0) wants + FeedTile.More(userFriendId, latest, more) else wants
    }
}
