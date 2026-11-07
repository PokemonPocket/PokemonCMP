package com.tcgptrading.tcgptrading.features.trading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcgptrading.tcgptrading.app.domain.Detail
import com.tcgptrading.tcgptrading.core.data.model.Card
import com.tcgptrading.tcgptrading.core.presentation.components.CARD_RATIO
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.Panel
import com.tcgptrading.tcgptrading.core.presentation.components.RarityText
import com.tcgptrading.tcgptrading.core.presentation.components.SearchField
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.expansionName
import com.tcgptrading.tcgptrading.core.util.rarityOf
import com.tcgptrading.tcgptrading.main.presentation.LocalFriendIdGate
import com.tcgptrading.tcgptrading.main.presentation.LocalSnackbar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Offer a trade: you give the wanted card and pick up to 5 cards of the same rarity
 * you would like back. The collector chooses one of them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeOfferScreen(route: Detail.MakeOffer, onBack: () -> Unit, onSent: () -> Unit) {
    val vm = koinViewModel<MakeOfferViewModel>(key = route.requestId) { parametersOf(route) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val gate = LocalFriendIdGate.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                // showSnackbar waits until the message is gone, so it runs apart from the events
                is MakeOfferEvent.Message -> scope.launch { snackbar.showSnackbar(event.text) }
                MakeOfferEvent.Sent -> onSent()
            }
        }
    }

    LaunchedEffect(gridState, state.cards.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                if (state.cards.isNotEmpty() && last >= gridState.layoutInfo.totalItemsCount - 6) vm.loadMore()
            }
    }

    Scaffold(
        containerColor = TcgColors.Ground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Offer a trade", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to wanted cards")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TcgColors.Ground, titleContentColor = TcgColors.Ink),
                windowInsets = WindowInsets(0),
            )
        },
        bottomBar = {
            if (state.wanted != null) {
                OfferSummaryBar(
                    picked = state.picked,
                    sending = state.sending,
                    onSend = { gate.require { vm.send() } },
                    onCancel = onBack,
                )
            }
        },
    ) { padding ->
        when {
            state.wantedLoading -> Box(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Skeleton(Modifier.width(140.dp).aspectRatio(CARD_RATIO), corner = 14.dp)
            }

            state.wanted == null -> Box(Modifier.fillMaxSize().padding(padding)) {
                ErrorState(message = state.wantedError ?: "This card could not be loaded.", onRetry = vm::loadWanted)
            }

            else -> LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 104.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                val wanted = state.wanted!!
                fullSpan("wanted") { WantedCardHeader(wanted) }
                fullSpan("pickHeader") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Text("What would you like in return?", style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink)
                        Text(
                            "Pick up to $MAX_PICKS cards. Tap a card again to remove it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TcgColors.Muted,
                        )
                        SearchField(
                            value = state.query,
                            onValueChange = vm::setQuery,
                            placeholder = "Search ${rarityOf(wanted.rarityName ?: wanted.rarity)?.glyphs ?: ""} cards".replace("  ", " "),
                        )
                    }
                }

                when {
                    state.loading && state.cards.isEmpty() -> items(9, key = { "sk$it" }) {
                        Skeleton(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO), corner = 12.dp)
                    }

                    state.error != null && state.cards.isEmpty() -> fullSpan("error") {
                        ErrorState(message = state.error.orEmpty(), onRetry = vm::retryCards, modifier = Modifier.padding(0.dp))
                    }

                    state.cards.isEmpty() -> fullSpan("empty") {
                        EmptyState(
                            title = "No cards found",
                            message = if (state.query.isBlank()) "There are no other tradable cards of this rarity." else "Try another name.",
                        )
                    }

                    else -> {
                        items(state.cards, key = { it.cardId }) { card ->
                            val order = state.picked.indexOfFirst { it.cardId == card.cardId }
                            PickTile(card = card, order = if (order >= 0) order + 1 else null, onToggle = { vm.toggle(card) })
                        }
                        if (state.loadingMore) {
                            items(3, key = { "more$it" }) { Skeleton(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO), corner = 12.dp) }
                        }
                        if (state.loadMoreFailed) {
                            fullSpan("moreFailed") {
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
}

private fun LazyGridScope.fullSpan(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** The card you give, with the same-rarity rule explained. */
@Composable
private fun WantedCardHeader(card: Card) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CardArt(imageUrl = card.illustrationUrl, name = card.name, modifier = Modifier.width(132.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("You give", style = MaterialTheme.typography.labelMedium, color = TcgColors.Muted)
                Text(card.name, style = MaterialTheme.typography.headlineMedium, color = TcgColors.Ink)
                RarityText(card.rarityName ?: card.rarity, size = 14)
                expansionName(card.expansion).takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
                }
            }
        }
        Panel(padding = PaddingValues(14.dp)) {
            Text(
                "You give this card. Pick up to $MAX_PICKS cards of the same rarity you would like back; the collector chooses one.",
                style = MaterialTheme.typography.bodyMedium,
                color = TcgColors.Muted,
            )
        }
    }
}

/** A card you could ask for in return; picked cards show their pick number. */
@Composable
private fun PickTile(card: Card, order: Int?, onToggle: () -> Unit) {
    val selected = order != null
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics {
                this.selected = selected
                contentDescription = card.name + if (selected) ", picked $order of $MAX_PICKS" else ""
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            CardArt(imageUrl = card.illustrationUrl, name = card.name, modifier = Modifier.fillMaxWidth(), selected = selected)
            if (order != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(28.dp)
                        .clip(CircleShape).background(Color.White).padding(2.dp)
                        .clip(CircleShape).background(TcgColors.Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$order", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(
            card.name,
            style = MaterialTheme.typography.labelMedium,
            color = TcgColors.Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

/** Sticky bottom bar: what you would get, Cancel and Send offer. */
@Composable
private fun OfferSummaryBar(picked: List<Card>, sending: Boolean, onSend: () -> Unit, onCancel: () -> Unit) {
    Surface(color = TcgColors.Surface, shadowElevation = 8.dp) {
        Column {
            HorizontalDivider(color = TcgColors.LineSoft)
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (picked.isEmpty()) {
                    Text("No cards picked yet", style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
                } else {
                    Text(
                        "You would get one of: " + picked.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TcgColors.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TcgButton("Cancel", onClick = onCancel, style = TcgButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    TcgButton(
                        "Send offer",
                        onClick = onSend,
                        enabled = picked.isNotEmpty(),
                        loading = sending,
                        loadingText = "Sending…",
                        modifier = Modifier.weight(2f),
                    )
                }
            }
        }
    }
}
