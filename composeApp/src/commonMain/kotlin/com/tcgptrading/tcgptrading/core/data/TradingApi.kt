package com.tcgptrading.tcgptrading.core.data

import com.tcgptrading.tcgptrading.core.data.model.Card
import com.tcgptrading.tcgptrading.core.data.model.ChatContact
import com.tcgptrading.tcgptrading.core.data.model.ChatMessage
import com.tcgptrading.tcgptrading.core.data.model.CollectionBulkUpdate
import com.tcgptrading.tcgptrading.core.data.model.CollectionView
import com.tcgptrading.tcgptrading.core.data.model.CardDemandPage
import com.tcgptrading.tcgptrading.core.data.model.CardMarket
import com.tcgptrading.tcgptrading.core.data.model.FeedPage
import com.tcgptrading.tcgptrading.core.data.model.ForYou
import com.tcgptrading.tcgptrading.core.data.model.WantedTraders
import com.tcgptrading.tcgptrading.core.data.model.SetProgress
import com.tcgptrading.tcgptrading.core.data.model.SpareMatches
import com.tcgptrading.tcgptrading.core.data.model.Offer
import com.tcgptrading.tcgptrading.core.data.model.OfferCreate
import com.tcgptrading.tcgptrading.core.data.model.PageDto
import com.tcgptrading.tcgptrading.core.data.model.RequestCreate
import com.tcgptrading.tcgptrading.core.data.model.TradeRequest
import com.tcgptrading.tcgptrading.core.data.model.TradeSummary
import com.tcgptrading.tcgptrading.core.data.model.UserProfile
import com.tcgptrading.tcgptrading.core.data.model.UserStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Every backend call the app makes, mirroring the website's services.
 * Calls throw on failure; use [runApi] to turn that into a readable message.
 */
class TradingApi(private val client: HttpClient) {

    // ---------- Cards ----------

    /** Tradable cards, optionally by name ("all" = no filter). */
    suspend fun cards(search: String, page: Int, size: Int = 30): PageDto<Card> =
        client.get("cards/pokemon/${search.ifBlank { "all" }.encodeURLPathPart()}") {
            parameter("page", page); parameter("size", size)
        }.body()

    /** Cards of one rarity (rarityName, e.g. "Rare"), optionally by name: used to pick what you want back. */
    suspend fun cardsOfRarity(rarityName: String, search: String, page: Int, size: Int = 30): PageDto<Card> =
        client.get("cards/all/rarity/${rarityName.encodeURLPathPart()}") {
            parameter("pokemonName", search.ifBlank { "all" })
            parameter("page", page); parameter("size", size)
        }.body()

    suspend fun card(cardId: String): Card = client.get("cards/id/${cardId.encodeURLPathPart()}").body()

    /** Every card of one expansion in set-number order (collection grid). rarity: code like "RR". */
    suspend fun cardsOfExpansion(code: String, page: Int, size: Int = 120, rarity: String? = null, search: String? = null): PageDto<Card> =
        client.get("cards/expansion/${code.encodeURLPathPart()}") {
            parameter("page", page); parameter("size", size)
            rarity?.let { parameter("rarity", it) }
            search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
        }.body()

    // ---------- Collection ----------

    suspend fun collection(): CollectionView = client.get("collection").body()

    /** Sets the copies of one card (0 = missing). Returns the whole collection. */
    suspend fun setCollectionCount(cardId: String, count: Int): CollectionView =
        client.put("collection/${cardId.encodeURLPathPart()}") { parameter("count", count) }.body()

    suspend fun setCollectionCounts(cardIds: List<String>, count: Int): CollectionView =
        client.post("collection/bulk") {
            contentType(ContentType.Application.Json); setBody(CollectionBulkUpdate(cardIds, count))
        }.body()

    suspend fun collectionSets(): List<SetProgress> = client.get("collection/sets").body()

    suspend fun spareMatches(): SpareMatches = client.get("collection/matches").body()

    // ---------- Wants (requests) ----------

    /** The public Wanted list. My own wants are hidden by the backend when signed in. */
    suspend fun wanted(
        page: Int, size: Int = 20, cardName: String? = null,
        rarity: String? = null, expansion: String? = null, language: String? = null,
    ): PageDto<TradeRequest> = client.get("requestCard/filtered") {
        parameter("page", page); parameter("size", size)
        cardName?.takeIf { it.isNotBlank() }?.let { parameter("cardName", it) }
        rarity?.let { parameter("rarity", it) }
        expansion?.let { parameter("expansion", it) }
        language?.let { parameter("language", it) }
    }.body()

