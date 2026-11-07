package com.tcgptrading.tcgptrading.core.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import com.tcgptrading.tcgptrading.core.util.directImageUrl
import com.tcgptrading.tcgptrading.core.util.rarityOf

/** Pokémon TCG Pocket card proportions (width / height). */
const val CARD_RATIO = 367f / 512f

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

enum class TcgButtonStyle { Primary, Outline, Secondary, Dark, Danger, Ghost }

/** The app's button. Primary (red) is for the one main action of a screen. */
@Composable
fun TcgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TcgButtonStyle = TcgButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    large: Boolean = false,
    /** Shown instead of [text] while [loading], e.g. "Sending…". */
    loadingText: String = "Please wait…",
) {
    val height = if (large) 52.dp else 46.dp
    val shape = RoundedCornerShape(if (large) 14.dp else 12.dp)
    val content: @Composable () -> Unit = {
        Text(text = if (loading) loadingText else text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val mod = modifier.heightIn(min = height)
    when (style) {
        TcgButtonStyle.Primary -> Button(
            onClick, mod, enabled && !loading, shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = TcgColors.Red, contentColor = Color.White, disabledContainerColor = TcgColors.LineSoft, disabledContentColor = TcgColors.Faint),
        ) { content() }
        TcgButtonStyle.Dark -> Button(
            onClick, mod, enabled && !loading, shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = TcgColors.Ink, contentColor = Color.White, disabledContainerColor = TcgColors.LineSoft, disabledContentColor = TcgColors.Faint),
        ) { content() }
        TcgButtonStyle.Outline, TcgButtonStyle.Danger -> OutlinedButton(
            onClick, mod, enabled && !loading, shape = shape,
            border = BorderStroke(2.dp, if (enabled) TcgColors.Red else TcgColors.LineSoft),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TcgColors.RedInk, disabledContentColor = TcgColors.Faint),
        ) { content() }
        TcgButtonStyle.Secondary -> OutlinedButton(
            onClick, mod, enabled && !loading, shape = shape,
            border = BorderStroke(1.dp, TcgColors.Line),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = TcgColors.Surface, contentColor = TcgColors.Ink, disabledContentColor = TcgColors.Faint),
        ) { content() }
        TcgButtonStyle.Ghost -> TextButton(onClick, mod, enabled && !loading, shape = shape,
            colors = ButtonDefaults.textButtonColors(contentColor = TcgColors.Ink)) { content() }
    }
}

// ---------------------------------------------------------------------------
// Card art
// ---------------------------------------------------------------------------

/**
 * Card art in a white "sleeve". Converts Drive links, and shows the card name when the image
 * is missing or fails, so a card is always identifiable.
 */
@Composable
fun CardArt(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    sleeve: Boolean = true,
    corner: Dp = 10.dp,
    selected: Boolean = false,
) {
    val outer = RoundedCornerShape(corner + if (sleeve) 4.dp else 0.dp)
    Box(
        modifier
            .then(
                if (sleeve) Modifier
                    .clip(outer)
                    .background(if (selected) TcgColors.RedSoft else TcgColors.Surface)
                    .then(if (selected) Modifier.border(2.dp, TcgColors.Red, outer) else Modifier)
                    .padding(6.dp)
                else Modifier
            )
    ) {
        val url = directImageUrl(imageUrl)
        val fallback: @Composable () -> Unit = {
            Box(
                Modifier.fillMaxWidth().aspectRatio(CARD_RATIO).clip(RoundedCornerShape(corner)).background(TcgColors.Soft).padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp), textAlign = TextAlign.Center, color = TcgColors.Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        if (url == null) {
            fallback()
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = "$name card art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(CARD_RATIO).clip(RoundedCornerShape(corner)),
                loading = { Skeleton(Modifier.fillMaxSize()) },
                error = { fallback() },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Small pieces
// ---------------------------------------------------------------------------

/** Rarity glyphs (◆◆◆, ★, ♛) with an accessible label. */
@Composable
fun RarityText(rarity: String?, modifier: Modifier = Modifier, size: Int = 13) {
    val r = rarityOf(rarity) ?: return
    Text(
        r.glyphs,
        modifier = modifier.semantics { contentDescription = r.label },
        color = r.color,
        fontSize = size.sp,
        letterSpacing = 1.sp,
        maxLines = 1,
    )
}

/** Filter / toggle chip. Selected = navy. */
@Composable
fun TcgChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, label: String = text) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp).semantics { contentDescription = label + if (selected) ", selected" else "" },
        shape = CircleShape,
        color = if (selected) TcgColors.Ink else TcgColors.Surface,
        contentColor = if (selected) Color.White else TcgColors.Ink,
        border = if (selected) null else BorderStroke(1.dp, TcgColors.Line),
    ) {
        Box(Modifier.padding(horizontal = 16.dp).heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyMedium, letterSpacing = if (text.any { it == '◆' || it == '★' }) 1.sp else 0.sp)
        }
    }
}

enum class TagTone { Red, Ink, Soft }

