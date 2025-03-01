package com.tcgptrading.tcgptrading.app.domain

import kotlinx.serialization.Serializable
/**
 * `bottomNavItems` is a list of Triple objects which is used to define the items present in the bottom navigation bar of the application.
 * Each Trio represents a bottom navigation item composed of:
 * - A string representing the display name of the item
 * - An Icon object, determining the visual representation of the item in the navigation bar.
 */

@Serializable
sealed interface Screen {

    @Serializable
    data object Login : Screen

    @Serializable
    data object Splash : Screen

    @Serializable
    data object Main : Screen
}
