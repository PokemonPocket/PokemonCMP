package com.tcgptrading.tcgptrading.features.trading

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcgptrading.tcgptrading.app.domain.Detail
import com.tcgptrading.tcgptrading.core.data.model.TradeRequest
import com.tcgptrading.tcgptrading.core.presentation.components.CARD_RATIO
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.RarityText
import com.tcgptrading.tcgptrading.core.presentation.components.ScreenHeader
import com.tcgptrading.tcgptrading.core.presentation.components.SearchField
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.Tag
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.components.TcgChip
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.EXPANSIONS
import com.tcgptrading.tcgptrading.core.util.LANGUAGES
import com.tcgptrading.tcgptrading.core.util.RARITY_FILTERS
import com.tcgptrading.tcgptrading.core.util.expansionName
import com.tcgptrading.tcgptrading.core.util.playerLabel
import com.tcgptrading.tcgptrading.core.util.rarityOf
import com.tcgptrading.tcgptrading.main.presentation.LocalFriendIdGate
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wanted cards, in four tabs (every list newest first):
 * For you (trades picked from my collection), By card, Traders, and All wants.
 */
@Composable
fun WantedScreen(onOffer: (Detail.MakeOffer) -> Unit, onAddWants: () -> Unit, onSeeOffers: () -> Unit) {
    val vm = koinViewModel<WantedViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val header: @Composable () -> Unit = { WantedHeader(state.tab, onTab = vm::setTab) }
    // Each tab keeps its scroll position and open cards while another tab is shown
    val tabStates = rememberSaveableStateHolder()

    // Back from Collection (or anywhere): the spare marks follow the collection
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { }
    }

    tabStates.SaveableStateProvider(state.tab.name) {
        when (state.tab) {
            WantedTab.ForYou -> ForYouTab(header = header, onOffer = onOffer, onOpenCollection = onAddWants, onOpenTab = vm::setTab)
            WantedTab.ByCard -> ByCardTab(
                header = header,
                filters = { WantedFiltersBlock(state.filters, vm) },
                applied = state.applied,
                sparesOf = state::sparesOf,
                onOffer = onOffer,
            )
            WantedTab.Traders -> TradersTab(
                header = header,
                onOffer = onOffer,
                onShowPlayer = vm::showPlayer,
                onOpenCollection = onAddWants,
                onOpenTab = vm::setTab,
                onSeeOffers = onSeeOffers,
            )
            WantedTab.All -> AllWantsTab(state = state, vm = vm, header = header, onOffer = onOffer, onAddWants = onAddWants)
        }
    }
}

// ---------------------------------------------------------------------------
// Header, tabs and filters (shared by the tabs)
// ---------------------------------------------------------------------------

private fun subtitleOf(tab: WantedTab): String = when (tab) {
    WantedTab.ForYou -> "Trades picked from your collection. Same rarity on both sides, so each one can go through in the game."
    WantedTab.ByCard -> "One card at a time, with how many players want it. Tap a card to see who wants it, newest first."
    WantedTab.Traders -> "Players you can do several trades with: they want 2 or more of your spares. Newest first."
    WantedTab.All -> "Every open want, newest first. Many cards from one player on the same day are folded into one tile."
}

@Composable
internal fun WantedHeader(tab: WantedTab, onTab: (WantedTab) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(title = "Wanted cards", subtitle = subtitleOf(tab), modifier = Modifier.padding(horizontal = 0.dp))
        WantedTabRow(tab, onTab)
    }
}

