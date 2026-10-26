package com.tcgptrading.tcgptrading.core.data

import com.tcgptrading.tcgptrading.core.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The signed-in player's profile (email, Friend ID, settings), shared by all screens.
 * Loaded after sign-in; cleared on sign-out.
 */
class Session(private val api: TradingApi) {

    private val _me = MutableStateFlow<UserProfile?>(null)
    val me: StateFlow<UserProfile?> = _me.asStateFlow()

    val email: String? get() = _me.value?.email?.takeIf { it.isNotBlank() }
    val friendId: String? get() = _me.value?.friendId?.takeIf { it.isNotBlank() }

    /** Creates the user on first sign-in, then loads the profile. */
    suspend fun start(): Result<UserProfile> = runApi {
        api.saveUser()
        api.me().also { _me.value = it }
    }

    suspend fun refresh(): Result<UserProfile> = runApi { api.me().also { _me.value = it } }

    fun update(transform: (UserProfile) -> UserProfile) {
        _me.value?.let { _me.value = transform(it) }
    }

    fun clear() {
        _me.value = null
    }
}
