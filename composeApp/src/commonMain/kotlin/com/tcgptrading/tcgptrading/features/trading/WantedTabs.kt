package com.tcgptrading.tcgptrading.features.trading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcgptrading.tcgptrading.app.domain.Detail
import com.tcgptrading.tcgptrading.core.data.model.CardDemand
import com.tcgptrading.tcgptrading.core.data.model.CardWant
import com.tcgptrading.tcgptrading.core.data.model.TradeLine
import com.tcgptrading.tcgptrading.core.data.model.WantedCard
import com.tcgptrading.tcgptrading.core.data.model.WantedHelp
import com.tcgptrading.tcgptrading.core.data.model.WantedSuggest
import com.tcgptrading.tcgptrading.core.data.model.WantedSwap
import com.tcgptrading.tcgptrading.core.data.model.WantedTrader
import com.tcgptrading.tcgptrading.core.presentation.components.Avatar
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.Panel
import com.tcgptrading.tcgptrading.core.presentation.components.RarityText
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.Tag
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.expansionName
import com.tcgptrading.tcgptrading.core.util.offersText
import com.tcgptrading.tcgptrading.core.util.playerLabel
import com.tcgptrading.tcgptrading.core.util.timeAgo
import com.tcgptrading.tcgptrading.main.presentation.LocalFriendIdGate

import org.koin.compose.viewmodel.koinViewModel

private val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)

private fun LazyListScope.headerItem(header: @Composable () -> Unit) = item(key = "header") { header() }

@Composable
private fun SkeletonPanels(count: Int, height: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(count) { Skeleton(Modifier.fillMaxWidth().height(height.dp), corner = 20.dp) }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String, tag: String? = null) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink)
            tag?.let { Tag(it) }
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
    }
}

// ---------------------------------------------------------------------------
// For you
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForYouTab(
    header: @Composable () -> Unit,
    onOffer: (Detail.MakeOffer) -> Unit,
    onOpenCollection: () -> Unit,
    onOpenTab: (WantedTab) -> Unit,
) {
    val vm = koinViewModel<ForYouViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val gate = LocalFriendIdGate.current

    // Matches follow the collection and the offers sent, so the tab reloads when it shows again
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { }
    }

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = ListPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            headerItem(header)
            val d = state.data
            if (state.error != null && d != null) {
                item(key = "reloadError") { InlineErrorBanner(state.error.orEmpty(), onRetry = vm::retry) }
            }
            when {
                state.loading -> item(key = "loading") { SkeletonPanels(3, 360) }
                state.error != null && d == null -> item(key = "error") { ErrorState(state.error.orEmpty(), onRetry = vm::retry) }
                d == null -> Unit
                !d.hasCollection -> item(key = "noCollection") {
                    EmptyState(
                        title = "Add your collection first",
                        message = "Mark the cards you own in Collection and tap + on the ones you have spare. This tab then shows who wants them.",
                        actionText = "Open Collection",
                        onAction = onOpenCollection,
                    )
                }
                d.spareCards == 0 -> item(key = "noSpares") {
                    EmptyState(
                        title = "Mark your spares",
                        message = "In Collection, tap + on a card when you have more than one copy. Spares are what you can trade away.",
                        actionText = "Open Collection",
                        onAction = onOpenCollection,
                    )
                }
                else -> {
                    item(key = "status") { MatchedBanner(d.spareCards, onUpdateSpares = onOpenCollection) }
                    if (d.swaps.isEmpty() && d.help.isEmpty()) {
                        item(key = "nothing") {
                            EmptyState(
                                title = "None of your spares are wanted right now",
                                message = "New wants come in all the time. Check back later, or look through every want.",
                                actionText = "See all wants",
                                onAction = { onOpenTab(WantedTab.All) },
                            )
                        }
                    }
                    if (d.swaps.isNotEmpty()) {
                        item(key = "swapTitle") {
                            SectionTitle(
                                "Swap now",
                                "They want a spare you have, and they have a spare you're missing.",
                                tag = if (d.swaps.size == 1) "1 two-way match" else "${d.swaps.size} two-way matches",
                            )
                        }
                        items(d.swaps, key = { "swap-" + it.request.requestId }) { swap ->
                            SwapCard(
                                swap = swap,
                                onPropose = {
                                    gate.require {
                                        onOffer(Detail.MakeOffer(swap.request.requestId, swap.request.cardId, swap.request.userFriendId.orEmpty(), want = swap.get.cardId))
                                    }
                                },
                                onMore = { onOpenTab(WantedTab.Traders) },
                            )
                        }
                    }
                    if (d.help.isNotEmpty()) {
                        item(key = "helpTitle") {
                            SectionTitle("You have what they want", "You have a spare of these. Offer it and pick cards of the same rarity you'd like back.")
                        }
                        // Two per row, like the card grids elsewhere
                        items(d.help.chunked(2), key = { row -> "help-" + row.first().request.requestId }) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { help ->
                                    HelpTile(
                                        help = help,
                                        onOffer = {
                                            gate.require { onOffer(Detail.MakeOffer(help.request.requestId, help.request.cardId, help.request.userFriendId.orEmpty())) }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        if (state.reloading) ReloadBar(Modifier.align(Alignment.TopCenter))
    }
}

/** Shown on For you and in the Traders banner details. */
private const val SPARES_PUBLIC_NOTE = "Other players can see which cards you marked as spare."

@Composable
private fun MatchedBanner(spareCards: Int, onUpdateSpares: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TcgColors.Surface)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TcgColors.OkText)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Matched against your $spareCards ${if (spareCards == 1) "card" else "cards"} with spares. " +
                    "Newest wants first; your own wants never show here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TcgColors.Ink,
            )
            Text(SPARES_PUBLIC_NOTE, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
            TcgButton("Update spares", onClick = onUpdateSpares, style = TcgButtonStyle.Ghost)
        }
    }
}

