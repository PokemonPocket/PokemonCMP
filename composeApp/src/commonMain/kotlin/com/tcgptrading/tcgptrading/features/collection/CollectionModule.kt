package com.tcgptrading.tcgptrading.features.collection

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Collection feature: owned cards, spares and adding missing cards to wants. Screen in CollectionScreen.kt. */
val collectionModule = module {
    viewModelOf(::CollectionViewModel)
}
