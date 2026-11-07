package com.tcgptrading.tcgptrading.app.domain

import kotlinx.serialization.Serializable

/** Top-level destinations: splash while checking sign-in, the sign-in screen, and the signed-in app. */
@Serializable
sealed interface Screen {

    @Serializable
    data object Login : Screen

    @Serializable
    data object Splash : Screen

    @Serializable
    data object Main : Screen
}

/** Bottom tabs of the signed-in app. */
@Serializable
sealed interface Tab {
    @Serializable data object Wanted : Tab
    /** My cards: mark what I own and my spares; missing cards can be added to my wants from here. */
    @Serializable data object Collection : Tab
    /** tab: "offers" or "wants". */
    @Serializable data class MyTrades(val tab: String = "offers") : Tab
    @Serializable data object Chats : Tab
    @Serializable data object Profile : Tab
}

/** Screens opened on top of a tab (they hide the tab bar and the ad). */
@Serializable
sealed interface Detail {
    /**
     * Offer a trade on someone's want: you give the wanted card and pick what you'd like back.
     * want: cards to start picked, comma separated (from Wanted › For you / Traders), at most 5.
     */
    @Serializable data class MakeOffer(
        val requestId: String,
        val cardId: String,
        val requesterFriendId: String,
        val want: String? = null,
    ) : Detail
    /** Offers other players made on one of my wants. */
    @Serializable data class ReviewOffers(val requestId: String, val cardId: String) : Detail
    @Serializable data class Conversation(val email: String) : Detail
    @Serializable data object Contact : Detail
}
