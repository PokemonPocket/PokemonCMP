package com.tcgptrading.tcgptrading.features.wants

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Wants feature: Add wants, My trades (my offers and my wants) and Review offers on a want.
 * Screens live in AddWantsScreen.kt, MyTradesScreen.kt and ReviewOffersScreen.kt.
 */
val wantsModule = module {
    viewModelOf(::AddWantsViewModel)
    viewModelOf(::MyTradesViewModel)
    viewModelOf(::ReviewOffersViewModel)
}
