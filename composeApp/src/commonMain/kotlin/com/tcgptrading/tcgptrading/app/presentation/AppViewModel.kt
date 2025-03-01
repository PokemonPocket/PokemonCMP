package com.tcgptrading.tcgptrading.app.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.app.presentation.AppEffect.NavigateToScreen
import com.tcgptrading.tcgptrading.app.presentation.AppEffect.ShowSnackbar
import com.tcgptrading.tcgptrading.app.presentation.AppState.PartialState
import com.tcgptrading.tcgptrading.core.base.BaseViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch


/**
 * `AppViewModel` is a class that acts as a ViewModel in the Model-View-ViewModel architecture (MVVM). It inherits from the
 * `BaseViewModel` class and defines its generic parameters as `AppState`, `PartialState`, `AppIntent` and `AppEffect`.
 *
 * An instance of `AppViewModel` is created with an initial application state (`AppState`) and a state holder (`savedStateHolder`).
 *
 * `AppViewModel` primarily serves two purposes:
 * 1. It maps incoming intents (`AppIntent`) to the corresponding flow of partial states (`Flow<PartialState>`).
 * 2. It updates the current UI state based on the provided partial state (`PartialState`).
 *
 * Upon initialization, `AppViewModel` adds an anonymous listener to the `NotifierManager`.
 */
class AppViewModel(
    initialState: AppState,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AppState, PartialState, AppIntent, AppEffect>(
    initialState = initialState,
    savedStateHandle = savedStateHandle,
    uiStateClass = AppState::class,
) {

    init {
        viewModelScope.launch {
            Firebase.auth.authStateChanged.collect { user ->
                user?.let {
                    acceptIntent(
                        AppIntent.ChangeUserLogInState(
                            true,
                            it.getIdToken(forceRefresh = true)
                        )
                    )
                } ?: run {
                    acceptIntent(AppIntent.ChangeUserLogInState(false, null))
                }
            }

            Firebase.auth.idTokenChanged.collect { user ->
                acceptIntent(AppIntent.ChangeUserIdToken(user?.getIdToken(forceRefresh = true)))
            }

        }
//        NotifierManager.addListener(object : NotifierManager.Listener {
//            override fun onNewToken(token: String) {
//                Napier.d("onNewToken: $token") //Update user token in the server if needed
//            }
//        })
    }

    /**
     * The function `mapIntents` maps the provided `AppIntent` to a corresponding `Flow<PartialState>`.
     * Based on the type of `AppIntent` passed to this function, it will dispatch an appropriate action.
     *
     * - `AppIntent.ShowSnackbar`: It will send an effect to show Snackbar.
     * - `AppIntent.NavigateToScreen`: It will send an effect to navigate to a certain screen in the application.
     * - `AppIntent.ShowBottomNavigation`: Returns a flow containing the partial state for showing the Bottom Navigation.
     * - `AppIntent.ShowBottomNavigationAndFloatingAb`: Returns a flow containing the partial state for showing the Bottom Navigation and Floating Action Button.
     * - `AppIntent.ShowFloatingActionButton`: Returns a flow containing the partial state for showing the Floating Action Button.
     * - `AppIntent.ShowAddEventBottomSheet`: Currently not implemented (TODO).
     *
     * @param intent The given `AppIntent` that needs to be mapped to a corresponding `Flow<PartialState>`.
     * @return A `Flow<PartialState>` corresponding to the provided `AppIntent`.
     */
    override fun mapIntents(intent: AppIntent): Flow<PartialState> =
        when (intent) {
            is AppIntent.ShowSnackbar -> flow {
                sendEffect(ShowSnackbar(intent.snackbarUiModel))
            }

            is AppIntent.NavigateToScreen -> flow {
                sendEffect(NavigateToScreen(intent.route))

            }

            is AppIntent.ChangeUserLogInState -> flow {
                if (intent.loggedIn) {
                    sendEffect(NavigateToScreen(Screen.Main))
                    emit(PartialState.TokenIdChanged(intent.token))
                } else {
                    sendEffect(NavigateToScreen(Screen.Login))
                    emit(PartialState.TokenIdChanged(intent.token))
                }
            }

            is AppIntent.ChangeUserIdToken -> flow {
                emit(PartialState.LoggedIn)
            }
        }

    /**
     * Updates the current UI state based on the provided partial state.
     *
     * This method operates on instances of AppState and PartialState:
     * - AppState represents the overall state of the app.
     * - PartialState represents a minimal change that should be applied to the AppState.
     *
     * The update is performed by copying the current AppState (previousState) and changing specific values based on the partial state.
     *
     * Here are the-handed cases:
     * - Login state: If the user is logged in, updates the loggedIn flag of the AppState to true.
     * - Bottom Navigation display: If the state mandates displaying Bottom Navigation, updates showBottomNavigation flag in AppState.
     * - Floating Action Button display: If the state mandates displaying Floating Action Button, updates showFloatingActionButton flag.
     * - Displaying both Bottom Navigation and Floating button: If this state is received, updates both showBottomNavigation and showFloatingActionButton flags.
     *
     * @param previousState The current application state.
     * @param partialState The change that should be applied to the app state.
     * @return updated version of AppState with new UI configuration.
     */
    override fun reduceUiState(previousState: AppState, partialState: PartialState): AppState =
        when (partialState) {
            is PartialState.LoggedIn -> previousState.copy(loggedIn = true)
            is PartialState.TokenIdChanged -> previousState.copy(tokenId = partialState.idToken)
        }
}
