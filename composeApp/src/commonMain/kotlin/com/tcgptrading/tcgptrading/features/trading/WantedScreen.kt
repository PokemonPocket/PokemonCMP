package com.tcgptrading.tcgptrading.features.trading

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcgptrading.tcgptrading.core.data.model.TradeRequest
import com.tcgptrading.tcgptrading.core.presentation.components.CARD_RATIO
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.RarityText
import com.tcgptrading.tcgptrading.core.presentation.components.ScreenHeader
import com.tcgptrading.tcgptrading.core.presentation.components.SearchField
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.components.TcgChip
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.EXPANSIONS
import com.tcgptrading.tcgptrading.core.util.LANGUAGES
import com.tcgptrading.tcgptrading.core.util.RARITY_FILTERS
import com.tcgptrading.tcgptrading.core.util.expansionName
import com.tcgptrading.tcgptrading.core.util.offersText
import com.tcgptrading.tcgptrading.core.util.rarityOf
import com.tcgptrading.tcgptrading.core.util.requestedText
import com.tcgptrading.tcgptrading.main.presentation.LocalFriendIdGate
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wanted cards: what other players are looking for. Search, filter by rarity, expansion and language,
 * and offer a trade on any card you have.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WantedScreen(onOffer: (TradeRequest) -> Unit, onAddWants: () -> Unit) {
    val vm = koinViewModel<WantedViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val gate = LocalFriendIdGate.current
    val gridState = rememberLazyGridState()

    LoadMoreWhenNearEnd(gridState, state.items.size, onLoadMore = vm::loadMore)

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            fullSpan("header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ScreenHeader(
                        title = "Wanted cards",
                        subtitle = "Have one of these? Offer it and pick cards of the same rarity you would like back.",
                        modifier = Modifier.padding(horizontal = 0.dp),
                    )
                    SearchField(value = state.query, onValueChange = vm::setQuery, placeholder = "Search a Pokémon or card")
                    RarityChips(selected = state.rarity, onSelect = vm::setRarity)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterPicker(
                            label = "Expansion",
                            value = state.expansion?.let { expansionName(it) } ?: "All expansions",
                            options = listOf<Pair<String?, String>>(null to "All expansions") + EXPANSIONS.map { it.key to it.value },
                            onSelect = vm::setExpansion,
                            modifier = Modifier.weight(1f),
                        )
                        FilterPicker(
                            label = "Language",
                            value = state.language ?: "Any language",
                            options = listOf<Pair<String?, String>>(null to "Any language") + LANGUAGES.map { it to it },
                            onSelect = vm::setLanguage,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (!state.loading && state.error == null && state.items.isNotEmpty()) {
                        Text(
                            if (state.totalElements == 1L) "1 wanted card" else "${state.totalElements} wanted cards",
                            style = MaterialTheme.typography.bodySmall,
                            color = TcgColors.Muted,
                        )
                    }
                }
            }

            when {
                state.loading && state.items.isEmpty() -> items(6, key = { "skeleton$it" }) { WantedTileSkeleton() }

                state.error != null && state.items.isEmpty() -> fullSpan("error") {
                    ErrorState(message = state.error.orEmpty(), onRetry = vm::retry, modifier = Modifier.padding(horizontal = 0.dp))
                }

                state.items.isEmpty() -> fullSpan("empty") {
                    if (state.hasFilters) {
                        EmptyState(
                            title = "No wanted cards match",
                            message = "Try another name, or clear the filters to see everything players are looking for.",
                            actionText = "Clear filters",
                            onAction = vm::clearFilters,
                        )
                    } else {
                        EmptyState(
                            title = "Nothing wanted yet",
                            message = "Be the first: add the cards you are missing and players who have them will send offers.",
                            actionText = "Add wants",
                            onAction = onAddWants,
                        )
                    }
                }

                else -> {
                    items(state.items, key = { it.requestId }) { request ->
                        WantedTile(request = request, onOffer = { gate.require { onOffer(request) } })
                    }
                    if (state.loadingMore) {
                        items(2, key = { "more$it" }) { WantedTileSkeleton() }
                    }
                    if (state.loadMoreFailed) {
                        fullSpan("loadMoreFailed") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("Could not load more cards.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
                                Spacer(Modifier.height(8.dp))
                                TcgButton("Try again", onClick = vm::loadMore, style = TcgButtonStyle.Secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullSpan(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** Asks for the next page when the last visible tile is close to the end. */
@Composable
private fun LoadMoreWhenNearEnd(state: LazyGridState, itemCount: Int, onLoadMore: () -> Unit) {
    LaunchedEffect(state, itemCount) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                if (itemCount > 0 && last >= state.layoutInfo.totalItemsCount - 6) onLoadMore()
            }
    }
}

@Composable
private fun RarityChips(selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") { TcgChip("All rarities", selected = selected == null, onClick = { onSelect(null) }) }
        items(RARITY_FILTERS.size, key = { RARITY_FILTERS[it] }) { i ->
            val code = RARITY_FILTERS[i]
            val rarity = rarityOf(code)
            TcgChip(
                text = rarity?.glyphs ?: code,
                selected = selected == code,
                onClick = { onSelect(if (selected == code) null else code) },
                label = rarity?.label ?: code,
            )
        }
    }
}

/** A pill that opens a menu of options; shows the current choice. */
@Composable
private fun <T> FilterPicker(
    label: String,
    value: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = CircleShape,
            color = TcgColors.Surface,
            border = BorderStroke(1.dp, TcgColors.Line),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "$label: $value" },
        ) {
            Row(
                Modifier.padding(start = 16.dp, end = 8.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TcgColors.Ink)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = TcgColors.Surface) {
            options.forEach { (key, text) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (text == value) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = { open = false; onSelect(key) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun WantedTile(request: TradeRequest, onOffer: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardArt(imageUrl = request.imageUrl, name = request.cardName, modifier = Modifier.fillMaxWidth())
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    request.cardName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TcgColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                RarityText(request.rarity, size = 11)
            }
            val meta = listOfNotNull(expansionName(request.expansion).ifBlank { null }, request.language).joinToString(", ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            requestedText(request.creationDate).takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted, maxLines = 1)
            }
            Text(
                offersText(request.offerCount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TcgColors.Ink,
            )
        }
        TcgButton(
            text = "Offer a trade",
            onClick = onOffer,
            style = TcgButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WantedTileSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Skeleton(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO), corner = 14.dp)
        Skeleton(Modifier.fillMaxWidth(0.7f).height(16.dp))
        Skeleton(Modifier.fillMaxWidth(0.5f).height(12.dp))
        Skeleton(Modifier.fillMaxWidth().height(46.dp), corner = 12.dp)
    }
}
