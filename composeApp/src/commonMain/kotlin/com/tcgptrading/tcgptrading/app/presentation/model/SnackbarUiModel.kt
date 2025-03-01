package com.tcgptrading.tcgptrading.app.presentation.model

import androidx.compose.material3.SnackbarDuration

/**
 * This is a data model class `SnackbarUiModel` which represents the UI State of a Snackbar in a Compose Material3 UI.
 * It accepts:
 *  - `message`: The main text content of the Snackbar.
 *  - `actionLabel`: An optional text label for the action button of the Snackbar. It could be null, in which case no action button would be shown.
 *  - `withDismissAction`: Boolean flag to control whether the Snackbar should have a dismiss button. Defaults to false.
 *  - `duration`: Controls the duration for which the Snackbar is visible. It's of type `SnackbarDuration` enum.
 *      - If `actionLabel` is null, the Snackbar is visible for a short duration.
 *      - If `actionLabel` is not null, the Snackbar is visible indefinitely until a user action.
 *  - `onDismiss`: An optional function which is invoked when the Snackbar is dismissed.
 *  - `onAction`: An optional function which is invoked when the action button on the Snackbar is clicked.
 *
 * Usage of this class aids in facilitating a consistent Snackbar interaction throughout the app.
 */
data class SnackbarUiModel(
    val message: String,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = false,
    val duration: SnackbarDuration =
        if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    val onDismiss: () -> Unit = {},
    val onAction: () -> Unit = {}
)
