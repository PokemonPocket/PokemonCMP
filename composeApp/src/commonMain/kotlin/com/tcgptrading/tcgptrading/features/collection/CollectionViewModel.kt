package com.tcgptrading.tcgptrading.features.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.model.Card
import com.tcgptrading.tcgptrading.core.data.model.RequestCreate
import com.tcgptrading.tcgptrading.core.data.model.SetProgress
import com.tcgptrading.tcgptrading.core.data.model.SpareMatches
import com.tcgptrading.tcgptrading.core.data.runApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OwnFilter { All, Owned, Missing, Spares }

data class CollectionState(
    val sets: List<SetProgress> = emptyList(),
    val setCode: String? = null,
    /** All cards of the selected set loaded so far, in set-number order. */
    val cards: List<Card> = emptyList(),
    /** cardId -> copies, including taps not yet saved. */
    val counts: Map<String, Int> = emptyMap(),
    val matches: SpareMatches = SpareMatches(),
    val wantedIds: Set<String> = emptySet(),
    val filter: OwnFilter = OwnFilter.All,
    val rarity: String? = null,
    val query: String = "",
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
) {
    val selectedSet: SetProgress? get() = sets.firstOrNull { it.code == setCode }

    /** Owned count for the selected set, kept live while tapping. */
    val ownedInSet: Int get() = cards.count { (counts[it.cardId] ?: 0) > 0 }

    /** Cards after the Owned / Missing / Spares, rarity and search filters. */
    val shown: List<Card>
        get() {
            val q = query.trim().removePrefix("#").lowercase()
            return cards.filter { card ->
                val n = counts[card.cardId] ?: 0
                val byOwn = when (filter) {
                    OwnFilter.All -> true
                    OwnFilter.Owned -> n > 0
                    OwnFilter.Missing -> n == 0
                    OwnFilter.Spares -> n > 1
                }
                val byRarity = rarity == null || card.rarity.equals(rarity, ignoreCase = true)
                val byQuery = q.isEmpty() || card.name.lowercase().contains(q) ||
                    (q.toIntOrNull() != null && card.collectionNumber == q.toInt())
                byOwn && byRarity && byQuery
            }
        }

    /** How many players want this card, when it is one of my spares. */
    fun wantedBy(cardId: String): Long = matches.cards.firstOrNull { it.cardId == cardId }?.wantedBy ?: 0
}

/**
 * Collection: the cards a player owns and their spare copies, per expansion.
 * Taps are applied right away and saved in the background, one request at a time per card,
 * so quick taps end on the last value; a failed save puts the last saved value back.
 */
class CollectionViewModel(private val api: TradingApi) : ViewModel() {

    private val _state = MutableStateFlow(CollectionState())
    val state: StateFlow<CollectionState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Short messages for the snackbar. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Last value the server confirmed, per card. */
    private val saved = mutableMapOf<String, Int>()
    private val saving = mutableMapOf<String, Job>()
    private var setJob: Job? = null
    private var summaryJob: Job? = null

    init {
        loadAll()
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        loadAll()
    }

    fun retry() = loadAll()

    private fun loadAll() {
        _state.update { it.copy(error = null, loading = it.cards.isEmpty() && !it.refreshing) }
        viewModelScope.launch {
            val collection = runApi { api.collection() }
            val sets = runApi { api.collectionSets() }
            if (collection.isFailure || sets.isFailure) {
                val msg = (collection.exceptionOrNull() ?: sets.exceptionOrNull())?.message ?: "Something went wrong."
                _state.update { it.copy(loading = false, refreshing = false, error = msg) }
                return@launch
            }
            val counts = collection.getOrThrow().cards
            saved.clear(); saved.putAll(counts)
            val setList = sets.getOrThrow()
            _state.update { s ->
                // Keep the chosen set; first visit starts on the first set the player has cards in
                val code = s.setCode?.takeIf { c -> setList.any { it.code == c } }
                    ?: setList.firstOrNull { it.owned > 0 }?.code
                    ?: setList.firstOrNull { it.code == "A1" }?.code
                    ?: setList.firstOrNull()?.code
                s.copy(sets = setList, setCode = code, counts = counts + pendingCounts(s))
            }
            loadSetCards()
            loadMatches()
            loadWants()
        }
    }

    /** Taps still being saved keep their value across a refresh. */
    private fun pendingCounts(s: CollectionState): Map<String, Int> =
        s.counts.filterKeys { saving[it]?.isActive == true }

    fun selectSet(code: String) {
        if (code == _state.value.setCode) return
        _state.update { it.copy(setCode = code, cards = emptyList(), loading = true, query = "") }
        loadSetCards()
    }

