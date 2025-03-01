package com.tcgptrading.tcgptrading.app.presentation

import kotlinx.serialization.Serializable

/**
 * `AppState` is a data class in Kotlin, primarily used to denote the state of the application at any given time.
 *
 * @property loggedIn represents the login status of the user. By default, it is set to `false`.
 *
 * This class also contains a sealed interface `PartialState` which contains different states for
 * login status, bottom navigation visibility, and floating action button visibility.
 * The sealed interface makes it possible to have a limited and known number of `PartialState` instances .
 * The types of PartialState are defined within the interface.
 */
@Serializable
data class AppState(
    val loggedIn: Boolean = false,
    val tokenId: String? = null,
) {
    sealed interface PartialState {
        data object LoggedIn : PartialState
        data class TokenIdChanged(val idToken: String?): PartialState
    }
}
