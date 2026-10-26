package com.tcgptrading.tcgptrading.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcgptrading.tcgptrading.app.domain.Detail
import com.tcgptrading.tcgptrading.core.data.model.ChatContact
import com.tcgptrading.tcgptrading.core.data.model.TradeSummary
import com.tcgptrading.tcgptrading.core.presentation.components.Avatar
import com.tcgptrading.tcgptrading.core.presentation.components.CardArt
import com.tcgptrading.tcgptrading.core.presentation.components.ConfirmDialog
import com.tcgptrading.tcgptrading.core.presentation.components.CountBadge
import com.tcgptrading.tcgptrading.core.presentation.components.EmptyState
import com.tcgptrading.tcgptrading.core.presentation.components.ErrorState
import com.tcgptrading.tcgptrading.core.presentation.components.ScreenHeader
import com.tcgptrading.tcgptrading.core.presentation.components.SearchField
import com.tcgptrading.tcgptrading.core.presentation.components.Skeleton
import com.tcgptrading.tcgptrading.core.presentation.components.Tag
import com.tcgptrading.tcgptrading.core.presentation.components.TagTone
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.clockTime
import com.tcgptrading.tcgptrading.core.util.dayLabel
import com.tcgptrading.tcgptrading.core.util.groupFriendId
import com.tcgptrading.tcgptrading.core.util.nameFromEmail
import com.tcgptrading.tcgptrading.core.util.timeAgo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val chatModule = module {
    viewModelOf(::ChatListViewModel)
    viewModel { (email: String) -> ConversationViewModel(email, get(), get(), get()) }
}

// ---------------------------------------------------------------------------
// Chats
// ---------------------------------------------------------------------------

@Composable
fun ChatListScreen(onOpen: (email: String) -> Unit, onBrowseWanted: () -> Unit) {
    val vm = koinViewModel<ChatListViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        vm.load(silent = true)
        onPauseOrDispose { }
    }

    val visible = remember(state.chats, state.query) {
        val q = state.query.trim().lowercase()
        if (q.isEmpty()) state.chats
        else state.chats.filter {
            nameFromEmail(it.user?.email).lowercase().contains(q) || it.lastMessage.orEmpty().lowercase().contains(q)
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
        item { ScreenHeader("Chats") }
        if (state.chats.isNotEmpty()) {
            item {
                SearchField(
                    value = state.query, onValueChange = vm::setQuery, placeholder = "Search chats",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        when {
            state.loading -> items(5) { ChatRowSkeleton() }
            state.error != null -> item { ErrorState(state.error!!, onRetry = { vm.load() }) }
            state.chats.isEmpty() -> item {
                EmptyState(
                    title = "No chats yet",
                    message = "Chats open when an offer is accepted.",
                    actionText = "See wanted cards",
                    onAction = onBrowseWanted,
                )
            }
            visible.isEmpty() -> item {
                Text(
                    "No chats match \"${state.query}\".",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
                )
            }
            else -> items(visible, key = { it.user?.email ?: it.hashCode().toString() }) { chat ->
                ChatRow(chat, onClick = { chat.user?.email?.let(onOpen) })
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatContact, onClick: () -> Unit) {
    val name = nameFromEmail(chat.user?.email).ifBlank { "Player" }
    val unread = chat.unreadCount > 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TcgColors.Surface)
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(name)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(timeAgo(chat.lastMessageTime), style = MaterialTheme.typography.labelSmall, color = TcgColors.Muted)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.lastMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unread) TcgColors.Ink else TcgColors.Muted,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    CountBadge(chat.unreadCount, Modifier.semantics { contentDescription = "${chat.unreadCount} unread" })
                }
            }
        }
    }
}