@Composable
private fun WantedTabRow(tab: WantedTab, onTab: (WantedTab) -> Unit) {
    Surface(shape = CircleShape, color = TcgColors.Surface, border = BorderStroke(1.dp, TcgColors.Line)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            WantedTab.entries.forEach { t ->
                val isSelected = t == tab
                Surface(
                    onClick = { onTab(t) },
                    shape = CircleShape,
                    color = if (isSelected) TcgColors.Ink else Color.Transparent,
                    contentColor = if (isSelected) Color.White else TcgColors.Ink,
                    modifier = Modifier.heightIn(min = 44.dp).semantics { role = Role.Tab; selected = isSelected },
                ) {
                    Box(Modifier.padding(horizontal = 14.dp).heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                        Text(t.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Search, rarity, expansion, language and "only cards I have spare". */
@Composable
internal fun WantedFiltersBlock(filters: WantedFilters, vm: WantedViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SearchField(value = filters.query, onValueChange = vm::setQuery, placeholder = "Search a Pokémon or card")
        RarityChips(selected = filters.rarity, onSelect = vm::setRarity)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPicker(
                label = "Expansion",
                value = filters.expansion?.let { expansionName(it) } ?: "All expansions",
                options = listOf<Pair<String?, String>>(null to "All expansions") + EXPANSIONS.map { it.key to it.value },
                onSelect = vm::setExpansion,
                modifier = Modifier.weight(1f),
            )
            FilterPicker(
                label = "Language",
                value = filters.language ?: "Any language",
                options = listOf<Pair<String?, String>>(null to "Any language") + LANGUAGES.map { it to it },
                onSelect = vm::setLanguage,
                modifier = Modifier.weight(1f),
            )
        }
        TcgChip(
            text = if (filters.spareOnly) "✓  Only cards I have spare" else "Only cards I have spare",
            selected = filters.spareOnly,
            onClick = vm::toggleSpareOnly,
            label = "Only cards I have spare",
        )
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
internal fun <T> FilterPicker(
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

/** Green "You have 2 spares" pill. */
@Composable
internal fun SpareChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.clip(CircleShape).background(SpareGreen).padding(horizontal = 10.dp, vertical = 3.dp),
        color = TcgColors.OkText,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 1,
    )
}

internal val SpareGreen = Color(0xFFE6F4EC)

/** A reload failed while the old results are still on screen: say so, with a way to try again. */
@Composable
internal fun InlineErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TcgColors.RedSoft)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Couldn't refresh", style = MaterialTheme.typography.titleSmall, color = TcgColors.RedInk)
            Text(message, style = MaterialTheme.typography.bodySmall, color = TcgColors.Ink)
        }
        TcgButton("Retry", onClick = onRetry, style = TcgButtonStyle.Ghost)
    }
}

/** Thin bar at the top while a list loads again behind the results already shown. */
@Composable
internal fun ReloadBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth().height(3.dp),
        color = TcgColors.Red,
        trackColor = Color.Transparent,
    )
}

/** Asks for the next page when the last visible tile is close to the end. */
@Composable
internal fun LoadMoreWhenNearEnd(state: LazyGridState, itemCount: Int, onLoadMore: () -> Unit) {
    LaunchedEffect(state, itemCount) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                if (itemCount > 0 && last >= state.layoutInfo.totalItemsCount - 6) onLoadMore()
            }
    }
}

@Composable
internal fun LoadMoreWhenNearEnd(state: LazyListState, itemCount: Int, onLoadMore: () -> Unit) {
    LaunchedEffect(state, itemCount) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                if (itemCount > 0 && last >= state.layoutInfo.totalItemsCount - 4) onLoadMore()
            }
    }
}

