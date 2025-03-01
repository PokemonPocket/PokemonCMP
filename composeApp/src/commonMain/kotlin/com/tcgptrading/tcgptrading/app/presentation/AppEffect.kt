package com.tcgptrading.tcgptrading.app.presentation

import com.tcgptrading.tcgptrading.app.domain.Screen
import com.tcgptrading.tcgptrading.app.presentation.model.SnackbarUiModel

/**
 * `AppEffect` is a sealed interface used to define the effects within the application.
 *
 * It acts as a parent for different named effect implementations that model and specify different types of application effects.
 * The effects may include operations such as displaying a Snackbar message or navigation between different screens in the application.
 *
 * This design allows for an exhaustive use in `when` expressions. It makes the architecture more expressive and robust to future additions or changes.
 *
 * Currently, it includes:
 * - `ShowSnackbar`: An effect to display a Snackbar message.
 * - `NavigateToScreen`: An effect to navigate to a certain screen.
 *
 * Note: There is an ongoing plan to refactor `NavigateToScreen` to accept String and resource identifiers. This could help in feature decoupling.
 */
sealed interface AppEffect {

    data class ShowSnackbar(val snackbarUiModel: SnackbarUiModel) : AppEffect

    //TODO change to accept String + resources in order for one feature to not know about the other
    data class NavigateToScreen(val route: Screen) : AppEffect

}
