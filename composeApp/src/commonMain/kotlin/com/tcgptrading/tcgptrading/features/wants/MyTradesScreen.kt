package com.tcgptrading.tcgptrading.features.wants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.model.CardRef
import com.tcgptrading.tcgptrading.core.data.model.Offer
import com.tcgptrading.tcgptrading.core.data.model.TradeRequest
import com.tcgptrading.tcgptrading.core.data.runApi
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.ConfirmDialog
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.Panel
import com.tcgptrading.tcgptrading.core.presentation.components.RarityText
import com.tcgptrading.tcgptrading.core.presentation.components.ScreenHeader
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.Tag
import com.tcgptrading.tcgptrading.core.presentation.components.TagTone
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.components.TradeSteps
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.offersText
import com.tcgptrading.tcgptrading.core.util.playerLabel
import com.tcgptrading.tcgptrading.core.util.timeAgo
import com.tcgptrading.tcgptrading.features.trading.ReloadPolicy
import com.tcgptrading.tcgptrading.features.trading.TradingSignals
import com.tcgptrading.tcgptrading.main.presentation.LocalSnackbar
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel

/** Accepted trades must be finished in the game within this many days. */
private const val TRADE_EXPIRY_DAYS = 3

data class MyTradesState(
    val offers: List<Offer> = emptyList(),
    val wants: List<TradeRequest> = emptyList(),
    val loadingOffers: Boolean = true,
    val loadingWants: Boolean = true,
    val offersError: String? = null,
    val wantsError: String? = null,
    val refreshing: Boolean = false,
    /** Wanted card names by cardId, fetched once. */
    val cardNames: Map<String, String> = emptyMap(),
    /** Ids (offer or want) with an action in progress. */
    val busy: Set<String> = emptySet(),
)

sealed interface MyTradesEvent {
    data class OpenChat(val email: String) : MyTradesEvent
    data class Message(val text: String) : MyTradesEvent
}

class MyTradesViewModel(private val api: TradingApi, private val signals: TradingSignals) : ViewModel() {

    private val _state = MutableStateFlow(MyTradesState())
    val state: StateFlow<MyTradesState> = _state.asStateFlow()

    private val _events = Channel<MyTradesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val reload = ReloadPolicy()

    init {
        loadOffers()
        loadWants()
        // An offer sent from Wanted shows up here at once
        viewModelScope.launch { signals.offerSent.drop(1).collect { loadOffers() } }
    }

