package com.tcgptrading.tcgptrading.features.trading

import com.tcgptrading.tcgptrading.app.domain.Detail
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.koin.dsl.module

/**
 * Lets another tab open Wanted with a search already applied (e.g. Collection's "3 players want Arcanine").
 * The Wanted screen consumes the request once.
 */
class WantedSearchRequests {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun request(search: String) {
        _pending.value = search
    }

    fun consume() {
        _pending.value = null
    }
}

/** A short message for the app's snackbar; seeOffers adds a "See your offers" button. */
data class TradeNotice(val text: String, val seeOffers: Boolean = false)

/**
 * Trading news shared across screens: an offer went out (so For you and Traders reload),
 * and messages the app shows at the bottom (MainScreen collects them, so leaving a tab or a screen never drops one).
 */
class TradingSignals {
    private val _offerSent = MutableStateFlow(0)
    /** Goes up by one each time an offer is sent. */
    val offerSent: StateFlow<Int> = _offerSent.asStateFlow()

    private val _notices = Channel<TradeNotice>(Channel.BUFFERED)
    val notices: Flow<TradeNotice> = _notices.receiveAsFlow()

    fun offerSent() = _offerSent.update { it + 1 }

    fun notify(notice: TradeNotice) {
        _notices.trySend(notice)
    }
}

/**
 * Trading feature: the Wanted list and Make offer.
 * Screens live in WantedScreen.kt and MakeOfferScreen.kt.
 */
val tradingModule = module {
    single { WantedSearchRequests() }
    single { TradingSignals() }
    viewModelOf(::WantedViewModel)
    viewModelOf(::ForYouViewModel)
    viewModelOf(::ByCardViewModel)
    viewModelOf(::TradersViewModel)
    viewModel { params -> MakeOfferViewModel(params.get<Detail.MakeOffer>(), get(), get(), get()) }
}