@Composable
private fun ChatRowSkeleton() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp))
            .background(TcgColors.Surface).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Skeleton(Modifier.size(44.dp), corner = 22.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Skeleton(Modifier.width(120.dp).height(14.dp))
            Skeleton(Modifier.width(200.dp).height(12.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Conversation
// ---------------------------------------------------------------------------

private sealed interface ChatItem {
    val key: String
    data class Day(val label: String) : ChatItem { override val key = "day-$label" }
    data class Msg(val message: UiMessage, val seen: Boolean) : ChatItem { override val key = message.key }
}

@Composable
fun ConversationScreen(route: Detail.Conversation, onBack: () -> Unit) {
    val vm = koinViewModel<ConversationViewModel>(key = "conversation-${route.email}", parameters = { parametersOf(route.email) })
    val state by vm.state.collectAsStateWithLifecycle()
    val connected by vm.connected.collectAsStateWithLifecycle()
    val name = nameFromEmail(route.email).ifBlank { "Player" }
    var confirmTrade by remember { mutableStateOf(false) }

    // Chronological items with day separators; displayed reversed so the list starts at the bottom
    val items = remember(state.messages) {
        val lastSeen = state.messages.indexOfLast { it.mine && it.read }
        buildList {
            var lastDay = ""
            state.messages.forEachIndexed { i, m ->
                val day = dayLabel(m.time)
                if (day.isNotEmpty() && day != lastDay) {
                    add(ChatItem.Day(day)); lastDay = day
                }
                add(ChatItem.Msg(m, seen = i == lastSeen))
            }
        }.asReversed()
    }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Column(Modifier.fillMaxSize().background(TcgColors.Surface).imePadding()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chats", tint = TcgColors.Ink)
            }
            Avatar(name, online = state.online, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = TcgColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val status = when {
                    state.online -> "Online"
                    state.lastActive != null -> "Last seen ${timeAgo(state.lastActive).lowercase()}"
                    else -> null
                }
                if (status != null) {
                    Text(
                        status, style = MaterialTheme.typography.labelMedium,
                        color = if (state.online) TcgColors.OkText else TcgColors.Muted,
                    )
                }
            }
            val trade = state.trade
            if (trade != null && trade.completedAt == null) {
                TcgButton(
                    "Mark as traded", onClick = { confirmTrade = true },
                    loading = state.completing, modifier = Modifier.padding(end = 8.dp),
                )
            } else if (trade?.completedAt != null) {
                Tag("Traded", Modifier.padding(end = 12.dp), tone = TagTone.Ink)
            }
        }
        HorizontalDivider(color = TcgColors.LineSoft)

        state.trade?.let { TradePanel(it, state.moreTrades, name) }

        // Messages
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Skeleton(Modifier.width(180.dp).height(40.dp), corner = 16.dp)
                    Skeleton(Modifier.width(140.dp).height(40.dp).align(Alignment.End), corner = 16.dp)
                    Skeleton(Modifier.width(220.dp).height(40.dp), corner = 16.dp)
                }
                state.error != null -> ErrorState(state.error!!, onRetry = vm::load)
                state.messages.isEmpty() -> Text(
                    "No messages yet. Say hi and agree how you'll trade.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
                )
                else -> LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items, key = { it.key }) { item ->
                        when (item) {
                            is ChatItem.Day -> Text(
                                item.label, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium, color = TcgColors.Muted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            is ChatItem.Msg -> Bubble(item.message, item.seen, name)
                        }
                    }
                    // reverseLayout: the last item shows at the top
                    item { Text("Messages are deleted after 14 days.", modifier = Modifier.fillMaxWidth().padding(top = 4.dp), style = MaterialTheme.typography.labelSmall, color = TcgColors.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                }
            }
        }

        if (state.sendError != null) {
            Text(
                state.sendError!!, modifier = Modifier.fillMaxWidth().background(TcgColors.RedSoft).padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = TcgColors.RedInk,
            )
        } else if (!connected) {
            Text(
                "Reconnecting…", modifier = Modifier.fillMaxWidth().background(TcgColors.Soft).padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted,
            )
        }

        Composer(name = name, onSend = vm::send, onTyping = vm::clearSendError)
    }

    val trade = state.trade
    if (confirmTrade && trade != null) {
        ConfirmDialog(
            title = "Mark this trade as done?",
            message = "Do this once the cards were swapped in Pokémon TCG Pocket. It moves the trade to Done for both of you.",
            confirmText = "Mark as traded",
            swap = if (trade.youGive != null && trade.youGet != null) trade.youGive to trade.youGet else null,
            onConfirm = { confirmTrade = false; vm.completeTrade() },
            onDismiss = { confirmTrade = false },
        )
    }
}