@Composable
fun Tag(text: String, modifier: Modifier = Modifier, tone: TagTone = TagTone.Red) {
    val (bg, fg) = when (tone) {
        TagTone.Red -> TcgColors.RedTag to TcgColors.RedInk
        TagTone.Ink -> TcgColors.Ink to Color.White
        TagTone.Soft -> TcgColors.Chip to TcgColors.Ink
    }
    Text(
        text, modifier.clip(CircleShape).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
        color = fg, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Red count badge (unread chats, new offers). */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp).clip(CircleShape).background(TcgColors.Red).padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 99) "99+" else count.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Initials circle with an optional online dot. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, online: Boolean = false, size: Dp = 44.dp) {
    Box(modifier.size(size)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(TcgColors.Chip), contentAlignment = Alignment.Center) {
            Text(name.take(2).uppercase(), style = MaterialTheme.typography.titleSmall, color = TcgColors.Ink)
        }
        if (online) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(TcgColors.Surface).padding(2.dp)
                    .clip(CircleShape).background(TcgColors.Online)
            )
        }
    }
}

/** Page title with an optional one-line explanation. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = TcgColors.Ink)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted)
        }
    }
}

/** Search field with icon and clear button. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TcgColors.Faint) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TcgColors.Muted) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TcgColors.Muted) }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = TcgColors.Surface, unfocusedContainerColor = TcgColors.Surface,
            focusedBorderColor = TcgColors.Ink, unfocusedBorderColor = TcgColors.Line,
            cursorColor = TcgColors.Ink,
        ),
    )
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

/** Shimmering placeholder while content loads. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, corner: Dp = 10.dp) {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "skeletonAlpha",
    )
    Box(modifier.alpha(alpha).clip(RoundedCornerShape(corner)).background(TcgColors.LineSoft))
}

/** Empty screen: says what is missing and offers the next step. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(20.dp)).background(TcgColors.Surface).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(6.dp))
            TcgButton(actionText, onAction, style = TcgButtonStyle.Dark)
        }
    }
}

/** Error with a retry button. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) =
    EmptyState("Something went wrong", message, modifier, actionText = "Try again", onAction = onRetry)

// ---------------------------------------------------------------------------
// Trade progress
// ---------------------------------------------------------------------------

/**
 * Offer sent → Accepted → Trade in game → Done.
 * reached = index of the current step (0..3), or 4 when everything is done.
 */
@Composable
fun TradeSteps(reached: Int, modifier: Modifier = Modifier) {
    val labels = listOf("Offer sent", "Accepted", "Trade in game", "Done")
    Row(modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            val done = i < reached
            val current = i == reached
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(if (done) TcgColors.Ink else if (current) TcgColors.Surface else TcgColors.LineSoft)
                            .then(if (current) Modifier.border(2.dp, TcgColors.Red, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (done) "✓" else "${i + 1}",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (done) Color.White else if (current) TcgColors.Red else TcgColors.Faint,
                        )
                    }
                    if (i < labels.lastIndex) {
                        Box(Modifier.weight(1f).height(2.dp).padding(horizontal = 4.dp).background(if (done) TcgColors.Ink else TcgColors.LineSoft))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label, style = MaterialTheme.typography.labelSmall,
                    color = if (done || current) TcgColors.Ink else TcgColors.Faint,
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Confirmation
// ---------------------------------------------------------------------------

/**
 * Styled confirmation. swap shows "You give ⇄ You get" for trades.
 * Destructive actions use danger = true (red outline confirm button).
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
    danger: Boolean = false,
    swap: Pair<String, String>? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TcgColors.Surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (swap != null) SwapSummary(give = swap.first, get = swap.second)
                Text(message, style = MaterialTheme.typography.bodyLarge, color = TcgColors.Muted)
            }
        },
        confirmButton = {
            TcgButton(confirmText, onClick = onConfirm, style = if (danger) TcgButtonStyle.Danger else TcgButtonStyle.Primary)
        },
        dismissButton = { TcgButton(dismissText, onClick = onDismiss, style = TcgButtonStyle.Secondary) },
    )
}

/** "You give X ⇄ You get Y" box. */
@Composable
fun SwapSummary(give: String, get: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TcgColors.Soft).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("You give", style = MaterialTheme.typography.labelMedium, color = TcgColors.Muted)
            Text(give, style = MaterialTheme.typography.titleMedium, color = TcgColors.Ink)
        }
        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = TcgColors.Diamond)
        Column(Modifier.weight(1f)) {
            Text("You get", style = MaterialTheme.typography.labelMedium, color = TcgColors.Muted)
            Text(get, style = MaterialTheme.typography.titleMedium, color = TcgColors.Ink)
        }
    }
}

/** White rounded panel used for list rows and sections. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TcgColors.Surface).padding(padding)) { content() }
}

/** Keeps a value in state that resets when key changes; small helper for dialogs. */
@Composable
fun <T> rememberDialogTarget(): androidx.compose.runtime.MutableState<T?> = remember { mutableStateOf(null) }