@Composable
private fun SwapCard(swap: WantedSwap, onPropose: () -> Unit, onMore: () -> Unit) {
    val who = playerLabel(swap.request.userFriendId)
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(playerInitials(swap.request.userFriendId), size = 40.dp)
                Column {
                    Text(who, style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
                    val meta = listOf(activeText(swap.traderActiveAt), askedText(swap.request.creationDate)).filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                }
            }
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SwapSide("You give", swap.give, sparesText(swap.mySpares), TcgColors.OkText, highlight = false, Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = null,
                    tint = TcgColors.Red,
                    modifier = Modifier.padding(top = 90.dp).size(28.dp),
                )
                SwapSide("You get", swap.get, "Missing from your collection", TcgColors.RedInk, highlight = true, Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TcgButton("Propose this swap", onClick = onPropose, modifier = Modifier.fillMaxWidth())
                if (swap.moreWithTrader > 0) {
                    TcgButton(
                        "${swap.moreWithTrader} more ${if (swap.moreWithTrader == 1) "swap" else "swaps"} with $who",
                        onClick = onMore,
                        style = TcgButtonStyle.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwapSide(label: String, card: WantedCard, note: String, noteColor: androidx.compose.ui.graphics.Color, highlight: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (highlight) TcgColors.RedInk else TcgColors.Muted,
        )
        CardArt(card.illustrationUrl, card.name, Modifier.fillMaxWidth(), selected = highlight)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(card.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            RarityText(card.rarityName, size = 10)
        }
        Text(note, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = noteColor)
    }
}

@Composable
private fun HelpTile(help: WantedHelp, onOffer: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardArt(help.give.illustrationUrl, help.give.name, Modifier.fillMaxWidth())
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(help.give.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                RarityText(help.give.rarityName, size = 11)
            }
            Text("Wanted by ${playersText(help.wantedBy)}", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
            Text(
                "${playerLabel(help.request.userFriendId)}, ${askedText(help.request.creationDate)}",
                style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted, maxLines = 2,
            )
            Spacer(Modifier.height(2.dp))
            SpareChip(sparesText(help.mySpares))
        }
        TcgButton("Offer a trade", onClick = onOffer, style = TcgButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------------------
// By card
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ByCardTab(
    header: @Composable () -> Unit,
    filters: @Composable () -> Unit,
    applied: WantedFilters,
    sparesOf: (String) -> Int,
    onOffer: (Detail.MakeOffer) -> Unit,
) {
    val vm = koinViewModel<ByCardViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val gate = LocalFriendIdGate.current

    LaunchedEffect(applied) { vm.apply(applied) }
    LoadMoreWhenNearEnd(listState, state.cards.size, onLoadMore = vm::loadMore)

    // New filters or sort start at the top; coming back to the tab keeps the place
    val listKey = "$applied|${state.sort}"
    var shownKey by rememberSaveable { mutableStateOf(listKey) }
    LaunchedEffect(listKey) {
        if (listKey != shownKey) {
            shownKey = listKey
            listState.scrollToItem(0)
        }
    }

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = ListPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    header()
                    filters()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (!state.loading && state.error == null) "${state.totalElements} ${if (state.totalElements == 1L) "card" else "cards"} wanted" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TcgColors.Muted,
                            modifier = Modifier.weight(1f),
                        )
                        FilterPicker(
                            label = "Sort",
                            value = if (state.sort == "popular") "Most players" else "Newest want",
                            options = listOf("newest" to "Newest want", "popular" to "Most players"),
                            onSelect = vm::setSort,
                            modifier = Modifier.width(170.dp),
                        )
                    }
                }
            }
            if (state.error != null && state.cards.isNotEmpty()) {
                item(key = "reloadError") { InlineErrorBanner(state.error.orEmpty(), onRetry = vm::retry) }
            }
            when {
                state.loading && state.cards.isEmpty() -> item(key = "loading") { SkeletonPanels(4, 170) }
                state.error != null && state.cards.isEmpty() -> item(key = "error") { ErrorState(state.error.orEmpty(), onRetry = vm::retry) }
                state.cards.isEmpty() -> item(key = "empty") {
                    EmptyState(title = "No wanted cards match", message = "Try another name or clear the filters.")
                }
                else -> {
                    items(state.cards, key = { it.cardId }) { card ->
                        CardDemandTile(card, spares = sparesOf(card.cardId), onOpen = { vm.open(card) })
                    }
                    if (state.loadingMore) item(key = "more") { SkeletonPanels(1, 170) }
                    if (state.loadMoreFailed) {
                        item(key = "moreFailed") {
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
        if (state.loading && state.cards.isNotEmpty() && !state.refreshing) ReloadBar(Modifier.align(Alignment.TopCenter))
    }

    state.selected?.let { card ->
        WhoWantsItSheet(
            card = card,
            spares = sparesOf(card.cardId),
            state = state,
            onRetry = vm::retryMarket,
            onDismiss = vm::close,
            onOffer = { want ->
                gate.require {
                    vm.close()
                    onOffer(Detail.MakeOffer(want.requestId, card.cardId, want.userFriendId.orEmpty()))
                }
            },
        )
    }
}

@Composable
private fun CardDemandTile(card: CardDemand, spares: Int, onOpen: () -> Unit) {
    // One announcement for the whole tile instead of each text, the art and the arrow
    val description = buildString {
        append(card.cardName)
        append(", ${playersText(card.wantedBy)} ${if (card.wantedBy == 1L) "wants" else "want"} it")
        if (spares > 0) append(", ${sparesText(spares).replaceFirstChar { it.lowercase() }}")
        append(". See who wants it.")
    }
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = TcgColors.Surface,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick { onOpen(); true }
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CardArt(card.imageUrl, card.cardName, Modifier.width(88.dp), sleeve = false, corner = 8.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(card.cardName, style = MaterialTheme.typography.titleLarge, color = TcgColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RarityText(card.rarity, size = 11)
                        Text(expansionName(card.expansion), style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted, maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${card.wantedBy}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = TcgColors.Ink)
                        Text(
                            if (card.wantedBy == 1L) "player wants it" else "players want it",
                            style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    latestWantText(card.latest).takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                    }
                }
            }
            HorizontalDivider(color = TcgColors.LineSoft)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (spares > 0) SpareChip(sparesText(spares))
                Spacer(Modifier.weight(1f))
                Text("See who wants it", style = MaterialTheme.typography.labelLarge, color = TcgColors.RedInk)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TcgColors.RedInk)
            }
        }
    }
}