// ---------------------------------------------------------------------------
// All wants
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllWantsTab(
    state: WantedState,
    vm: WantedViewModel,
    header: @Composable () -> Unit,
    onOffer: (Detail.MakeOffer) -> Unit,
    onAddWants: () -> Unit,
) {
    val gate = LocalFriendIdGate.current
    val gridState = rememberLazyGridState()

    LoadMoreWhenNearEnd(gridState, state.tiles.size, onLoadMore = vm::loadMore)

    // A new filter or player ("See all their wants") starts at the top; coming back to the tab keeps the place
    val listKey = "${state.applied}|${state.friendId}"
    var shownKey by rememberSaveable { mutableStateOf(listKey) }
    LaunchedEffect(listKey) {
        if (listKey != shownKey) {
            shownKey = listKey
            gridState.scrollToItem(0)
        }
    }

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            // Two columns on a phone, more on wider screens
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            fullSpan("header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    header()
                    WantedFiltersBlock(state.filters, vm)
                    state.friendId?.let { friendId ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TcgColors.Surface).padding(start = 16.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Showing the wants of ${playerLabel(friendId)}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TcgButton("Show everyone", onClick = vm::clearPlayer, style = TcgButtonStyle.Ghost)
                        }
                    }
                    if (!state.loading && state.error == null && state.tiles.isNotEmpty()) {
                        Text(
                            if (state.totalElements == 1L) "1 wanted card, newest first" else "${state.totalElements} wanted cards, newest first",
                            style = MaterialTheme.typography.bodySmall,
                            color = TcgColors.Muted,
                        )
                    }
                }
            }

            if (state.error != null && state.tiles.isNotEmpty()) {
                fullSpan("reloadError") { InlineErrorBanner(state.error, onRetry = vm::retry) }
            }

            when {
                state.loading && state.tiles.isEmpty() -> items(6, key = { "skeleton$it" }) { WantedTileSkeleton() }

                state.error != null && state.tiles.isEmpty() -> fullSpan("error") {
                    ErrorState(message = state.error.orEmpty(), onRetry = vm::retry, modifier = Modifier.padding(horizontal = 0.dp))
                }

                state.tiles.isEmpty() -> fullSpan("empty") {
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
                    items(state.tiles, key = { it.key }) { tile ->
                        when (tile) {
                            is FeedTile.Want -> WantedTile(
                                request = tile.request,
                                spares = state.sparesOf(tile.request.cardId),
                                onOffer = {
                                    gate.require {
                                        onOffer(Detail.MakeOffer(tile.request.requestId, tile.request.cardId, tile.request.userFriendId.orEmpty()))
                                    }
                                },
                            )
                            is FeedTile.More -> BatchTile(tile, onSeeAll = tile.friendId?.let { id -> { vm.showPlayer(id) } })
                        }
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
                    if (!state.canLoadMore && !state.loading && !state.loadingMore && !state.loadMoreFailed) {
                        fullSpan("end") {
                            Text(
                                "That's all ${state.totalElements} wanted ${if (state.totalElements == 1L) "card" else "cards"}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TcgColors.Muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        if (state.loading && state.tiles.isNotEmpty() && !state.refreshing) ReloadBar(Modifier.align(Alignment.TopCenter))
    }
}

private fun LazyGridScope.fullSpan(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun WantedTile(request: TradeRequest, spares: Int, onOffer: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            CardArt(imageUrl = request.imageUrl, name = request.cardName, modifier = Modifier.fillMaxWidth())
            wantTag(request)?.let { tag -> Tag(tag.text, Modifier.align(Alignment.TopStart).padding(12.dp), tone = tag.tone) }
        }
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
            val who = listOfNotNull(playerLabel(request.userFriendId), request.language).joinToString(" · ")
            Text(who, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            addedText(request.creationDate).takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted, maxLines = 1)
            }
            if (spares > 0) {
                Spacer(Modifier.height(2.dp))
                SpareChip(sparesText(spares))
            }
        }
        TcgButton(
            text = "Offer a trade",
            onClick = onOffer,
            style = TcgButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The rest of a player's same-day wants, folded into one tile. onSeeAll is null when the player is unknown. */
@Composable
private fun BatchTile(tile: FeedTile.More, onSeeAll: (() -> Unit)?) {
    val who = playerLabel(tile.friendId)
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TcgColors.Soft)
            .border(1.5.dp, TcgColors.Line, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth(0.62f).aspectRatio(CARD_RATIO).padding(top = 6.dp)) {
            StackCard(Modifier.rotate(-8f))
            StackCard(Modifier.rotate(5f))
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(TcgColors.Surface).border(1.dp, TcgColors.Line, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+${tile.more}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = TcgColors.Ink)
            }
        }
        Text(
            "$who added ${tile.more} more ${if (tile.more == 1) "card" else "cards"}",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        addedText(tile.latest).takeIf { it.isNotBlank() }?.let {
            Text("$it, all on the same day", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted, textAlign = TextAlign.Center)
        }
        // Short label: the tile is half a phone wide, and the line above says whose wants these are
        if (onSeeAll != null) {
            TcgButton("See all", onClick = onSeeAll, style = TcgButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StackCard(modifier: Modifier) {
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(TcgColors.Surface).border(1.dp, TcgColors.Line, RoundedCornerShape(10.dp)))
}

@Composable
internal fun WantedTileSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Skeleton(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO), corner = 14.dp)
        Skeleton(Modifier.fillMaxWidth(0.7f).height(16.dp))
        Skeleton(Modifier.fillMaxWidth(0.5f).height(12.dp))
        Skeleton(Modifier.fillMaxWidth().height(46.dp), corner = 12.dp)
    }
}