    fun setFilter(filter: OwnFilter) = _state.update { it.copy(filter = filter) }
    fun setRarity(rarity: String?) = _state.update { it.copy(rarity = rarity) }
    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    /** Loads the whole set page by page (sets are a few hundred cards), showing cards as they arrive. */
    private fun loadSetCards() {
        val code = _state.value.setCode ?: run {
            _state.update { it.copy(loading = false, refreshing = false) }
            return
        }
        setJob?.cancel()
        setJob = viewModelScope.launch {
            var page = 0
            val all = mutableListOf<Card>()
            while (true) {
                val result = runApi { api.cardsOfExpansion(code, page) }
                val data = result.getOrElse { e ->
                    _state.update { it.copy(loading = false, loadingMore = false, refreshing = false, error = if (all.isEmpty()) e.message else null) }
                    if (all.isNotEmpty()) _messages.tryEmit("Some cards could not be loaded. Pull to refresh.")
                    return@launch
                }
                all += data.content
                val more = page + 1 < data.page.totalPages
                _state.update { it.copy(cards = all.toList(), loading = false, loadingMore = more, refreshing = false) }
                if (!more) break
                page++
            }
        }
    }

    private fun loadMatches() {
        viewModelScope.launch {
            runApi { api.spareMatches() }.onSuccess { m -> _state.update { it.copy(matches = m) } }
        }
    }

    private fun loadWants() {
        viewModelScope.launch {
            runApi { api.myWants(page = 0, size = 200) }.onSuccess { page ->
                _state.update { s -> s.copy(wantedIds = page.content.filter { it.cardTraded != true }.map { it.cardId }.toSet()) }
            }
        }
    }

    /** Tap: owned <-> missing. */
    fun toggle(cardId: String) {
        val n = _state.value.counts[cardId] ?: 0
        setCount(cardId, if (n > 0) 0 else 1)
    }

    fun setCount(cardId: String, count: Int) {
        val value = count.coerceIn(0, 99)
        _state.update { it.copy(counts = it.counts.withCount(cardId, value)) }
        if (saving[cardId]?.isActive == true) return // the running save picks up the newest value
        saving[cardId] = viewModelScope.launch {
            while (true) {
                val target = _state.value.counts[cardId] ?: 0
                val result = runApi { api.setCollectionCount(cardId, target) }
                if (result.isFailure) {
                    val back = saved[cardId] ?: 0
                    _state.update { it.copy(counts = it.counts.withCount(cardId, back)) }
                    _messages.tryEmit(result.exceptionOrNull()?.message ?: "Could not save. Try again.")
                    break
                }
                saved[cardId] = target
                if ((_state.value.counts[cardId] ?: 0) == target) break
            }
            refreshSummarySoon()
        }
    }

    /** Marks every shown missing card as owned in one request. */
    fun markShownAsOwned() {
        val ids = _state.value.shown.filter { (_state.value.counts[it.cardId] ?: 0) == 0 }.map { it.cardId }
        if (ids.isEmpty()) return
        val before = _state.value.counts
        _state.update { s -> s.copy(counts = s.counts + ids.associateWith { 1 }) }
        viewModelScope.launch {
            ids.chunked(500).forEach { chunk ->
                runApi { api.setCollectionCounts(chunk, 1) }
                    .onSuccess { chunk.forEach { id -> saved[id] = 1 } }
                    .onFailure { e ->
                        _state.update { it.copy(counts = before) }
                        _messages.tryEmit(e.message ?: "Could not save. Try again.")
                        return@launch
                    }
            }
            _messages.tryEmit(if (ids.size == 1) "1 card marked as owned." else "${ids.size} cards marked as owned.")
            refreshSummarySoon()
        }
    }

    /** Set progress and spare matches change with the collection; refreshed once taps settle. */
    private fun refreshSummarySoon() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            delay(1200)
            runApi { api.collectionSets() }.onSuccess { sets -> _state.update { it.copy(sets = sets) } }
            runApi { api.spareMatches() }.onSuccess { m -> _state.update { it.copy(matches = m) } }
        }
    }

    /** Adds a missing card to my wants. onDone(true) when saved. */
    fun addWant(card: Card, language: String, note: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            runApi { api.addWant(RequestCreate(cardId = card.cardId, message = note.trim().ifBlank { null }, language = language)) }
                .onSuccess {
                    _state.update { it.copy(wantedIds = it.wantedIds + card.cardId) }
                    onDone(true)
                }
                .onFailure { e ->
                    _messages.tryEmit(e.message ?: "Could not add it. Try again.")
                    onDone(false)
                }
        }
    }

    private fun Map<String, Int>.withCount(cardId: String, count: Int): Map<String, Int> =
        if (count > 0) this + (cardId to count) else this - cardId
}
