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
