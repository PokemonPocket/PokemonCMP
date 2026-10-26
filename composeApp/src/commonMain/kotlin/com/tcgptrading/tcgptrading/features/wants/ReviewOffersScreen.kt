package com.tcgptrading.tcgptrading.features.wants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.app.domain.Detail
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.model.Card
import com.tcgptrading.tcgptrading.core.data.model.CardRef
import com.tcgptrading.tcgptrading.core.data.model.Offer
import com.tcgptrading.tcgptrading.core.data.runApi
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.ConfirmDialog
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.Panel
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.Tag
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.components.TcgChip
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.isWithinHours
import com.tcgptrading.tcgptrading.core.util.playerLabel
import com.tcgptrading.tcgptrading.core.util.timeAgo
import com.tcgptrading.tcgptrading.main.presentation.LocalSnackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class ReviewOffersState(
    val wanted: Card? = null,
    val offers: List<Offer> = emptyList(),
    val sortBy: String = "newest",
    val loading: Boolean = true,
    val error: String? = null,
    /** Picked card per offer id; key = card id, or the name for older offers. */
    val picks: Map<String, String> = emptyMap(),
    /** An accept or decline is in progress. */
    val busy: Boolean = false,
)

sealed interface ReviewOffersEvent {
    data class Accepted(val card: String, val partnerEmail: String?) : ReviewOffersEvent
    data class Message(val text: String) : ReviewOffersEvent
}

/** Same name can exist in several sets, so a pick is keyed by card id when there is one. */
internal fun CardRef.key(): String = cardId ?: name

internal fun Offer.refs(): List<CardRef> =
    requestedCardRefs?.takeIf { it.isNotEmpty() } ?: requestedCards.map { CardRef(name = it) }

class ReviewOffersViewModel(private val api: TradingApi) : ViewModel() {

    private val _state = MutableStateFlow(ReviewOffersState())
    val state: StateFlow<ReviewOffersState> = _state.asStateFlow()

    private val _events = Channel<ReviewOffersEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var route: Detail.ReviewOffers? = null
    private var loadJob: Job? = null

    fun start(route: Detail.ReviewOffers) {
        if (this.route == route) return
        this.route = route
        viewModelScope.launch {
            runApi { api.card(route.cardId) }.onSuccess { card -> _state.update { it.copy(wanted = card) } }
        }
        load()
    }

    fun setSort(sortBy: String) {
        if (sortBy == _state.value.sortBy) return
        _state.update { it.copy(sortBy = sortBy) }
        load()
    }

    fun load() {
        val r = route ?: return
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            runApi { api.offersOnWant(r.requestId, sortBy = _state.value.sortBy) }
                .onSuccess { page ->
                    // Only offers still waiting for an answer can be accepted or declined
                    _state.update { it.copy(offers = page.content.filter { o -> o.status == "PENDING" }, loading = false) }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun togglePick(offer: Offer, ref: CardRef) {
        val key = ref.key()
        _state.update { s ->
            val current = s.picks[offer.id]
            s.copy(picks = if (current == key) s.picks - offer.id else s.picks + (offer.id to key))
        }
    }

    fun pickedRef(offer: Offer, state: ReviewOffersState = _state.value): CardRef? {
        val key = state.picks[offer.id] ?: return null
        return offer.refs().firstOrNull { it.key() == key }
    }

    fun accept(offer: Offer) {
        val ref = pickedRef(offer) ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            // Exact card id for offers that stored ids; older offers only have names
            val cardId = if (!offer.requestedCardIds.isNullOrEmpty()) ref.cardId else null
            runApi { api.acceptOffer(offer.id, ref.name, cardId) }
                .onSuccess {
                    val email = offer.bidderFriendId?.let { id -> runApi { api.emailOf(id) }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() } }
                    _state.update { it.copy(busy = false) }
                    _events.send(ReviewOffersEvent.Accepted(ref.name, email))
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false) }
                    _events.send(ReviewOffersEvent.Message(e.message ?: "Could not accept the offer. Try again."))
                    load()
                }
        }
    }

    fun decline(offer: Offer) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runApi { api.declineOffer(offer.id) }
                .onSuccess {
                    _state.update { s -> s.copy(busy = false, offers = s.offers.filterNot { it.id == offer.id }, picks = s.picks - offer.id) }
                    _events.send(ReviewOffersEvent.Message("Offer declined."))
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false) }
                    _events.send(ReviewOffersEvent.Message(e.message ?: "Could not decline the offer. Try again."))
                }
        }
    }
}