    /**
     * Wanted › All wants, newest first. One player's wants from the same day come as one group.
     * friendId shows one player's wants only; spareOnly keeps the cards I have spare.
     */
    suspend fun wantedFeed(
        page: Int, size: Int = 24, cardName: String? = null,
        rarity: String? = null, expansion: String? = null, language: String? = null,
        friendId: String? = null, spareOnly: Boolean = false,
    ): FeedPage = client.get("wanted/feed") {
        parameter("page", page); parameter("size", size)
        cardName?.takeIf { it.isNotBlank() }?.let { parameter("cardName", it) }
        rarity?.let { parameter("rarity", it) }
        expansion?.let { parameter("expansion", it) }
        language?.let { parameter("language", it) }
        friendId?.takeIf { it.isNotBlank() }?.let { parameter("userFriendId", it) }
        if (spareOnly) parameter("spareOnly", true)
    }.body()

    /** Wanted › By card: one row per card. sort: "newest" (latest want first) or "popular". */
    suspend fun wantedByCard(
        page: Int, size: Int = 24, sort: String = "newest", cardName: String? = null,
        rarity: String? = null, expansion: String? = null, language: String? = null, spareOnly: Boolean = false,
    ): CardDemandPage = client.get("wanted/by-card") {
        parameter("page", page); parameter("size", size); parameter("sort", sort)
        cardName?.takeIf { it.isNotBlank() }?.let { parameter("cardName", it) }
        rarity?.let { parameter("rarity", it) }
        expansion?.let { parameter("expansion", it) }
        language?.let { parameter("language", it) }
        if (spareOnly) parameter("spareOnly", true)
    }.body()

    /** Wanted › For you: wants I can fill with my spares, two-way swaps first. */
    suspend fun wantedForYou(): ForYou = client.get("wanted/for-you").body()

    /** Wanted › Traders: players who want my spares, newest want first. */
    suspend fun wantedTraders(): WantedTraders = client.get("wanted/traders").body()

    /** Who wants one card, newest first, with what each player could give me back. */
    suspend fun cardMarket(cardId: String): CardMarket = client.get("cards/id/${cardId.encodeURLPathPart()}/market").body()

    /** My wants. status: "all", "open" or "accepted". */
    suspend fun myWants(page: Int, size: Int = 50, cardName: String = "all", status: String = "all"): PageDto<TradeRequest> =
        client.get("requestCard/own-requests") {
            parameter("page", page); parameter("size", size)
            parameter("cardName", cardName.ifBlank { "all" }); parameter("status", status)
        }.body()

    suspend fun addWant(request: RequestCreate) {
        client.post("requestCard/save") { contentType(ContentType.Application.Json); setBody(request) }
    }

    suspend fun removeWant(requestId: String) {
        client.delete("requestCard/delete/$requestId")
    }

    // ---------- Offers ----------

    suspend fun makeOffer(offer: OfferCreate) {
        client.post("offers/save") { contentType(ContentType.Application.Json); setBody(offer) }
    }

    /** My offers. status: "all", "PENDING" or "ACCEPTED". */
    suspend fun myOffers(page: Int, size: Int = 50, status: String = "all"): PageDto<Offer> =
        client.get("offers/own-offers") {
            parameter("status", status); parameter("page", page); parameter("size", size)
        }.body()

    /** Offers on one of my wants. sortBy: "newest" or "oldest". */
    suspend fun offersOnWant(requestId: String, page: Int = 0, size: Int = 50, sortBy: String = "newest"): PageDto<Offer> =
        client.get("offers/all/$requestId") {
            parameter("page", page); parameter("size", size); parameter("sortBy", sortBy)
        }.body()

    suspend fun offer(offerId: String): Offer = client.get("offers/$offerId").body()

    /** Accept, giving back one specific card (cardId for new offers, name for older ones). */
    suspend fun acceptOffer(offerId: String, cardName: String?, cardId: String?) {
        client.get("offers/accept/$offerId") {
            cardName?.let { parameter("card", it) }
            cardId?.let { parameter("cardId", it) }
        }
    }

    /** Declining = reject then delete, like the website. */
    suspend fun declineOffer(offerId: String) {
        client.get("offers/reject/$offerId")
        client.delete("offers/delete/$offerId")
    }

    suspend fun withdrawOffer(offerId: String) {
        client.delete("offers/delete/$offerId")
    }

