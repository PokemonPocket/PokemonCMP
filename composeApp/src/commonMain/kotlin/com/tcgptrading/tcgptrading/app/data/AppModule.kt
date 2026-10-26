package com.tcgptrading.tcgptrading.app.data

import com.tcgptrading.tcgptrading.app.presentation.AppState
import com.tcgptrading.tcgptrading.app.presentation.AppViewModel
import com.tcgptrading.tcgptrading.core.data.AppPrefs
import com.tcgptrading.tcgptrading.core.data.ChatSocket
import com.tcgptrading.tcgptrading.core.data.Session
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.network.createHttpClient
import com.tcgptrading.tcgptrading.features.account.accountModule
import com.tcgptrading.tcgptrading.features.chat.chatModule
import com.tcgptrading.tcgptrading.features.collection.collectionModule
import com.tcgptrading.tcgptrading.features.trading.tradingModule
import com.tcgptrading.tcgptrading.features.wants.wantsModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Shared services: one HTTP client, the API, the live chat connection, the session and local flags. */
val coreModule = module {
    single { createHttpClient() }
    single { TradingApi(get()) }
    single { ChatSocket(get()) }
    single { Session(get()) }
    single { AppPrefs() }
}

/**
 * The application module. Each feature group registers its own view models:
 * - tradingModule: Wanted, Make offer
 * - wantsModule: Add wants, My trades, Review offers
 * - collectionModule: Collection (owned cards, spares, adding wants)
 * - chatModule: Chats, Conversation
 * - accountModule: Profile, Contact, Friend ID, tour
 */
val appModule = module {
    includes(coreModule, tradingModule, wantsModule, collectionModule, chatModule, accountModule)
    viewModelOf(::AppViewModel)
    single { AppState() }
}