/** Opened from By card: the players who want this card, newest first, with what each could give back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhoWantsItSheet(
    card: CardDemand,
    spares: Int,
    state: ByCardState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOffer: (CardWant) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = TcgColors.Surface,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        ) {
            item(key = "title") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CardArt(card.imageUrl, card.cardName, Modifier.width(64.dp), sleeve = false, corner = 6.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(card.cardName, style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink)
                        val wanted = state.market?.wantedCount ?: card.wantedBy
                        Text(
                            "${playersText(wanted)} ${if (wanted == 1L) "wants" else "want"} it." + if (spares > 0) " ${sparesText(spares)}." else "",
                            style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "NEWEST FIRST",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TcgColors.Muted,
                )
            }
            val market = state.market
            when {
                state.marketLoading -> item(key = "loading") {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) { Skeleton(Modifier.fillMaxWidth().height(56.dp)) }
                    }
                }
                state.marketError != null -> item(key = "error") { ErrorState(state.marketError, onRetry = onRetry) }
                market == null || market.wants.isEmpty() -> item(key = "none") {
                    Text("Nobody wants it right now.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted, modifier = Modifier.padding(top = 12.dp))
                }
                else -> {
                    items(market.wants, key = { it.requestId }) { want -> WantRow(want, onOffer = { onOffer(want) }) }
                    if (market.wantedCount > market.wants.size) {
                        item(key = "more") {
                            Text(
                                "Showing the newest ${market.wants.size} of ${market.wantedCount}.",
                                style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WantRow(want: CardWant, onOffer: () -> Unit) {
    Column {
        HorizontalDivider(color = TcgColors.LineSoft)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(playerInitials(want.userFriendId), size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val meta = listOfNotNull(want.language, timeAgo(want.creationDate).ifBlank { null }).joinToString(" · ")
                Text(
                    playerLabel(want.userFriendId) + if (meta.isNotBlank()) " · $meta" else "",
                    style = MaterialTheme.typography.bodyMedium, color = TcgColors.Ink, maxLines = 2,
                )
                if (want.canGive != null) {
                    Text("Can give you ${want.canGive}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TcgColors.OkText)
                } else {
                    Text(offersText(want.offerCount), style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                }
            }
            // Each row's button says whom it is for, so a list of "Offer" buttons is clear to a screen reader
            TcgButton(
                "Offer",
                onClick = onOffer,
                style = TcgButtonStyle.Outline,
                modifier = Modifier.semantics { contentDescription = "Offer a trade to ${playerLabel(want.userFriendId)}" },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Traders
// ---------------------------------------------------------------------------

/** Trade lines shown per player before "Show N more". */
private const val PREVIEW_LINES = 2