    suspend fun tradesWith(email: String): List<TradeSummary> =
        client.get("offers/with/${email.encodeURLPathPart()}").body()

    suspend fun completeTrade(offerId: String) {
        client.post("offers/complete/$offerId")
    }

    // ---------- User ----------

    /** Creates the user on first sign-in (no-op afterwards). */
    suspend fun saveUser() {
        client.get("users/saveUser")
    }

    suspend fun me(): UserProfile = client.get("users/getUser").body()

    suspend fun hasFriendId(): Boolean = client.get("users/hasFriendId").body()

    /** Returns false when the Friend ID is already taken. */
    suspend fun setFriendId(friendId: String): Boolean =
        client.post("users/updateUser") {
            contentType(ContentType.Application.Json); setBody(mapOf("friendId" to friendId))
        }.body()

    suspend fun setEmailNotifications(enabled: Boolean) {
        client.post("users/updateEmailSettings") { parameter("acceptNotifications", enabled) }
    }

    suspend fun setTutorialCompleted(completed: Boolean) {
        client.post("users/tutorialCompleted") { parameter("completed", completed) }
    }

    suspend fun deleteAccount() {
        client.delete("users/deleteUser")
    }

    /** Email of the player with this Friend ID (only after a trade, to open the chat). */
    suspend fun emailOf(friendId: String): String = client.get("users/getFriendEmail/$friendId").bodyAsText()

    // ---------- Chat ----------

    suspend fun chats(): List<ChatContact> = client.get("api/contacts").body()

    suspend fun messages(me: String, peer: String): List<ChatMessage> =
        client.get("api/messages/${me.encodeURLPathPart()}/${peer.encodeURLPathPart()}").body()

    suspend fun markRead(peerEmail: String) {
        client.post("api/messages/read/${peerEmail.encodeURLPathPart()}")
    }

    suspend fun status(email: String): UserStatus = client.get("api/user/status/${email.encodeURLPathPart()}").body()

    // ---------- Contact ----------

    suspend fun contact(category: String, subject: String, message: String, name: String?) {
        client.post("contact/send") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("category" to category, "subject" to subject, "message" to message, "name" to (name ?: "")))
        }
    }
}

/**
 * Runs an API call and returns its value, or a short message in the app's voice when it fails.
 * Cancellation is rethrown so leaving a screen stops its work.
 */
suspend fun <T> runApi(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: ClientRequestException) {
    val server = serverMessageOf(e)
    val message = when (e.response.status) {
        HttpStatusCode.Unauthorized -> "Please sign in again."
        HttpStatusCode.Forbidden -> "You can't do that."
        HttpStatusCode.NotFound -> "It no longer exists."
        // The server says what changed ("You already made an offer on this want", "Request was already traded")
        HttpStatusCode.Conflict -> server ?: "This changed in the meantime. Refresh and try again."
        // 400 explains what was wrong with the request ("Offered card doesn't match the want")
        HttpStatusCode.BadRequest -> server ?: "Something went wrong. Try again."
        else -> "Something went wrong. Try again."
    }
    Result.failure(ApiException(message, e, server))
} catch (e: ResponseException) {
    Result.failure(ApiException("The server had a problem. Try again in a moment.", e))
} catch (e: Exception) {
    Result.failure(ApiException("Can't reach TCGP Trading. Check your connection.", e))
}

/** The `message` of the server's JSON error body as a sentence, or null when it sent none. */
private suspend fun serverMessageOf(e: ResponseException): String? = try {
    val body = e.response.bodyAsText()
    val text = ((Json.parseToJsonElement(body) as? JsonObject)?.get("message") as? JsonPrimitive)
        ?.takeIf { it.isString }?.content?.trim()
    when {
        text.isNullOrEmpty() -> null
        text.last().isLetterOrDigit() -> "$text."
        else -> text
    }
} catch (c: CancellationException) {
    throw c
} catch (_: Exception) {
    null
}

/** serverMessage: what the server said, when its error body had a message. */
class ApiException(message: String, cause: Throwable? = null, val serverMessage: String? = null) : Exception(message, cause)

/** True when the server answered 409 Conflict, e.g. an offer already made on this want. */
fun Throwable.isConflict(): Boolean = (cause as? ClientRequestException)?.response?.status == HttpStatusCode.Conflict

/** The server's own message for this failure, if it sent one. */
val Throwable.serverMessage: String? get() = (this as? ApiException)?.serverMessage
