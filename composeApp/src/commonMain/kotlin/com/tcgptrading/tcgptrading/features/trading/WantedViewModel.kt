package com.tcgptrading.tcgptrading.features.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.core.data.TradingApi
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

data class WantedState(
    val items: List<TradeRequest> = emptyList(),
    val query: String = "",
    /** Backend Rarity code (C, U, R, RR, AR, SR, IM, UR), null = all. */
    val rarity: String? = null,
    /** Backend Expansion code (A1, A1a…), null = all. */
    val expansion: String? = null,
    /** Backend Language name, null = any. */
    val language: String? = null,
    val page: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val loadMoreFailed: Boolean = false,
) {
    val hasFilters: Boolean get() = query.isNotBlank() || rarity != null || expansion != null || language != null
    val canLoadMore: Boolean get() = page + 1 < totalPages
}

/** The public Wanted list: search, filters and paging. */
@OptIn(FlowPreview::class)
class WantedViewModel(private val api: TradingApi, private val searchRequests: WantedSearchRequests) : ViewModel() {

    private val _state = MutableStateFlow(WantedState())
    val state: StateFlow<WantedState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load(reset = true)
        // Search waits until typing stops, then starts again from the first page
        viewModelScope.launch {
            _state.map { it.query.trim() }
                .distinctUntilChanged()
                .drop(1)
                .debounce(350)
                .collect { load(reset = true) }
        }
        // Another tab asked to show wants for a specific card: search for it with no other filters
        viewModelScope.launch {
            searchRequests.pending.filterNotNull().collect { search ->
                searchRequests.consume()
                _state.update { it.copy(query = search, rarity = null, expansion = null, language = null) }
                load(reset = true)
            }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun setRarity(rarity: String?) {
        if (_state.value.rarity == rarity) return
        _state.update { it.copy(rarity = rarity) }
        load(reset = true)
    }

    fun setExpansion(expansion: String?) {
        if (_state.value.expansion == expansion) return
        _state.update { it.copy(expansion = expansion) }
        load(reset = true)
    }

    fun setLanguage(language: String?) {
        if (_state.value.language == language) return
        _state.update { it.copy(language = language) }
        load(reset = true)
    }

    fun clearFilters() {
        _state.update { it.copy(query = "", rarity = null, expansion = null, language = null) }
        load(reset = true)
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        load(reset = true)
    }

    fun retry() = load(reset = true)

    /** Next page near the end of the list. A failed page is retried, never skipped. */
    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || !s.canLoadMore) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        // A new search or filter cancels the request still running for the old one
        if (reset) job?.cancel() else if (job?.isActive == true) return
        val s = _state.value
        val page = if (reset) 0 else s.page + 1
        _state.update {
            if (reset) it.copy(loading = !it.refreshing, error = null, loadMoreFailed = false)
            else it.copy(loadingMore = true, loadMoreFailed = false)
        }
        job = viewModelScope.launch {
            runApi {
                api.wanted(
                    page = page,
                    cardName = s.query.trim().ifBlank { null },
                    rarity = s.rarity,
                    expansion = s.expansion,
                    language = s.language,
                )
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        items = if (reset) result.content else it.items + result.content,
                        page = page,
                        totalPages = result.page.totalPages,
                        totalElements = result.page.totalElements,
                        loading = false, refreshing = false, loadingMore = false, error = null,
                    )
                }
            }.onFailure { e ->
                _state.update {
                    if (reset) it.copy(loading = false, refreshing = false, error = e.message ?: "Could not load the wanted cards.")
                    else it.copy(loadingMore = false, loadMoreFailed = true)
                }
            }
        }
    }
}
