package com.tcgptrading.tcgptrading.app.data

import com.tcgptrading.tcgptrading.app.presentation.AppState
import com.tcgptrading.tcgptrading.app.presentation.AppViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * A list that holds all features modules to be included in the main application module (appModule). It includes:
 *  - dataStoreModule: Module responsible for data storage-related tasks
 *  - networkModule: Module responsible for network-related tasks
 *  - permissionsModule: Module responsible for permissions handling
 *  - onboardingModule: Module responsible for onboarding-related tasks
 *  - loginModule: Module responsible for login-related tasks
 *  - feedModule: Module responsible for feed-related tasks
 *  - splashModule: Module responsible for splash-related tasks
 *  - addPostModule: Module responsible for adding post-related tasks
 *
 * These modules are used to organise the app's functionalities into manageable and related sets of tasks.
 */
//val featuresModules = listOf(
//    splashModule,
//)

/**
 * The central module defining properties of the application (appModule).
 *
 * This module includes all necessary feature modules (featuresModules) to provide required functionalities across the app.
 *
 * Definitions in the current module include:
 * 1. A factory for creating AppViewModel instances, requesting necessary parameters from Koin.
 * 2. A singleton (single) defining AppState object, which contains the application's state.
 * 3. A singleton (single) defining SavedStateHolder object with provided constant parameters ("root" and null).
 *
 * Note: In the context of Koin DI framework, "factory" specifies a definition that should be created anew each time it is required,
 *       while "single" specifies a singleton definition, which is created once and shared.
 */
val appModule = module {
//    includes(featuresModules)
    viewModelOf(::AppViewModel)
    single { AppState() }
}
