package com.tcgptrading.tcgptrading.features.wants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.core.data.Session
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.model.Card
import com.tcgptrading.tcgptrading.core.data.model.RequestCreate
import com.tcgptrading.tcgptrading.core.data.runApi
import com.tcgptrading.tcgptrading.core.presentation.components.CARD_RATIO
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.RarityText
import com.tcgptrading.tcgptrading.core.presentation.components.ScreenHeader
import com.tcgptrading.tcgptrading.core.presentation.components.SearchField
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.Tag
import com.tcgptrading.tcgptrading.core.presentation.components.TagTone
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.components.TcgChip
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.LANGUAGES
import com.tcgptrading.tcgptrading.core.util.expansionName
import com.tcgptrading.tcgptrading.main.presentation.LocalFriendIdGate
import com.tcgptrading.tcgptrading.main.presentation.LocalSnackbar
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private const val NOTE_MAX = 200

data class AddWantsState(
    val query: String = "",
    val cards: List<Card> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    /** Cards I already want (by cardId). */
    val wantedIds: Set<String> = emptySet(),
    val selected: Card? = null,
    val language: String = "English",
    val note: String = "",
    val submitting: Boolean = false,
    /** The selected card was just added. */
    val added: Boolean = false,
)

class AddWantsViewModel(
    private val api: TradingApi,
    private val session: Session,
) : ViewModel() {

    private val _state = MutableStateFlow(AddWantsState())
    val state: StateFlow<AddWantsState> = _state.asStateFlow()

    private val queries = MutableStateFlow("")
    private var loadJob: Job? = null

    init {
        loadPage(0)
        loadWantedIds()
        // New search text: wait until typing pauses, then start again from the first page
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            queries.drop(1).debounce(350).distinctUntilChanged().collect { loadPage(0) }
        }
    }

    fun onQueryChange(text: String) {
        _state.update { it.copy(query = text) }
        queries.value = text.trim()
    }

    fun retry() = loadPage(0)

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.page + 1 >= s.totalPages) return
        loadPage(s.page + 1)
    }

    private fun loadPage(page: Int) {
        loadJob?.cancel()
        _state.update { if (page == 0) it.copy(loading = true, error = null) else it.copy(loadingMore = true) }
        loadJob = viewModelScope.launch {
            runApi { api.cards(queries.value, page) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            cards = if (page == 0) result.content else it.cards + result.content,
                            page = page,
                            totalPages = result.page.totalPages,
                            loading = false,
                            loadingMore = false,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        if (page == 0) it.copy(loading = false, error = e.message)
                        else it.copy(loadingMore = false)
                    }
                }
        }
    }

    private fun loadWantedIds() {
        viewModelScope.launch {
            runApi { api.myWants(page = 0, size = 200) }
                .onSuccess { wants -> _state.update { it.copy(wantedIds = wants.content.map { w -> w.cardId }.toSet()) } }
        }
    }

    fun select(card: Card) {
        _state.update { it.copy(selected = card, language = "English", note = "", added = false, submitting = false) }
    }

    fun dismiss() {
        _state.update { it.copy(selected = null) }
    }

    fun setLanguage(language: String) = _state.update { it.copy(language = language) }

    fun setNote(note: String) = _state.update { it.copy(note = note.take(NOTE_MAX)) }

    /** Adds the selected card to my wants; returns a message for the snackbar on failure. */
    fun add(onError: (String) -> Unit) {
        val s = _state.value
        val card = s.selected ?: return
        if (s.submitting || s.added) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            runApi {
                api.addWant(
                    RequestCreate(
                        cardId = card.cardId,
                        userFriendId = session.friendId,
                        message = s.note.trim().ifBlank { null },
                        language = s.language,
                    )
                )
            }.onSuccess {
                _state.update { it.copy(submitting = false, added = true, wantedIds = it.wantedIds + card.cardId) }
            }.onFailure { e ->
                _state.update { it.copy(submitting = false) }
                onError(e.message ?: "Could not add the card. Try again.")
            }
        }
    }
}

