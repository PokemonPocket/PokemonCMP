package com.tcgptrading.tcgptrading.app.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.app.presentation.AppEffect.NavigateToScreen
import com.tcgptrading.tcgptrading.app.presentation.AppEffect.ShowSnackbar
import com.tcgptrading.tcgptrading.app.presentation.AppState.PartialState
import com.tcgptrading.tcgptrading.core.base.BaseViewModel
import com.tcgptrading.tcgptrading.core.data.ChatSocket
import com.tcgptrading.tcgptrading.core.data.Session
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * App-level state: follows Firebase sign-in, starts the session and the live chat when signed in,
 * and routes between the sign-in screen and the app.
 */
class AppViewModel(
    initialState: AppState,
    savedStateHandle: SavedStateHandle,
    private val session: Session,
    private val chatSocket: ChatSocket,
) : BaseViewModel<AppState, PartialState, AppIntent, AppEffect>(
    initialState = initialState,
    savedStateHandle = savedStateHandle,
    uiStateClass = AppState::class,
) {

    init {
        viewModelScope.launch {
            Firebase.auth.authStateChanged
                .distinctUntilChangedBy { it?.uid }
                .collect { user ->
                    if (user != null) {
                        // Create the user on the backend (first time) and load the profile before showing the app
                        session.start()
                        chatSocket.start()
                        acceptIntent(AppIntent.ChangeUserLogInState(true, null))
                    } else {
                        chatSocket.stop()
                        session.clear()
                        acceptIntent(AppIntent.ChangeUserLogInState(false, null))
                    }
                }
        }
    }

    override fun mapIntents(intent: AppIntent): Flow<PartialState> =
        when (intent) {
            is AppIntent.ShowSnackbar -> flow {
                sendEffect(ShowSnackbar(intent.snackbarUiModel))
            }

            is AppIntent.NavigateToScreen -> flow {
                sendEffect(NavigateToScreen(intent.route))
            }

            is AppIntent.ChangeUserLogInState -> flow {
                sendEffect(NavigateToScreen(if (intent.loggedIn) Screen.Main else Screen.Login))
                emit(PartialState.TokenIdChanged(intent.token))
                if (intent.loggedIn) emit(PartialState.LoggedIn)
            }

            is AppIntent.ChangeUserIdToken -> flow {
                emit(PartialState.LoggedIn)
            }

            AppIntent.LogoutUser -> flow {
                Firebase.auth.signOut()
            }
        }

    override fun reduceUiState(previousState: AppState, partialState: PartialState): AppState =
        when (partialState) {
            is PartialState.LoggedIn -> previousState.copy(loggedIn = true)
            is PartialState.TokenIdChanged -> previousState.copy(tokenId = partialState.idToken)
        }
}
