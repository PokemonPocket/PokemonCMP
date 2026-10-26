package com.tcgptrading.tcgptrading.features.trading

import com.tcgptrading.tcgptrading.app.domain.Detail
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * Trading feature: the Wanted list and Make offer.
 * Screens live in WantedScreen.kt and MakeOfferScreen.kt.
 */
val tradingModule = module {
    single { WantedSearchRequests() }
    viewModelOf(::WantedViewModel)
    viewModel { params -> MakeOfferViewModel(params.get<Detail.MakeOffer>(), get(), get()) }
}
