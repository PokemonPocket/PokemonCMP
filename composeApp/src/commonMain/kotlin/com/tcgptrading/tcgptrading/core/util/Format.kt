package com.tcgptrading.tcgptrading.core.util

import androidx.compose.ui.graphics.Color
import com.tcgptrading.tcgptrading.core.data.network.ApiConfig
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Rarity as the game shows it. */
data class Rarity(val glyphs: String, val label: String, val color: Color)

/** Accepts a short code (C, U, R, RR, AR, SR, IM, UR) or a rarity name ("Uncommon", "Double Rare"…). */
fun rarityOf(value: String?): Rarity? {
    val key = value?.trim()?.lowercase() ?: return null
    val d = TcgColors.Diamond
    val s = TcgColors.Star
    return when (key) {
        "c", "common", "1 diamond" -> Rarity("◆", "One diamond", d)
        "u", "uncommon", "2 diamond" -> Rarity("◆◆", "Two diamonds", d)
        "r", "rare", "3 diamond" -> Rarity("◆◆◆", "Three diamonds", d)
        "rr", "double rare", "4 diamond" -> Rarity("◆◆◆◆", "Four diamonds", d)
        "ar", "art rare", "1 star" -> Rarity("★", "One star", s)
        "sr", "super rare", "2 stars" -> Rarity("★★", "Two stars", s)
        "im", "immersive rare", "3 stars" -> Rarity("★★★", "Three stars", s)
        "ur", "ultra rare", "crown rare" -> Rarity("♛", "Crown rare", TcgColors.Crown)
        else -> null
    }
}

/** Rarity filter options, in game order. Keys are the backend's Rarity enum names. */
val RARITY_FILTERS = listOf("C", "U", "R", "RR", "AR", "SR", "IM", "UR")

/** Expansion codes the backend uses, with their names. */
val EXPANSIONS = linkedMapOf(
    "A1" to "Genetic Apex",
    "A1a" to "Mythical Island",
    "A2" to "Space-Time Smackdown",
    "A2a" to "Triumphant Light",
    "A2b" to "Shining Revelry",
    "A3" to "Celestial Guardians",
    "A3a" to "Extradimensional Crisis",
    "A3b" to "Eevee Grove",
    "A4" to "Wisdom of Sea and Sky",
    "A4a" to "Secluded Springs",
    "A4b" to "Deluxe Pack ex",
    "B1" to "Mega Rising",
)

/** Promo sets: shown in the collection, but not a filter on wants (the backend spells them PROMO_A there). */
private val PROMO_SETS = mapOf("PROMO-A" to "Promo A", "PROMO-B" to "Promo B")

fun expansionName(code: String?): String = code?.let {
    EXPANSIONS[it] ?: PROMO_SETS[it.replace('_', '-')] ?: it.replace('_', '-')
} ?: ""

/** Languages a want can be for (backend Language enum). */
val LANGUAGES = listOf("English", "Italian", "French", "German", "Brazilian", "Spanish", "Japanese", "Chinese", "Korean")

/**
 * Card images are stored as Google Drive share links. Their copies on Cloudflare Pages are named by the
 * Drive file id (Drive rate-limits image traffic), so the link maps straight to the copy.
 * large = 600px wide; otherwise 360px, enough for every card size in the app.
 */
fun directImageUrl(url: String?, large: Boolean = false): String? {
    if (url.isNullOrBlank()) return null
    val id = Regex("/d/([a-zA-Z0-9_-]{10,})/?").find(url)?.groupValues?.get(1)
    return if (id != null) "${ApiConfig.CARD_IMAGES_URL}/${if (large) "l" else "s"}/$id.webp" else url
}

/** Players are shown by the last 4 digits of their Friend ID, never by email. */
fun playerLabel(friendId: String?): String {
    val digits = friendId?.filter { it.isDigit() }.orEmpty()
    return if (digits.length >= 4) "Player ${digits.takeLast(4)}" else "A player"
}

/** Friend ID in groups of 4: "1190 4583 2201 7764". */
fun groupFriendId(friendId: String?): String =
    friendId?.filter { it.isDigit() }?.chunked(4)?.joinToString(" ").orEmpty()

/** Name shown for a chat contact: the email's local part. */
fun nameFromEmail(email: String?): String = email?.substringBefore('@').orEmpty()

fun offersText(count: Int?): String = when (val n = count ?: 0) {
    0 -> "No offers yet"
    1 -> "1 offer"
    else -> "$n offers"
}

private fun parseInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { LocalDate.parse(value.take(10)).let { Instant.parse("${it}T00:00:00Z") } }.getOrNull()
}

/** "Just now", "5 minutes ago", "Yesterday", "3 days ago", else "13 Oct". */
fun timeAgo(value: String?, now: Instant = Clock.System.now()): String {
    val then = parseInstant(value) ?: return ""
    val mins = (now - then).inWholeMinutes
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> if (mins == 1L) "1 minute ago" else "$mins minutes ago"
        mins < 60 * 24 -> (mins / 60).let { if (it == 1L) "1 hour ago" else "$it hours ago" }
        mins < 60 * 48 -> "Yesterday"
        mins < 60 * 24 * 7 -> "${mins / (60 * 24)} days ago"
        else -> shortDate(value)
    }
}

/** "Requested 3 days ago", "Requested yesterday", "Requested on 13 Oct". */
fun requestedText(value: String?): String {
    val ago = timeAgo(value)
    return when {
        ago.isEmpty() -> ""
        ago.endsWith(" ago") -> "Requested $ago"
        ago == "Just now" || ago == "Yesterday" -> "Requested ${ago.lowercase()}"
        else -> "Requested on $ago"
    }
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun shortDate(value: String?): String {
    val local = parseInstant(value)?.toLocalDateTime(TimeZone.currentSystemDefault()) ?: return ""
    return "${local.dayOfMonth} ${MONTHS[local.monthNumber - 1]}"
}

/** "21:32" in the device's time zone. */
fun clockTime(value: String?): String {
    val local = parseInstant(value)?.toLocalDateTime(TimeZone.currentSystemDefault()) ?: return ""
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}

/** Day separator in chats: "Today", "Yesterday" or "13 Oct". */
fun dayLabel(value: String?, now: Instant = Clock.System.now()): String {
    val tz = TimeZone.currentSystemDefault()
    val day = parseInstant(value)?.toLocalDateTime(tz)?.date ?: return ""
    val today = now.toLocalDateTime(tz).date
    return when (today.toEpochDays() - day.toEpochDays()) {
        0 -> "Today"
        1 -> "Yesterday"
        else -> shortDate(value)
    }
}

fun isWithinHours(value: String?, hours: Int, now: Instant = Clock.System.now()): Boolean {
    val then = parseInstant(value) ?: return false
    return (now - then).inWholeHours < hours
}