@Composable
private fun Bubble(m: UiMessage, seen: Boolean, peerName: String) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start,
    ) {
        val shape = if (m.mine) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        Text(
            m.text,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (m.mine) TcgColors.Ink else TcgColors.Ground)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics { contentDescription = (if (m.mine) "You: " else "$peerName: ") + m.text },
            color = if (m.mine) androidx.compose.ui.graphics.Color.White else TcgColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (m.pending) "Sending…" else clockTime(m.time), style = MaterialTheme.typography.labelSmall, color = TcgColors.Muted)
            if (seen) Text("Seen", style = MaterialTheme.typography.labelSmall, color = TcgColors.Muted)
        }
    }
}

@Composable
private fun Composer(name: String, onSend: (String) -> Boolean, onTyping: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val send = { if (onSend(text)) text = "" }
    HorizontalDivider(color = TcgColors.LineSoft)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; onTyping() },
            modifier = Modifier.weight(1f).semantics { contentDescription = "Message to $name" },
            placeholder = { Text("Write a message", color = TcgColors.Faint) },
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TcgColors.Surface, unfocusedContainerColor = TcgColors.Surface,
                focusedBorderColor = TcgColors.Ink, unfocusedBorderColor = TcgColors.Line, cursorColor = TcgColors.Ink,
            ),
        )
        IconButton(
            onClick = { send() },
            enabled = text.isNotBlank(),
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                .background(if (text.isNotBlank()) TcgColors.Ink else TcgColors.LineSoft),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send, contentDescription = "Send",
                tint = if (text.isNotBlank()) androidx.compose.ui.graphics.Color.White else TcgColors.Faint,
            )
        }
    }
}

/** The accepted trade with this player: what moves each way and how to finish it. */
@Composable
private fun TradePanel(trade: TradeSummary, moreTrades: Int, peerName: String) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    val give = trade.youGive?.let { "You give $it" } ?: "You give the card they picked"
    val get = trade.youGet?.let { "you get $it" }
        ?: trade.youGetOptions?.takeIf { it.isNotEmpty() }?.let { "you get one of: ${it.joinToString(", ")}" }
    val line = if (get != null) "$give, $get" else give

    Column(
        Modifier.fillMaxWidth().background(TcgColors.Soft).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CardArt(trade.youGiveImage, trade.youGive ?: "Your card", Modifier.width(40.dp), sleeve = false, corner = 5.dp)
                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = TcgColors.Diamond, modifier = Modifier.size(20.dp))
                CardArt(trade.youGetImage, trade.youGet ?: "Their card", Modifier.width(40.dp), sleeve = false, corner = 5.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(line, style = MaterialTheme.typography.titleMedium, color = TcgColors.Ink)
                val meta = buildString {
                    if (trade.completedAt != null) append("Traded ${timeAgo(trade.completedAt).lowercase()}.")
                    else trade.expiresInDays?.let { d ->
                        append(if (d <= 0) "Expires today." else if (d == 1) "Expires in 1 day." else "Expires in $d days.")
                    }
                    if (moreTrades > 0) append(" $moreTrades more ${if (moreTrades == 1) "trade" else "trades"} with $peerName in My trades.")
                }.trim()
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
            }
        }

        if (trade.completedAt == null) {
            Step(1) {
                Column {
                    Text("Add each other in Pocket.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Ink)
                    val id = groupFriendId(trade.partnerFriendId)
                    if (id.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Their ID ", style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted)
                            Text(id, style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
                            TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(trade.partnerFriendId.orEmpty().filter { it.isDigit() }))
                                    copied = true
                                    scope.launch { delay(2000); copied = false }
                                },
                                modifier = Modifier.heightIn(min = 48.dp).semantics {
                                    contentDescription = if (copied) "Friend ID copied" else "Copy their Friend ID"
                                },
                            ) { Text(if (copied) "Copied" else "Copy", color = TcgColors.Ink) }
                        }
                    }
                }
            }
            Step(2) { Text("Make the trade in the game.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Ink) }
            Step(3) { Text("Mark it as traded here.", style = MaterialTheme.typography.bodyMedium, color = TcgColors.Ink) }
        }
    }
    HorizontalDivider(color = TcgColors.LineSoft)
}

@Composable
private fun Step(number: Int, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(TcgColors.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
        }
        content()
    }
}
