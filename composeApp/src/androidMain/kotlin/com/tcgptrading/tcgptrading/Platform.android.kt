package com.tcgptrading.tcgptrading

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

actual fun onApplicationStartPlatformSpecific() {
    /* Do nothing */
}

fun initNapier() {
    Napier.base(DebugAntilog())
}