/** What one trade line shows besides the line itself; compared by value so other lines don't redraw. */
@Immutable
internal data class TradeLineUi(
    val unpicked: Set<String> = emptySet(),
    val sending: Boolean = false,
    val sent: Boolean = false,
    /** What the server said when it refused the offer (already offered, already traded). */
    val refused: String? = null,
)

/** One trader's lines, by requestId. Toggling a card on one trader leaves the other cards untouched. */
@Immutable
internal data class TraderUi(val lines: Map<String, TradeLineUi>) {
    fun of(line: TradeLine): TradeLineUi = lines[line.requestId] ?: TradeLineUi()
}

private fun TradersState.uiOf(trader: WantedTrader): TraderUi = TraderUi(
    trader.give.associate { line ->
        val id = line.requestId
        id to TradeLineUi(unpicked[id].orEmpty(), id in sending, id in sent, refused[id])
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TradersTab(
    header: @Composable () -> Unit,
    onOffer: (Detail.MakeOffer) -> Unit,
    onShowPlayer: (String) -> Unit,
    onOpenCollection: () -> Unit,
    onOpenTab: (WantedTab) -> Unit,
    onSeeOffers: () -> Unit,
) {
    val vm = koinViewModel<TradersViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val gate = LocalFriendIdGate.current

    // Trades follow the collection and the offers sent, so the tab reloads when it shows again
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { }
    }

    // The offer page, for lines without suggestions or to pick other cards
    fun openOffer(trader: WantedTrader, line: TradeLine, want: List<String>) = gate.require {
        onOffer(
            Detail.MakeOffer(
                line.requestId, line.give.cardId, trader.userFriendId,
                want = want.take(MAX_PICKS).joinToString(",").ifEmpty { null },
            )
        )
    }

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = ListPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            headerItem(header)
            val d = state.data
            if (state.error != null && d != null) {
                item(key = "reloadError") { InlineErrorBanner(state.error.orEmpty(), onRetry = vm::retry) }
            }
            when {
                state.loading -> item(key = "loading") { SkeletonPanels(3, 320) }
                state.error != null && d == null -> item(key = "error") { ErrorState(state.error.orEmpty(), onRetry = vm::retry) }
                d == null -> Unit
                d.spareCards == 0 -> item(key = "noSpares") {
                    EmptyState(
                        title = if (d.hasCollection) "Mark your spares" else "Add your collection first",
                        message = "In Collection, tap + on the cards you have more than one copy of. Players who want several of them show up here.",
                        actionText = "Open Collection",
                        onAction = onOpenCollection,
                    )
                }
                else -> {
                    item(key = "keep") { KeepCollectionBanner(d.collectionUpdatedAt, onOpenCollection) }
                    if (d.traders.isEmpty()) {
                        item(key = "none") {
                            EmptyState(
                                title = "No player wants several of your spares yet",
                                message = "Single trades are on For you. When one player wants 2 or more of your spares, they show up here.",
                                actionText = "Open For you",
                                onAction = { onOpenTab(WantedTab.ForYou) },
                            )
                        }
                    }
                    items(d.traders, key = { it.userFriendId }) { trader ->
                        TraderCard(
                            trader = trader,
                            ui = state.uiOf(trader),
                            onToggle = vm::togglePick,
                            // The gate asks for a Friend ID first when there is none, then sends
                            onSend = { line -> gate.require { vm.send(trader, line) } },
                            onOpenOffer = { line, want -> openOffer(trader, line, want) },
                            onShowPlayer = { onShowPlayer(trader.userFriendId) },
                            onSeeOffers = onSeeOffers,
                        )
                    }
                }
            }
        }
        if (state.reloading) ReloadBar(Modifier.align(Alignment.TopCenter))
    }
}