    /** Coming back to the screen: pick up offers accepted or declined meanwhile, without reloading on every switch. */
    fun onResume() {
        val s = _state.value
        if (reload.shouldReloadOnResume(s.loadingOffers || s.loadingWants || s.refreshing)) refresh()
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val offers = launch { fetchOffers() }
            val wants = launch { fetchWants() }
            offers.join(); wants.join()
            _state.update { it.copy(refreshing = false) }
        }
    }

    fun loadOffers() {
        _state.update { it.copy(loadingOffers = true, offersError = null) }
        viewModelScope.launch { fetchOffers() }
    }

    fun loadWants() {
        _state.update { it.copy(loadingWants = true, wantsError = null) }
        viewModelScope.launch { fetchWants() }
    }

    private suspend fun fetchOffers() {
        runApi { api.myOffers(page = 0, size = 100) }
            .onSuccess { page ->
                _state.update { it.copy(offers = page.content, loadingOffers = false, offersError = null) }
                reload.loaded()
                loadCardNames(page.content.map { it.cardId })
            }
            .onFailure { e -> _state.update { it.copy(loadingOffers = false, offersError = e.message) } }
    }

    private suspend fun fetchWants() {
        runApi { api.myWants(page = 0, size = 100) }
            .onSuccess { page -> _state.update { it.copy(wants = page.content, loadingWants = false, wantsError = null) } }
            .onFailure { e -> _state.update { it.copy(loadingWants = false, wantsError = e.message) } }
    }

    /** Offers only carry the wanted card's id, so its name is looked up once per card. */
    private fun loadCardNames(ids: List<String>) {
        val missing = ids.distinct().filter { it.isNotBlank() && it !in _state.value.cardNames }
        missing.forEach { id ->
            viewModelScope.launch {
                runApi { api.card(id) }.onSuccess { card ->
                    _state.update { it.copy(cardNames = it.cardNames + (id to card.name)) }
                }
            }
        }
    }

    fun withdraw(offer: Offer) = withBusy(offer.id) {
        runApi { api.withdrawOffer(offer.id) }
            .onSuccess {
                _state.update { s -> s.copy(offers = s.offers.filterNot { it.id == offer.id }) }
                _events.send(MyTradesEvent.Message("Offer withdrawn."))
            }
            .onFailure { _events.send(MyTradesEvent.Message(it.message ?: "Could not withdraw the offer.")) }
    }

    /** Takes a declined offer off my list; the server deletes it (it no longer counts on the want). */
    fun remove(offer: Offer) = withBusy(offer.id) {
        runApi { api.withdrawOffer(offer.id) }
            .onSuccess {
                _state.update { s -> s.copy(offers = s.offers.filterNot { it.id == offer.id }) }
                _events.send(MyTradesEvent.Message("Removed."))
            }
            .onFailure { _events.send(MyTradesEvent.Message(it.message ?: "Could not remove the offer.")) }
    }

    fun removeWant(want: TradeRequest) = withBusy(want.requestId) {
        runApi { api.removeWant(want.requestId) }
            .onSuccess {
                _state.update { s -> s.copy(wants = s.wants.filterNot { it.requestId == want.requestId }) }
                _events.send(MyTradesEvent.Message("${want.cardName} removed from your wants."))
            }
            .onFailure { _events.send(MyTradesEvent.Message(it.message ?: "Could not remove the want.")) }
    }

    /** On my offer, the partner is the want's owner (requester). */
    fun openChatForOffer(offer: Offer) = withBusy(offer.id) {
        openChatWith(offer.requesterFriendId)
    }

    /** On my traded want, the partner is the bidder of the accepted offer. */
    fun openChatForWant(want: TradeRequest) = withBusy(want.requestId) {
        val offerId = want.acceptedOffer
        if (offerId == null) {
            _events.send(MyTradesEvent.Message("Could not find the trade for this card."))
            return@withBusy
        }
        runApi { api.offer(offerId) }
            .onSuccess { openChatWith(it.bidderFriendId) }
            .onFailure { _events.send(MyTradesEvent.Message(it.message ?: "Could not open the chat.")) }
    }

    private suspend fun openChatWith(friendId: String?) {
        if (friendId.isNullOrBlank()) {
            _events.send(MyTradesEvent.Message("Could not find the other player."))
            return
        }
        runApi { api.emailOf(friendId) }
            .onSuccess { email ->
                if (email.isBlank()) _events.send(MyTradesEvent.Message("Could not find the other player."))
                else _events.send(MyTradesEvent.OpenChat(email.trim()))
            }
            .onFailure { _events.send(MyTradesEvent.Message(it.message ?: "Could not open the chat.")) }
    }

    private fun withBusy(id: String, block: suspend () -> Unit) {
        if (id in _state.value.busy) return
        _state.update { it.copy(busy = it.busy + id) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _state.update { it.copy(busy = it.busy - id) }
            }
        }
    }
}

