package com.tcgptrading.tcgptrading.core.data

import com.russhwolf.settings.Settings

/** Small on-device flags. */
class AppPrefs(private val settings: Settings = Settings()) {

    /** The first-time tour was finished or skipped on this device. */
    var tutorialDone: Boolean
        get() = settings.getBoolean(KEY_TUTORIAL, false)
        set(value) = settings.putBoolean(KEY_TUTORIAL, value)

    private companion object {
        const val KEY_TUTORIAL = "tutorial_done"
    }
}
