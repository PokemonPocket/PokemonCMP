package com.tcgptrading.tcgptrading.core.base

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

// Constant value representing a key used for managing UI state persisting. Specifically used to consume and register relevant UI state
// in the SavedStateHolder within the BaseViewModel class. This key ensures that the UI state is restored correctly when it's consumed
// and registered back.
private const val SAVED_UI_STATE_KEY = "savedUiStateKey"

/**
 * This is the BaseViewModel abstract class. It's a part of base architecture, responsible for the process of state mutation
 * in a reactive and unidirectional manner. The four generic types are UI_STATE, PARTIAL_UI_STATE, INTENT and EFFECT.
 *
 * @param initialState is the initial state of the View.
 * @param savedStateHandle is used to manage persisting view state. It provides a way to consume restorable state and
 *                         register a state provider.
 *
 * The model manages multiple flows:
 * - uiState: it's a cold StateFlow that emit states to the view layer. Its type is UI_STATE.
 * - effect: it's a hot flow emitted from a conflated Channel used for sending state-independent UI updates to the view layer.
 * - userIntents(): it's a function that returns a hot flow of PARTIAL_UI_STATE originated from intents. It is built by flat-mapping
 *                  intents mapped to partial states using mapIntents(), injecting an onStart trigger to notify
 *                  that the flow has started listening for intents. UserIntents() is an Experimental Coroutines API.
 *
 * BaseViewModel exposes two main public functions:
 * - acceptIntent(INTENT): This function accepts an Intent, and emits it to the IntentFlow only if the userIntents flow has started.
 * - sendEffect(EFFECT): It's a function encapsulated in a coroutine. It sends an EFFECT object to the effectChannel.
 *
 * The class also contains two abstract protected functions:
 * - mapIntents(INTENT): This function accepts an INTENT and returns a Flow<PARTIAL_UI_STATE>. It is responsible for
 *                       transforming a flow of user's INTENTS into a flow of PARTIAL_UI_STATE.
 * - reduceUiState(UI_STATE, PARTIAL_UI_STATE): This function takes a previous UI_STATE and a PARTIAL_UI_STATE, processes them and returns an updated UI_STATE.
 */

abstract class BaseViewModel<UI_STATE : Any, PARTIAL_UI_STATE, INTENT, EFFECT>(
    initialState: UI_STATE,
    savedStateHandle: SavedStateHandle,
    uiStateClass: KClass<UI_STATE>,
) : ViewModel() {

    @OptIn(InternalSerializationApi::class)
    private val serializer: KSerializer<UI_STATE> = uiStateClass.serializer()

    val uiState: StateFlow<UI_STATE> =
        savedStateHandle.getStateFlow(
            SAVED_UI_STATE_KEY,
            Json.encodeToString(serializer, initialState)
        ).map { Json.decodeFromString(serializer, it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialState)

    private val effectChannel = Channel<EFFECT>(Channel.CONFLATED)
    val effect = effectChannel.receiveAsFlow()

    private val intentFlowListenerStarted = CompletableDeferred<Unit>()

    private val intentFlow = MutableSharedFlow<INTENT>()
    private val continuousPartialStateFlow = MutableSharedFlow<PARTIAL_UI_STATE>()


    init {
        viewModelScope.launch {
            userIntents()
                .scan(initial = uiState.value, operation = ::reduceUiState)
                .catch { cause -> Napier.e(cause.message ?: "", cause) }
                .collect {
                    savedStateHandle[SAVED_UI_STATE_KEY] = Json.encodeToString(serializer, it)
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun userIntents(): Flow<PARTIAL_UI_STATE> =
        intentFlow
            .flatMapMerge(transform = ::mapIntents)

    /**
     * The `acceptIntent` function accepts an intent and emits it to intentFlow if the `userIntents` flow has started.
     * This function is encapsulated in a coroutine launched inside the ViewModel scope.
     *
     * @param intent: The intent of type `INTENT` that will be passed to `intentFlow`.
     */

    fun acceptIntent(intent: INTENT) {
        viewModelScope.launch {
            intentFlow.emit(intent)
        }
    }

    /**
     * `mapIntents()` is an abstract function in the BaseViewModel abstract class,
     * responsible for transforming a flow of user's `INTENT` into a flow of `PARTIAL_UI_STATE`.
     * Implementations of this method define the transformation from user interactions into state updates.
     *
     * @param intent represents the user's `INTENT` that needs to be converted to a partial state update.
     *
     * @return This function returns a `Flow<PARTIAL_UI_STATE>`, which represents the partial state updates resulting from the user's intent.
     */
    protected abstract fun mapIntents(intent: INTENT): Flow<PARTIAL_UI_STATE>

    /**
     * `reduceUiState()` is an abstract function which serves as a part in the unidirectional data flow in this architecture.
     * This function is responsible for the processing and mutation of UI states in the BaseViewModel abstract class.
     *
     * @param previousState is an instance of `UI_STATE` representing the previous UI state.
     * @param partialState is an instance of `PARTIAL_UI_STATE` representing the intermediate state which includes the partial changes to be applied to the previous state.
     *
     * @return This function returns an instance of `UI_STATE` which is the new state created after applying intended reductions to the previous state.
     **/
    protected abstract fun reduceUiState(
        previousState: UI_STATE,
        partialState: PARTIAL_UI_STATE
    ): UI_STATE

    /**
     * `sendEffect()` is a function encapsulated in a coroutine. It's a part of the BaseViewModel abstract class.
     * It sends an `EFFECT` object to the `effectChannel`.
     *
     * @param event is the `EFFECT` object that needs to sent to the `effectChannel`.
     *
     * This function launches a coroutine inside the ViewModel scope, and sends the `EFFECT` object inside this coroutine.
     * This transmission to the conflated channel is asynchronous and state-independent, used to send UI updates to the view layer.
     */
    protected fun sendEffect(event: EFFECT) {
        viewModelScope.launch {
            effectChannel.send(event)
        }
    }
}