/** My offers on other players' wants, and my own wants with their offers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTradesScreen(
    initialTab: String,
    onReviewOffers: (requestId: String, cardId: String) -> Unit,
    onOpenChat: (email: String) -> Unit,
    onBrowseWanted: () -> Unit,
    onAddWants: () -> Unit,
) {
    val vm = koinViewModel<MyTradesViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    var tab by rememberSaveable(initialTab) { mutableStateOf(if (initialTab == "wants") "wants" else "offers") }
    var withdrawTarget by remember { mutableStateOf<Offer?>(null) }
    var removeOfferTarget by remember { mutableStateOf<Offer?>(null) }
    var removeTarget by remember { mutableStateOf<TradeRequest?>(null) }

    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is MyTradesEvent.OpenChat -> onOpenChat(event.email)
                is MyTradesEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("My trades")
        SegmentedTabs(
            options = listOf("offers" to "My offers", "wants" to "My wants"),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (tab == "offers") {
                    when {
                        state.loadingOffers -> items(3) { RowSkeleton() }
                        state.offersError != null -> item { ErrorState(state.offersError ?: "", onRetry = vm::loadOffers) }
                        state.offers.isEmpty() -> item {
                            EmptyState(
                                title = "No offers yet",
                                message = "Have a card someone wants? Offer it on the Wanted list and pick what you'd like back.",
                                actionText = "Browse wanted cards",
                                onAction = onBrowseWanted,
                            )
                        }
                        else -> items(state.offers, key = { it.id }) { offer ->
                            OfferRow(
                                offer = offer,
                                wantedName = state.cardNames[offer.cardId] ?: "Card",
                                busy = offer.id in state.busy,
                                onWithdraw = { withdrawTarget = offer },
                                onRemove = { removeOfferTarget = offer },
                                onOpenChat = { vm.openChatForOffer(offer) },
                            )
                        }
                    }
                } else {
                    when {
                        state.loadingWants -> items(3) { RowSkeleton() }
                        state.wantsError != null -> item { ErrorState(state.wantsError ?: "", onRetry = vm::loadWants) }
                        state.wants.isEmpty() -> item {
                            EmptyState(
                                title = "No wants yet",
                                message = "Add the cards you are missing and players who have them will send you offers.",
                                actionText = "Add wants",
                                onAction = onAddWants,
                            )
                        }
                        else -> items(state.wants, key = { it.requestId }) { want ->
                            WantRow(
                                want = want,
                                busy = want.requestId in state.busy,
                                onReview = { onReviewOffers(want.requestId, want.cardId) },
                                onOpenChat = { vm.openChatForWant(want) },
                                onRemove = { removeTarget = want },
                            )
                        }
                    }
                }
            }
        }
    }

    withdrawTarget?.let { offer ->
        ConfirmDialog(
            title = "Withdraw your offer for ${state.cardNames[offer.cardId] ?: "this card"}?",
            message = "The collector will no longer see it. You can make a new offer later.",
            confirmText = "Withdraw offer",
            dismissText = "Keep offer",
            danger = true,
            onConfirm = { withdrawTarget = null; vm.withdraw(offer) },
            onDismiss = { withdrawTarget = null },
        )
    }

    removeOfferTarget?.let { offer ->
        ConfirmDialog(
            title = "Remove this declined offer from your list?",
            message = "It was for ${state.cardNames[offer.cardId] ?: "a card"}. You can make a new offer on that want later.",
            confirmText = "Remove",
            dismissText = "Keep it",
            danger = true,
            onConfirm = { removeOfferTarget = null; vm.remove(offer) },
            onDismiss = { removeOfferTarget = null },
        )
    }

    removeTarget?.let { want ->
        ConfirmDialog(
            title = "Remove ${want.cardName} from your wants?",
            message = "Offers other players made on it are removed too.",
            confirmText = "Remove want",
            dismissText = "Keep it",
            danger = true,
            onConfirm = { removeTarget = null; vm.removeWant(want) },
            onDismiss = { removeTarget = null },
        )
    }
}

/** Two-option pill switch ("My offers" / "My wants"). */
@Composable
internal fun SegmentedTabs(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(TcgColors.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            Surface(
                onClick = { onSelect(key) },
                shape = CircleShape,
                color = if (isSelected) TcgColors.Ink else Color.Transparent,
                contentColor = if (isSelected) Color.White else TcgColors.Ink,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
            ) {
                Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun RowSkeleton() {
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Skeleton(Modifier.width(64.dp).heightIn(min = 90.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Skeleton(Modifier.fillMaxWidth(0.6f).heightIn(min = 18.dp))
                Skeleton(Modifier.fillMaxWidth().heightIn(min = 14.dp))
                Skeleton(Modifier.fillMaxWidth(0.8f).heightIn(min = 14.dp))
            }
        }
    }
}

/** The cards the bidder asked for: exact art when the API sent it, else names. */
private fun Offer.cardRefs(): List<CardRef> =
    requestedCardRefs?.takeIf { it.isNotEmpty() } ?: requestedCards.map { CardRef(name = it) }

private fun Offer.isPicked(ref: CardRef): Boolean =
    if (acceptedCardId != null) acceptedCardId == ref.cardId else acceptedCard != null && acceptedCard == ref.name

private fun daysLeft(tradeDate: String?): Int? {
    val start = tradeDate?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: return null
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return (TRADE_EXPIRY_DAYS - start.daysUntil(today)).coerceAtLeast(0)
}

@Composable
private fun OfferRow(
    offer: Offer,
    wantedName: String,
    busy: Boolean,
    onWithdraw: () -> Unit,
    onRemove: () -> Unit,
    onOpenChat: () -> Unit,
) {
    val partner = playerLabel(offer.requesterFriendId)
    val refs = offer.cardRefs()
    val accepted = offer.status == "ACCEPTED"
    val declined = offer.status == "REJECTED"

    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // You give the wanted card ⇄ one of the cards you asked for
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CardArt(offer.imageUrl, wantedName, Modifier.width(64.dp), sleeve = false, corner = 6.dp)
                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "for", tint = TcgColors.Diamond)
                Row(
                    Modifier.weight(1f).semantics(mergeDescendants = true) {
                        contentDescription = "Cards you asked for in return: " + refs.joinToString { it.name }
                    },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    refs.take(5).forEach { ref ->
                        CardArt(
                            ref.imageUrl, ref.name,
                            Modifier.width(44.dp),
                            corner = 4.dp,
                            sleeve = offer.isPicked(ref),
                            selected = offer.isPicked(ref),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(wantedName, style = MaterialTheme.typography.titleLarge, color = TcgColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val detail = when {
                    accepted && offer.acceptedCard != null -> "You give $wantedName to $partner and get ${offer.acceptedCard}."
                    accepted -> "$partner accepted your offer."
                    // Declined outright, or they took another offer: either way no trade will happen
                    declined -> "You offered it to $partner. They declined it."
                    else -> "You give it to $partner for one of the cards above. Sent ${timeAgo(offer.creationDate).lowercase()}."
                }
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
            }

            if (declined) {
                Tag("Declined", tone = TagTone.Soft)
            } else {
                TradeSteps(reached = if (offer.completedAt != null) 4 else if (accepted) 2 else 0)
            }

            if (accepted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TcgButton("Open chat", onOpenChat, loading = busy)
                    val note = if (offer.completedAt != null) {
                        "Traded ${timeAgo(offer.completedAt).lowercase()}"
                    } else {
                        when (val days = daysLeft(offer.tradeDate)) {
                            null -> null
                            0 -> "Expires today"
                            1 -> "Expires in 1 day"
                            else -> "Expires in $days days"
                        }
                    }
                    if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                }
            } else if (declined) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TcgButton("Remove", onRemove, style = TcgButtonStyle.Secondary, loading = busy)
                    Text("No trade will happen", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TcgButton("Withdraw offer", onWithdraw, style = TcgButtonStyle.Secondary, loading = busy)
                    Text("Waiting for the collector", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun WantRow(
    want: TradeRequest,
    busy: Boolean,
    onReview: () -> Unit,
    onOpenChat: () -> Unit,
    onRemove: () -> Unit,
) {
    val traded = want.cardTraded == true
    val offerCount = want.offerCount ?: 0

    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            CardArt(want.imageUrl, want.cardName, Modifier.width(64.dp), sleeve = false, corner = 6.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        want.cardName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TcgColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    RarityText(want.rarity)
                }
                val added = timeAgo(want.creationDate).lowercase()
                Text(
                    listOfNotNull(want.language, if (added.isNotEmpty()) "added $added" else null).joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TcgColors.Muted,
                )
                Text(
                    if (traded) "Traded" else offersText(offerCount),
                    style = MaterialTheme.typography.titleSmall,
                    color = TcgColors.Ink,
                )
                Box(Modifier.padding(top = 4.dp)) {
                    when {
                        traded -> TcgButton("Open chat", onOpenChat, style = TcgButtonStyle.Outline, loading = busy)
                        offerCount > 0 -> TcgButton("Review offers", onReview)
                        else -> TcgButton("Remove want", onRemove, style = TcgButtonStyle.Secondary, loading = busy)
                    }
                }
            }
        }
    }
}
