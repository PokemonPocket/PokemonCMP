package com.tcgptrading.tcgptrading.app.presentation

import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.app.presentation.model.SnackbarUiModel

/**
 * The `AppIntent` sealed interface represents a set of actions within the application.
 * This sealed interface includes various implementations that represent different types of intents or actions.
 * These can range from showing a Snackbar to navigating to a different screen or showing different UI elements.
 *
 * Implementations of this interface include:
 * - `ShowSnackbar`: Shows a Snackbar with the associated UI model data
 * - `NavigateToScreen`: Navigates to the specified screen in the app
 * - `ShowAddEventBottomSheet`: Displays an add event bottom sheet
 * - `ShowBottomNavigation`: Controls the visibility of the Bottom Navigation
 * - `ShowFloatingActionButton`: Controls the visibility of the Floating Action Button
 * - `ShowBottomNavigationAndFloatingAb`: Controls the visibility of both the Bottom Navigation and Floating Action Button simultaneously
 *
 * Each implementation of `AppIntent` can be used to represent specific actions that the application can perform.
 */
sealed interface AppIntent {
    data class ChangeUserLogInState(val loggedIn: Boolean, val token: String?) : AppIntent
    data class ShowSnackbar(val snackbarUiModel: SnackbarUiModel) : AppIntent
    data class NavigateToScreen(val route: Screen) : AppIntent
    data class ChangeUserIdToken(val token: String?) : AppIntent
    data object LogoutUser: AppIntent
}
