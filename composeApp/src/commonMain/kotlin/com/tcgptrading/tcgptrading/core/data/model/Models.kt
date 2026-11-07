package com.tcgptrading.tcgptrading.core.data.model

import kotlinx.serialization.Serializable

/*
 * Models matching the backend JSON (PokemonApi). Unknown fields are ignored by the JSON config,
 * so only what the app uses is declared. Nullable where the backend may omit a value.
 */

/** Spring Data page as serialized by the backend: { content: [...], page: { ... } }. */
@Serializable
data class PageDto<T>(
    val content: List<T> = emptyList(),
    val page: PageMeta = PageMeta(),
)

@Serializable
data class PageMeta(
    val size: Int = 0,
    val number: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class Card(
    val cardId: String,
    val name: String = "",
    val aliasId: String? = null,
    val tradable: Boolean? = null,
    val rarity: String? = null,
    val rarityName: String? = null,
    val expansion: String? = null,
    val illustrationUrl: String? = null,
    val description: String? = null,
    val dustCost: Int? = null,
    /** Number within its set (shown as "#004"). */
    val collectionNumber: Int? = null,
)

/** A want: a card a player is looking for. */
@Serializable
data class TradeRequest(
    val requestId: String,
    val cardId: String = "",
    val userFriendId: String? = null,
    val imageUrl: String? = null,
    val message: String? = null,
    val language: String? = null,
    val rarity: String? = null,
    val expansion: String? = null,
    val cardName: String = "",
    val cardTraded: Boolean? = null,
    val acceptedOffer: String? = null,
    val offerCount: Int? = null,
    val tradeDate: String? = null,
    val creationDate: String? = null,
)

/** One exact card (print): names repeat across sets, the art is what counts. */
@Serializable
data class CardRef(
    val cardId: String? = null,
    val name: String = "",
    val imageUrl: String? = null,
)

/**
 * An offer on a want. The bidder gives the wanted card (cardId) and asks for one of
 * requestedCards back; the requester picks which one (acceptedCard / acceptedCardId).
 */
@Serializable
data class Offer(
    val id: String,
    val requesterFriendId: String? = null,
    val bidderFriendId: String? = null,
    val cardId: String = "",
    val imageUrl: String? = null,
    val requestId: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val requestedCards: List<String> = emptyList(),
    val requestedCardIds: List<String>? = null,
    val requestedCardRefs: List<CardRef>? = null,
    val acceptedCard: String? = null,
    val acceptedCardId: String? = null,
    val tradeDate: String? = null,
    val completedAt: String? = null,
    val creationDate: String? = null,
)

/** An accepted trade with another player, described from the signed-in user's side. */
@Serializable
data class TradeSummary(
    val offerId: String,
    val youGive: String? = null,
    val youGiveImage: String? = null,
    val youGet: String? = null,
    val youGetImage: String? = null,
    val youGetOptions: List<String>? = null,
    val partnerFriendId: String? = null,
    val acceptedOn: String? = null,
    val expiresInDays: Int? = null,
    val completedAt: String? = null,
)

@Serializable
data class UserProfile(
    val email: String = "",
    val friendId: String? = null,
    val acceptNotifications: Boolean? = null,
    val tutorialCompleted: Boolean? = null,
    val lastActiveAt: String? = null,
)

@Serializable
data class ChatContact(
    val user: UserProfile? = null,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0,
)

@Serializable
data class ChatMessage(
    val id: String? = null,
    val sender: String = "",
    val recipient: String = "",
    val message: String = "",
    val isRead: Boolean? = null,
    val creationDate: String? = null,
)

@Serializable
data class UserStatus(
    val online: Boolean = false,
    val lastActiveAt: String = "",
)

@Serializable
data class OfferCreate(
    val requesterFriendId: String,
    val bidderFriendId: String,
    val cardId: String,
    val requestId: String,
    val requestedCards: List<String>,
    val requestedCardIds: List<String>,
)

@Serializable
data class RequestCreate(
    val cardId: String,
    val userFriendId: String? = null,
    val message: String? = null,
    val language: String,
)

@Serializable
data class OutgoingChatMessage(
    val sender: String,
    val recipient: String,
    val message: String,
)

@Serializable
data class ReadReceipt(val readBy: String = "")

// ---------- Collection ----------

/** My collection: cardId -> copies (1 = owned, 2+ = owned with spares). Missing cards are absent. */
@Serializable
data class CollectionView(val cards: Map<String, Int> = emptyMap())

@Serializable
data class CollectionBulkUpdate(val cardIds: List<String>, val count: Int)

/** How many cards of one expansion I own. */
@Serializable
data class SetProgress(val code: String, val total: Long = 0, val owned: Long = 0)

/** One of my spare cards that other players currently want. */
@Serializable
data class SpareMatch(
    val cardId: String,
    val name: String = "",
    val imageUrl: String? = null,
    val rarityName: String? = null,
    val spares: Int = 0,
    val wantedBy: Long = 0,
)

@Serializable
data class SpareMatches(val totalSparesWanted: Long = 0, val cards: List<SpareMatch> = emptyList())

// ---------- Wanted tabs (/wanted) — every list is newest first ----------

/** A card as the Wanted tabs send it. rarityName is the long name ("Double Rare"). */
@Serializable
data class WantedCard(
    val cardId: String,
    val aliasId: String? = null,
    val name: String = "",
    val illustrationUrl: String? = null,
    val rarityName: String? = null,
)

/** Wants of one player from one day. total can be larger than requests: only the first ones come. */
@Serializable
data class FeedGroup(
    val userFriendId: String? = null,
    val latest: String? = null,
    val total: Int = 0,
    val requests: List<TradeRequest> = emptyList(),
)

@Serializable
data class FeedPage(
    val content: List<FeedGroup> = emptyList(),
    val totalRequests: Long = 0,
    val totalGroups: Long = 0,
    val page: Int = 0,
    val totalPages: Int = 0,
)

/** One card and how many players want it. rarity is the short code (RR). */
@Serializable
data class CardDemand(
    val cardId: String,
    val cardName: String = "",
    val rarity: String? = null,
    val expansion: String? = null,
    val imageUrl: String? = null,
    val wantedBy: Long = 0,
    val latest: String? = null,
)

@Serializable
data class CardDemandPage(
    val content: List<CardDemand> = emptyList(),
    val totalElements: Long = 0,
    val page: Int = 0,
    val totalPages: Int = 0,
)

/** They want give (my spare) and have get spare, which I'm missing. */
@Serializable
data class WantedSwap(
    val request: TradeRequest,
    val give: WantedCard,
    val get: WantedCard,
    val mySpares: Int = 0,
    val moreWithTrader: Int = 0,
    val traderActiveAt: String? = null,
)

/** They want a spare of mine, with nothing I'm missing to give back in that rarity. */
@Serializable
data class WantedHelp(
    val request: TradeRequest,
    val give: WantedCard,
    val mySpares: Int = 0,
    val wantedBy: Long = 0,
)

@Serializable
data class ForYou(
    val hasCollection: Boolean = false,
    val spareCards: Int = 0,
    val swaps: List<WantedSwap> = emptyList(),
    val help: List<WantedHelp> = emptyList(),
)

/** A card to ask for back. reason: THEIR_SPARE (they have it spare), YOUR_WANT (on my wants) or MISSING (I don't own it). */
@Serializable
data class WantedSuggest(val card: WantedCard, val reason: String = "MISSING")

/**
 * One want of a trader that I can fill. get: a card they have spare that I'm missing, or null.
 * suggest: up to 5 cards of the same rarity I don't own, best first.
 */
@Serializable
data class TradeLine(
    val requestId: String,
    val give: WantedCard,
    val get: WantedCard? = null,
    val mySpares: Int = 0,
    /** Null when the server is older than suggestions. */
    val suggest: List<WantedSuggest>? = null,
)

@Serializable
data class WantedTrader(
    val userFriendId: String,
    val latestWant: String? = null,
    val activeAt: String? = null,
    val openWants: Long = 0,
    /** How many of their wants I can fill (2 or more). */
    val trades: Int = 0,
    /** How many of those they can pay back with a card I'm missing. */
    val swaps: Int = 0,
    /** Their completed trades on the site. */
    val tradesDone: Long = 0,
    val give: List<TradeLine> = emptyList(),
    val get: List<WantedCard> = emptyList(),
)

@Serializable
data class WantedTraders(
    val hasCollection: Boolean = false,
    val spareCards: Int = 0,
    /** When I last changed my collection: every match is read from it. */
    val collectionUpdatedAt: String? = null,
    val traders: List<WantedTrader> = emptyList(),
)

/** Who wants one card right now (newest first), from the card page endpoint. */
@Serializable
data class CardWant(
    val requestId: String,
    val cardId: String = "",
    val userFriendId: String? = null,
    val language: String? = null,
    val offerCount: Int? = null,
    val message: String? = null,
    val creationDate: String? = null,
    /** A card of the same rarity this player has spare and I'm missing. */
    val canGive: String? = null,
)

@Serializable
data class CardMarket(val cardId: String = "", val wantedCount: Long = 0, val wants: List<CardWant> = emptyList())