/**
 * Every trade here is read from the collection, so remind to keep it current, with a way to fix it.
 * One line with Update; tapping the line shows why.
 */
@Composable
private fun KeepCollectionBanner(updatedAt: String?, onOpenCollection: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TcgColors.Surface)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable(onClickLabel = if (open) "Hide details" else "Show details") { open = !open }
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Update, contentDescription = null, tint = TcgColors.Red, modifier = Modifier.size(20.dp))
                Text(
                    "Keep your collection up to date",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TcgColors.Ink,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TcgColors.Muted,
                    modifier = Modifier.size(18.dp),
                )
            }
            TcgButton("Update", onClick = onOpenCollection, style = TcgButtonStyle.Ghost)
        }
        if (open) {
            val updated = withLastUpdated(updatedAt)
            Text(
                "These trades are worked out from your Collection: what you have spare and what you're missing. " +
                    "If it's out of date, you could offer a card you no longer have or ask for one you already own." +
                    (if (updated.isNotEmpty()) " $updated." else "") +
                    " $SPARES_PUBLIC_NOTE",
                style = MaterialTheme.typography.bodySmall,
                color = TcgColors.Muted,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
        }
    }
}

private fun withLastUpdated(value: String?): String {
    val ago = timeAgo(value)
    return when {
        ago.isEmpty() -> ""
        ago.endsWith(" ago") || ago == "Just now" || ago == "Yesterday" -> "Last updated ${ago.lowercase()}"
        else -> "Last updated on $ago"
    }
}