/** Offers other players made on one of my wants: pick the card I'll give back, then accept. */
@Composable
fun ReviewOffersScreen(route: Detail.ReviewOffers, onBack: () -> Unit, onAccepted: (partnerEmail: String?) -> Unit) {
    val vm = koinViewModel<ReviewOffersViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    var acceptTarget by remember { mutableStateOf<Offer?>(null) }
    var declineTarget by remember { mutableStateOf<Offer?>(null) }
    val wantedName = state.wanted?.name ?: "this card"

    LaunchedEffect(route) { vm.start(route) }
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is ReviewOffersEvent.Accepted -> {
                    snackbar.showSnackbar("Accepted. You give ${event.card}. Open the chat to finish the trade.")
                    onAccepted(event.partnerEmail)
                }
                is ReviewOffersEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to my wants", tint = TcgColors.Ink)
            }
            Text("Back to my wants", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Ink)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CardArt(state.wanted?.illustrationUrl, wantedName, Modifier.width(76.dp), corner = 8.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Offers for your $wantedName", style = MaterialTheme.typography.headlineMedium, color = TcgColors.Ink)
                        Text(
                            "Each player can give you this card and named what they would like back. Pick the card you will give, then accept.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TcgColors.Muted,
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val count = state.offers.size
                    Text(
                        if (state.loading) "Loading offers" else if (count == 1) "1 offer" else "$count offers",
                        style = MaterialTheme.typography.titleSmall,
                        color = TcgColors.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    TcgChip("Newest", state.sortBy == "newest", onClick = { vm.setSort("newest") }, label = "Sort by newest")
                    TcgChip("Oldest", state.sortBy == "oldest", onClick = { vm.setSort("oldest") }, label = "Sort by oldest")
                }
            }

            when {
                state.loading -> items(2) {
                    Panel { Skeleton(Modifier.fillMaxWidth().heightIn(min = 160.dp)) }
                }
                state.error != null -> item { ErrorState(state.error ?: "", onRetry = vm::load) }
                state.offers.isEmpty() -> item {
                    EmptyState(
                        title = "No offers yet",
                        message = "When players who have $wantedName make an offer, it shows up here. You'll get an email too.",
                    )
                }
                else -> items(state.offers, key = { it.id }) { offer ->
                    OfferCard(
                        offer = offer,
                        picked = vm.pickedRef(offer, state),
                        busy = state.busy,
                        onPick = { ref -> vm.togglePick(offer, ref) },
                        onAccept = { acceptTarget = offer },
                        onDecline = { declineTarget = offer },
                    )
                }
            }
        }
    }

    acceptTarget?.let { offer ->
        val ref = vm.pickedRef(offer, state)
        if (ref != null) {
            ConfirmDialog(
                title = "Accept ${playerLabel(offer.bidderFriendId)}'s offer?",
                message = "The other offers on this want will be declined, and a chat opens so you can finish the trade in the game.",
                confirmText = "Accept and give ${ref.name}",
                swap = ref.name to wantedName,
                onConfirm = { acceptTarget = null; vm.accept(offer) },
                onDismiss = { acceptTarget = null },
            )
        } else {
            acceptTarget = null
        }
    }

    declineTarget?.let { offer ->
        ConfirmDialog(
            title = "Decline ${playerLabel(offer.bidderFriendId)}'s offer?",
            message = "It is removed from this want. The player can send a new offer later.",
            confirmText = "Decline offer",
            dismissText = "Keep it",
            danger = true,
            onConfirm = { declineTarget = null; vm.decline(offer) },
            onDismiss = { declineTarget = null },
        )
    }
}

@Composable
private fun OfferCard(
    offer: Offer,
    picked: CardRef?,
    busy: Boolean,
    onPick: (CardRef) -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val player = playerLabel(offer.bidderFriendId)
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(player, style = MaterialTheme.typography.titleLarge, color = TcgColors.Ink)
                if (isWithinHours(offer.creationDate, 24)) Tag("New")
                Text(
                    timeAgo(offer.creationDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = TcgColors.Muted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }

            Text("Would like one of:", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                offer.refs().forEach { ref ->
                    val isPicked = picked?.key() == ref.key()
                    Column(
                        Modifier
                            .width(92.dp)
                            .clickable(role = Role.Button, onClick = { onPick(ref) })
                            .semantics {
                                selected = isPicked
                                contentDescription = ref.name + if (isPicked) ", picked" else ""
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CardArt(ref.imageUrl, ref.name, Modifier.fillMaxWidth(), corner = 6.dp, selected = isPicked)
                        Text(
                            ref.name,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            color = TcgColors.Ink,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            TcgButton(
                text = if (picked != null) "Accept and give ${picked.name}" else "Pick the card you will give",
                onClick = onAccept,
                enabled = picked != null && !busy,
                modifier = Modifier.fillMaxWidth(),
            )
            TcgButton("Decline", onDecline, Modifier.fillMaxWidth(), style = TcgButtonStyle.Secondary, enabled = !busy)
        }
    }
}
