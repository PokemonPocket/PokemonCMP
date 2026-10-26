package com.tcgptrading.tcgptrading.features.collection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.tcgptrading.tcgptrading.core.data.model.Card
import com.tcgptrading.tcgptrading.core.data.model.SetProgress
import com.tcgptrading.tcgptrading.core.presentation.components.CARD_RATIO
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.ConfirmDialog
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.RarityText
import com.tcgptrading.tcgptrading.core.presentation.components.SearchField
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.components.TcgChip
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.LANGUAGES
import com.tcgptrading.tcgptrading.core.util.directImageUrl
import com.tcgptrading.tcgptrading.core.util.expansionName
import com.tcgptrading.tcgptrading.core.util.rarityOf
import com.tcgptrading.tcgptrading.main.presentation.LocalFriendIdGate
import com.tcgptrading.tcgptrading.main.presentation.LocalSnackbar
import org.koin.compose.viewmodel.koinViewModel

private val RARITY_CHIPS = listOf("C", "U", "R", "RR", "AR", "SR", "IM", "UR")

private fun numberLabel(card: Card): String = card.collectionNumber?.let { "#" + it.toString().padStart(3, '0') } ?: ""

/**
 * Collection: tap a card to mark it owned, hold it for spares and wants.
 * onOpenWanted opens the Wanted tab searching for a card name (to offer a spare).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(onOpenWanted: (search: String) -> Unit, onSeeMyWants: () -> Unit) {
    val vm = koinViewModel<CollectionViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val haptics = LocalHapticFeedback.current

    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var sheetCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSets by remember { mutableStateOf(false) }
    var showMatches by remember { mutableStateOf(false) }
    var confirmMarkAll by remember { mutableStateOf(false) }

    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val shown = state.shown
    val missingShown = shown.count { (state.counts[it.cardId] ?: 0) == 0 }

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 78.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(
                    state = state,
                    searchOpen = searchOpen,
                    onToggleSearch = {
                        searchOpen = !searchOpen
                        if (!searchOpen) vm.setQuery("")
                    },
                    onOpenSets = { showSets = true },
                    onQuery = vm::setQuery,
                    onFilter = vm::setFilter,
                    onRarity = vm::setRarity,
                    onOpenMatches = { showMatches = true },
                )
            }

            when {
                state.loading -> items(16) {
                    Skeleton(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO), corner = 8.dp)
                }

                state.error != null && state.cards.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorState(state.error!!, onRetry = vm::retry)
                }

                shown.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "No cards match",
                        message = when (state.filter) {
                            OwnFilter.Spares -> "Hold a card and add copies to mark spares you could trade."
                            OwnFilter.Owned -> "Tap cards you own to add them to your collection."
                            else -> "Try another search or filter."
                        },
                        actionText = "Show all cards",
                        onAction = {
                            vm.setFilter(OwnFilter.All); vm.setRarity(null); vm.setQuery("")
                        },
                    )
                }

                else -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Tap a card to mark it owned. Hold it for spares and wants.",
                                style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted,
                                modifier = Modifier.weight(1f),
                            )
                            if (missingShown > 0) {
                                TextButton(
                                    onClick = { if (missingShown > 20) confirmMarkAll = true else vm.markShownAsOwned() },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text("Mark all owned", style = MaterialTheme.typography.labelMedium, color = TcgColors.Ink)
                                }
                            }
                        }
                    }
                    items(shown, key = { it.cardId }) { card ->
                        val count = state.counts[card.cardId] ?: 0
                        CollectionTile(
                            card = card,
                            count = count,
                            onTap = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.toggle(card.cardId)
                            },
                            onHold = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                sheetCardId = card.cardId
                            },
                        )
                    }
                    if (state.loadingMore) {
                        items(4) { Skeleton(Modifier.fillMaxWidth().aspectRatio(CARD_RATIO), corner = 8.dp) }
                    }
                }
            }
        }
    }

    // ---------- Sheets and dialogs ----------

    val sheetCard = state.cards.firstOrNull { it.cardId == sheetCardId }
    if (sheetCard != null) {
        CardSheet(
            card = sheetCard,
            count = state.counts[sheetCard.cardId] ?: 0,
            wantedBy = state.wantedBy(sheetCard.cardId),
            inWants = sheetCard.cardId in state.wantedIds,
            onCount = { vm.setCount(sheetCard.cardId, it) },
            onOffer = {
                sheetCardId = null
                onOpenWanted(sheetCard.name)
            },
            onAddWant = vm::addWant,
            onSeeMyWants = {
                sheetCardId = null
                onSeeMyWants()
            },
            onDismiss = { sheetCardId = null },
        )
    }

    if (showSets) {
        SetPickerSheet(
            sets = state.sets,
            selected = state.setCode,
            onPick = {
                vm.selectSet(it)
                showSets = false
            },
            onDismiss = { showSets = false },
        )
    }

    if (showMatches) {
        MatchesSheet(
            state = state,
            onOffer = { name ->
                showMatches = false
                onOpenWanted(name)
            },
            onDismiss = { showMatches = false },
        )
    }

    if (confirmMarkAll) {
        ConfirmDialog(
            title = "Mark $missingShown cards as owned?",
            message = "Every missing card shown with the current filters is added to your collection. You can untap any of them later.",
            confirmText = "Mark $missingShown as owned",
            onConfirm = {
                confirmMarkAll = false
                vm.markShownAsOwned()
            },
            onDismiss = { confirmMarkAll = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Header: title, set picker, matches strip, filters
// ---------------------------------------------------------------------------

@Composable
private fun Header(
    state: CollectionState,
    searchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSets: () -> Unit,
    onQuery: (String) -> Unit,
    onFilter: (OwnFilter) -> Unit,
    onRarity: (String?) -> Unit,
    onOpenMatches: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Collection", style = MaterialTheme.typography.headlineMedium, color = TcgColors.Ink, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleSearch, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (searchOpen) "Close search" else "Search this set",
                    tint = TcgColors.Ink,
                )
            }
        }

        if (searchOpen) {
            SearchField(value = state.query, onValueChange = onQuery, placeholder = "Name or number")
        }

        SetButton(state, onOpenSets)

        if (state.matches.totalSparesWanted > 0) {
            val n = state.matches.totalSparesWanted
            Surface(
                onClick = onOpenMatches,
                shape = RoundedCornerShape(14.dp),
                color = TcgColors.Surface,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .border(2.dp, TcgColors.Red, RoundedCornerShape(14.dp)),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (n == 1L) "1 spare matches wanted cards" else "$n spares match wanted cards",
                        style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink, modifier = Modifier.weight(1f),
                    )
                    Text("See", style = MaterialTheme.typography.labelLarge, color = TcgColors.RedInk)
                }
            }
        }

        // One scrolling row: what to show, then rarity
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(OwnFilter.entries) { f ->
                TcgChip(f.name, selected = state.filter == f, onClick = { onFilter(f) })
            }
            item { Spacer(Modifier.width(4.dp)) }
            items(RARITY_CHIPS) { code ->
                val r = rarityOf(code)
                TcgChip(
                    text = r?.glyphs ?: code,
                    label = r?.label ?: code,
                    selected = state.rarity == code,
                    onClick = { onRarity(if (state.rarity == code) null else code) },
                )
            }
        }
    }
}

@Composable
private fun SetButton(state: CollectionState, onClick: () -> Unit) {
    val set = state.selectedSet
    // While the set is fully loaded, count live from the grid; otherwise use the server's number
    val owned = if (state.cards.isNotEmpty() && !state.loadingMore) state.ownedInSet.toLong() else set?.owned ?: 0
    val total = set?.total ?: 0
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = TcgColors.Surface,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .semantics { contentDescription = "Set ${expansionName(state.setCode)}, $owned of $total cards owned. Change set" },
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row {
                    Text(expansionName(state.setCode).ifBlank { "Choose a set" }, style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink, modifier = Modifier.weight(1f))
                    if (total > 0) Text("$owned of $total", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                }
                LinearProgressIndicator(
                    progress = { if (total > 0) (owned.toFloat() / total).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = TcgColors.Red,
                    trackColor = TcgColors.LineSoft,
                    drawStopIndicator = {},
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TcgColors.Ink)
        }
    }
}

// ---------------------------------------------------------------------------
// Tile
// ---------------------------------------------------------------------------

private val Greyscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionTile(card: Card, count: Int, onTap: () -> Unit, onHold: () -> Unit) {
    val owned = count > 0
    val shape = RoundedCornerShape(8.dp)
    val number = numberLabel(card)
    Column(
        Modifier
            .clip(shape)
            .combinedClickable(onClick = onTap, onLongClick = onHold)
            .semantics {
                role = Role.Checkbox
                contentDescription = "${card.name} $number"
                stateDescription = when {
                    count > 1 -> "Owned, ${count - 1} spare" + if (count > 2) "s" else ""
                    owned -> "Owned"
                    else -> "Missing"
                }
                onLongClick(label = "Spares and wants") { onHold(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .then(
                    if (owned) Modifier.clip(shape).background(TcgColors.Surface)
                    else Modifier.drawBehind {
                        drawRoundRect(
                            color = TcgColors.Line,
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                        )
                    }
                )
                .padding(3.dp),
        ) {
            TileImage(card, owned)
            if (owned) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp).size(18.dp)
                        .clip(CircleShape).background(TcgColors.Ground).padding(2.dp)
                        .clip(CircleShape).background(TcgColors.Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (count > 1) {
                Text(
                    "+${count - 1}",
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                        .clip(CircleShape).background(TcgColors.Red).padding(horizontal = 5.dp, vertical = 1.dp),
                    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(number, style = MaterialTheme.typography.labelSmall, color = TcgColors.Muted, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun TileImage(card: Card, owned: Boolean) {
    val url = directImageUrl(card.illustrationUrl)
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxWidth().aspectRatio(CARD_RATIO).clip(RoundedCornerShape(5.dp)).background(TcgColors.Soft).padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(card.name, fontSize = 10.sp, textAlign = TextAlign.Center, color = TcgColors.Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
    val imageModifier = Modifier.fillMaxWidth().aspectRatio(CARD_RATIO).clip(RoundedCornerShape(5.dp))
        .then(if (owned) Modifier else Modifier.alpha(0.42f))
    if (url == null) {
        Box(if (owned) Modifier else Modifier.alpha(0.42f)) { fallback() }
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (owned) null else Greyscale,
            modifier = imageModifier,
            loading = { Skeleton(Modifier.fillMaxSize(), corner = 5.dp) },
            error = { fallback() },
        )
    }
}

// ---------------------------------------------------------------------------
// Card sheet: copies, spares, wants
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CardSheet(
    card: Card,
    count: Int,
    wantedBy: Long,
    inWants: Boolean,
    onCount: (Int) -> Unit,
    onOffer: () -> Unit,
    onAddWant: (Card, String, String, (Boolean) -> Unit) -> Unit,
    onSeeMyWants: () -> Unit,
    onDismiss: () -> Unit,
) {
    val gate = LocalFriendIdGate.current
    var language by rememberSaveable(card.cardId) { mutableStateOf("English") }
    var note by rememberSaveable(card.cardId) { mutableStateOf("") }
    var adding by remember(card.cardId) { mutableStateOf(false) }
    var added by remember(card.cardId) { mutableStateOf(false) }
    val spares = (count - 1).coerceAtLeast(0)
    val tradable = card.tradable != false

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = TcgColors.Surface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CardArt(card.illustrationUrl, card.name, Modifier.width(74.dp), corner = 6.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(card.name, style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            listOf(numberLabel(card), expansionName(card.expansion)).filter { it.isNotBlank() }.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
                        )
                        RarityText(card.rarity ?: card.rarityName)
                    }
                }
            }

            // Copies stepper
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TcgColors.Soft).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Copies you have", style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
                    Text(
                        when {
                            count == 0 -> "You don't have this card yet"
                            spares == 0 -> "1 for you, no spares"
                            spares == 1 -> "1 for you, 1 spare to trade"
                            else -> "1 for you, $spares spares to trade"
                        },
                        style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted,
                    )
                }
                StepButton("−", "One less ${card.name}", enabled = count > 0) { onCount(count - 1) }
                Text(
                    count.toString(),
                    modifier = Modifier.width(36.dp).semantics { contentDescription = "$count copies" },
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink,
                )
                StepButton("+", "One more ${card.name}", enabled = count < 99) { onCount(count + 1) }
            }

            // What a spare can do
            if (spares > 0 && wantedBy > 0) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, TcgColors.Red, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (wantedBy == 1L) "1 player wants ${card.name}" else "$wantedBy players want ${card.name}",
                            style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink,
                        )
                        Text("Offer your spare and pick what you would like back.", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                    }
                    TcgButton("Offer", onOffer)
                }
            }

            // Missing card: add it to my wants
            if (count == 0) {
                when {
                    added -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Added to your wants. Players who have ${card.name} can now send you offers.",
                            style = MaterialTheme.typography.bodyLarge, color = TcgColors.Ink,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TcgColors.Ground).padding(14.dp),
                        )
                        TcgButton("See my wants", onSeeMyWants, Modifier.fillMaxWidth(), style = TcgButtonStyle.Secondary)
                    }

                    inWants -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("This card is in your wants.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted, modifier = Modifier.weight(1f))
                        TextButton(onClick = onSeeMyWants, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("See my wants", color = TcgColors.Ink)
                        }
                    }

                    !tradable -> Text(
                        "This card can't be traded in Pokémon TCG Pocket, so it can't be added to your wants.",
                        style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
                    )

                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HorizontalDivider(color = TcgColors.LineSoft)
                        Text("Add to my wants", style = MaterialTheme.typography.titleMedium, color = TcgColors.Ink)
                        Text("Which language do you need?", style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LANGUAGES.forEach { lang -> TcgChip(lang, selected = language == lang, onClick = { language = lang }) }
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { if (it.length <= 200) note = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Note for traders (optional)") },
                            placeholder = { Text("For example: I can give Moltres ex or Articuno ex", color = TcgColors.Faint) },
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TcgColors.Ink, unfocusedBorderColor = TcgColors.Line, cursorColor = TcgColors.Ink),
                        )
                        TcgButton(
                            "Add to my wants",
                            onClick = {
                                gate.require {
                                    adding = true
                                    onAddWant(card, language, note) { ok ->
                                        adding = false
                                        added = ok
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            loading = adding,
                            large = true,
                        )
                    }
                }
            }

            TcgButton("Done", onDismiss, Modifier.fillMaxWidth(), style = TcgButtonStyle.Dark, large = true)
        }
    }
}

@Composable
private fun StepButton(text: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = TcgColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) TcgColors.Line else TcgColors.LineSoft),
        modifier = Modifier.size(44.dp).semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 20.sp, color = if (enabled) TcgColors.Ink else TcgColors.Faint)
        }
    }
}

// ---------------------------------------------------------------------------
// Set picker and matches
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetPickerSheet(sets: List<SetProgress>, selected: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = TcgColors.Surface) {
        Text("Choose a set", style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            items(sets, key = { it.code }) { set ->
                val on = set.code == selected
                Surface(
                    onClick = { onPick(set.code) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (on) TcgColors.Ground else TcgColors.Surface,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 2.dp)
                        .semantics { contentDescription = "${expansionName(set.code)}, ${set.owned} of ${set.total} owned" + if (on) ", selected" else "" },
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Text(expansionName(set.code), style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink, modifier = Modifier.weight(1f))
                            Text("${set.owned} of ${set.total}", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                        }
                        LinearProgressIndicator(
                            progress = { if (set.total > 0) (set.owned.toFloat() / set.total).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = if (on) TcgColors.Red else TcgColors.Diamond,
                            trackColor = TcgColors.LineSoft,
                            drawStopIndicator = {},
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchesSheet(state: CollectionState, onOffer: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = TcgColors.Surface) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Spares other players want", style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink)
            Spacer(Modifier.height(4.dp))
            Text("Offer one and pick what you would like back.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.matches.cards, key = { it.cardId }) { m ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TcgColors.Soft).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CardArt(m.imageUrl, m.name, Modifier.width(44.dp), sleeve = false, corner = 5.dp)
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = MaterialTheme.typography.titleMedium, color = TcgColors.Ink)
                        Text(
                            (if (m.wantedBy == 1L) "1 player wants it" else "${m.wantedBy} players want it") +
                                ", you have ${m.spares} spare" + if (m.spares == 1) "" else "s",
                            style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted,
                        )
                    }
                    TcgButton("Offer", { onOffer(m.name) })
                }
            }
        }
    }
}