@Composable
private fun TraderCard(
    trader: WantedTrader,
    ui: TraderUi,
    onToggle: (TradeLine, String) -> Unit,
    onSend: (TradeLine) -> Unit,
    onOpenOffer: (TradeLine, List<String>) -> Unit,
    onShowPlayer: () -> Unit,
    onSeeOffers: () -> Unit,
) {
    var expanded by rememberSaveable(trader.userFriendId) { mutableStateOf(false) }
    val lines = if (expanded) trader.give else trader.give.take(PREVIEW_LINES)
    val who = playerLabel(trader.userFriendId)

    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(playerInitials(trader.userFriendId), size = 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(who, style = MaterialTheme.typography.titleMedium, color = TcgColors.Ink)
                    val meta = listOf(
                        activeText(trader.activeAt),
                        "${trader.openWants} open ${if (trader.openWants == 1L) "want" else "wants"}",
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                    if (trader.tradesDone > 0) {
                        SpareChip(if (trader.tradesDone == 1L) "1 trade done" else "${trader.tradesDone} trades done")
                    }
                }
            }

            Text(
                if (trader.trades > 0) "RECOMMENDED TRADES · ${trader.trades}" else "RECOMMENDED TRADES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TcgColors.Muted,
            )
            lines.forEach { line ->
                key(line.requestId) {
                    TradeLineCard(
                        line = line,
                        who = who,
                        ui = ui.of(line),
                        onToggle = { cardId -> onToggle(line, cardId) },
                        onSend = { onSend(line) },
                        onOpenOffer = { want -> onOpenOffer(line, want) },
                        onSeeOffers = onSeeOffers,
                    )
                }
            }

            val hidden = trader.give.size - PREVIEW_LINES
            if (hidden > 0) {
                TcgButton(
                    if (expanded) "Show fewer" else "Show $hidden more ${if (hidden == 1) "trade" else "trades"}",
                    onClick = { expanded = !expanded },
                    style = TcgButtonStyle.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TcgButton("See all their wants", onClick = onShowPlayer, style = TcgButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * They want one of my spares. The suggested cards (same rarity, ones I don't own) start picked; tapping one
 * leaves it out, and Send offer sends it from here.
 */
@Composable
private fun TradeLineCard(
    line: TradeLine,
    who: String,
    ui: TradeLineUi,
    onToggle: (String) -> Unit,
    onSend: () -> Unit,
    onOpenOffer: (List<String>) -> Unit,
    onSeeOffers: () -> Unit,
) {
    // At most 5 are shown, and only those can be picked
    val suggest = line.suggest.orEmpty().take(MAX_PICKS)
    val sending = ui.sending
    val picked = pickedCards(line, ui.unpicked)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TcgColors.Soft).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardArt(line.give.illustrationUrl, line.give.name, Modifier.width(52.dp), sleeve = false, corner = 6.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "THEY WANT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TcgColors.Muted,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        line.give.name, style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    RarityText(line.give.rarityName, size = 10)
                }
                Text(
                    if (line.mySpares > 0) sparesText(line.mySpares) else "You have it spare",
                    style = MaterialTheme.typography.bodySmall, color = TcgColors.OkText,
                )
            }
        }

        if (ui.sent) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TcgColors.OkText)
                Text(
                    ui.refused ?: "Offer sent to $who.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TcgColors.OkText,
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            TcgButton("See your offers", onClick = onSeeOffers, style = TcgButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            return@Column
        }

        // null: an older server that sends no suggestions, so say nothing about them
        when {
            line.suggest == null -> Unit
            suggest.isEmpty() ->
                Text("You own every tradable card of this rarity.", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
            else -> {
                Text(
                    "ASK FOR ONE OF THESE · YOU DON'T HAVE THEM",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TcgColors.Muted,
                )
                // Five equal slots across the line, so every card fits on a phone without scrolling
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggest.forEach { s ->
                        key(s.card.cardId) {
                            SuggestThumb(
                                s,
                                picked = s.card.cardId !in ui.unpicked,
                                enabled = !sending,
                                onClick = { onToggle(s.card.cardId) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    repeat(MAX_PICKS - suggest.size) { Spacer(Modifier.weight(1f)) }
                }
                Text(
                    if (picked.isEmpty()) "Pick at least one card." else "Tap a card to leave it out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TcgColors.Muted,
                )
            }
        }

        if (suggest.isNotEmpty()) {
            TcgButton(
                if (picked.size > 1) "Send offer · they pick 1 of ${picked.size}" else "Send offer",
                onClick = onSend,
                enabled = picked.isNotEmpty() && !sending,
                loading = sending,
                loadingText = "Sending…",
                modifier = Modifier.fillMaxWidth(),
            )
            TcgButton(
                "Pick other cards",
                onClick = { onOpenOffer(picked.map { it.cardId }) },
                style = TcgButtonStyle.Ghost,
                enabled = !sending,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TcgButton("Offer a trade", onClick = { onOpenOffer(listOfNotNull(line.get?.cardId)) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SuggestThumb(s: WantedSuggest, picked: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val (tone, badge) = when (s.reason) {
        "THEIR_SPARE" -> TcgColors.OkText to "Has spare"
        "YOUR_WANT" -> TcgColors.Red to "You want"
        else -> TcgColors.Line to null
    }
    val why = when (s.reason) {
        "THEIR_SPARE" -> ", they have it spare"
        "YOUR_WANT" -> ", on your wants"
        else -> ""
    }
    Box(
        modifier
            .clip(shape)
            .border(if (badge != null && picked) 2.dp else 1.dp, if (picked) tone else TcgColors.Line, shape)
            // Read once as "Pikachu, they have it spare, selected" (not the art and the badge again)
            .clearAndSetSemantics {
                contentDescription = s.card.name + why
                role = Role.Checkbox
                toggleableState = ToggleableState(picked)
                stateDescription = if (picked) "selected" else "not selected"
                if (enabled) onClick { onClick(); true } else disabled()
            }
            .toggleable(value = picked, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() }),
    ) {
        CardArt(
            s.card.illustrationUrl, s.card.name,
            Modifier.fillMaxWidth().alpha(if (picked) 1f else 0.45f),
            sleeve = false, corner = 6.dp,
        )
        // A filled check when picked, an empty circle when left out: both say "tap me"
        Box(
            Modifier.align(Alignment.TopEnd).padding(3.dp).size(18.dp).clip(CircleShape)
                .background(if (picked) TcgColors.Ink else TcgColors.Surface.copy(alpha = 0.85f))
                .border(if (picked) 2.dp else 1.5.dp, if (picked) TcgColors.Surface else TcgColors.Muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (picked) Icon(Icons.Default.Check, contentDescription = null, tint = TcgColors.Surface, modifier = Modifier.size(11.dp))
        }
        if (badge != null) {
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = TcgColors.Surface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().alpha(if (picked) 1f else 0.55f)
                    .background(tone).padding(vertical = 2.dp),
            )
        }
    }
}
