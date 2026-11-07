package com.tcgptrading.tcgptrading.features.trading

import com.tcgptrading.tcgptrading.core.data.model.TradeRequest
import com.tcgptrading.tcgptrading.core.presentation.components.TagTone
import com.tcgptrading.tcgptrading.core.util.isWithinHours
import com.tcgptrading.tcgptrading.core.util.timeAgo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Text shared by the Wanted tabs (same wording as the website). */

/** "Added 3 hours ago", "Added yesterday", "Added on 12 Oct". */
fun addedText(value: String?): String = withVerb("Added", value)

/** "asked 3 hours ago" (lower case, used after a player's name). */
fun askedText(value: String?): String = withVerb("asked", value)

/** "Active 5 minutes ago", "Active yesterday", "Active on 12 Oct". */
fun activeText(value: String?): String = withVerb("Active", value)

/** "Latest want 2 hours ago". */
fun latestWantText(value: String?): String = withVerb("Latest want", value)

private fun withVerb(verb: String, value: String?): String {
    val ago = timeAgo(value)
    return when {
        ago.isEmpty() -> ""
        ago.endsWith(" ago") || ago == "Just now" || ago == "Yesterday" -> "$verb ${ago.lowercase()}"
        else -> "$verb on $ago"
    }
}

fun sparesText(n: Int): String = if (n == 1) "You have 1 spare" else "You have $n spares"

fun playersText(n: Long): String = if (n == 1L) "1 player" else "$n players"

/** Last two digits of the Friend ID, for the avatar circle. */
fun playerInitials(friendId: String?): String = friendId?.filter { it.isDigit() }?.takeLast(2)?.ifEmpty { null } ?: "?"

/** The label on a wanted card: "New" for the first day, then its offers, else how long it has waited (up to a month). */
data class WantTag(val text: String, val tone: TagTone)

fun wantTag(request: TradeRequest, now: Instant = Clock.System.now()): WantTag? {
    val offers = request.offerCount ?: 0
    if (offers > 0) return WantTag(if (offers == 1) "1 offer" else "$offers offers", TagTone.Soft)
    val created = request.creationDate?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    if (isWithinHours(request.creationDate, 24, now)) return WantTag("New", TagTone.Red)
    val days = (now - created).inWholeDays
    // Past a month the date under the card says enough
    if (days > 30) return null
    return WantTag(if (days == 1L) "Waiting 1 day" else "Waiting $days days", TagTone.Ink)
}

/** The server allows one open offer per player per want. */
const val ALREADY_OFFERED = "You already made an offer on this want. See it in My trades."