/** Pick a card you are missing; other players see it on Wanted and send offers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWantsScreen(onSeeMyWants: () -> Unit) {
    val vm = koinViewModel<AddWantsViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val snackbar = LocalSnackbar.current
    val gate = LocalFriendIdGate.current
    val scope = rememberCoroutineScope()

    // Load the next page when the list nears its end
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { nearEnd }.collect { if (it) vm.loadMore() }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Add wants",
            subtitle = "Pick a card you are missing. Other players see it on Wanted and send you offers.",
        )
        // Search stays in view while the grid scrolls
        SearchField(state.query, vm::onQueryChange, "Search a Pokémon or card", Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            when {
                state.loading -> items(6) { Skeleton(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO), corner = 14.dp) }

                state.error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorState(state.error ?: "", onRetry = vm::retry)
                }

                state.cards.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "No cards found",
                        message = "Try another name, or clear the search to see every card.",
                        actionText = if (state.query.isNotBlank()) "Clear search" else null,
                        onAction = { vm.onQueryChange("") },
                    )
                }

                else -> {
                    items(state.cards, key = { it.cardId }) { card ->
                        CardTile(
                            card = card,
                            wanted = card.cardId in state.wantedIds,
                            onClick = { vm.select(card) },
                        )
                    }
                    if (state.loadingMore) {
                        items(2) { Skeleton(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO), corner = 14.dp) }
                    }
                }
            }
        }
    }

    val selected = state.selected
    if (selected != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = vm::dismiss,
            sheetState = sheetState,
            containerColor = TcgColors.Surface,
        ) {
            AddWantSheet(
                card = selected,
                state = state,
                onLanguage = vm::setLanguage,
                onNote = vm::setNote,
                onAdd = {
                    gate.require {
                        vm.add { message -> scope.launch { snackbar.showSnackbar(message) } }
                    }
                },
                onSeeMyWants = {
                    vm.dismiss()
                    onSeeMyWants()
                },
            )
        }
    }
}

@Composable
private fun CardTile(card: Card, wanted: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable(role = Role.Button, onClickLabel = "Choose ${card.name}", onClick = onClick)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            CardArt(card.illustrationUrl, card.name, Modifier.fillMaxWidth())
            when {
                wanted -> Tag("In your wants", Modifier.align(Alignment.BottomStart).padding(10.dp), tone = TagTone.Ink)
                card.tradable == false -> Tag("Not tradable", Modifier.align(Alignment.BottomStart).padding(10.dp), tone = TagTone.Soft)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                card.name,
                style = MaterialTheme.typography.titleMedium,
                color = TcgColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            RarityText(card.rarity ?: card.rarityName, size = 11)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddWantSheet(
    card: Card,
    state: AddWantsState,
    onLanguage: (String) -> Unit,
    onNote: (String) -> Unit,
    onAdd: () -> Unit,
    onSeeMyWants: () -> Unit,
) {
    val alreadyWanted = card.cardId in state.wantedIds && !state.added
    val tradable = card.tradable != false

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CardArt(card.illustrationUrl, card.name, Modifier.width(76.dp), corner = 8.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(card.name, style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RarityText(card.rarity ?: card.rarityName)
                    Text(expansionName(card.expansion), style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
                }
            }
        }

        when {
            state.added -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Added. Players who have ${card.name} can now send you offers, and you'll get an email when they do.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TcgColors.Ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TcgColors.Ground, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                            .semantics { contentDescription = "Added to your wants. Players who have ${card.name} can now send you offers." },
                    )
                    TcgButton("See my wants", onSeeMyWants, Modifier.fillMaxWidth(), style = TcgButtonStyle.Dark, large = true)
                }
            }

            !tradable -> Text(
                "This card can't be traded in Pokémon TCG Pocket, so it can't be added to your wants.",
                style = MaterialTheme.typography.bodyLarge,
                color = TcgColors.Muted,
            )

            alreadyWanted -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This card is already in your wants.", style = MaterialTheme.typography.bodyLarge, color = TcgColors.Muted)
                TcgButton("See my wants", onSeeMyWants, Modifier.fillMaxWidth(), style = TcgButtonStyle.Secondary)
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Which language do you need?", style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LANGUAGES.forEach { language ->
                            TcgChip(language, selected = state.language == language, onClick = { onLanguage(language) })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Note for traders (optional)", style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = onNote,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("For example: I can give Moltres ex or Articuno ex", color = TcgColors.Faint) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = { Text("${state.note.length} / $NOTE_MAX", color = TcgColors.Muted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TcgColors.Ink,
                            unfocusedBorderColor = TcgColors.Line,
                            cursorColor = TcgColors.Ink,
                        ),
                    )
                }

                TcgButton(
                    "Add to my wants",
                    onAdd,
                    Modifier.fillMaxWidth(),
                    loading = state.submitting,
                    large = true,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
