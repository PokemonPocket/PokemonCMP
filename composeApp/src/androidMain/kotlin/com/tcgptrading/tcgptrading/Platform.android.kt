package com.tcgptrading.tcgptrading

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

actual fun onApplicationStartPlatformSpecific() {
    /* Do nothing */
}

fun initNapier() {
    Napier.base(DebugAntilog())
}

@Composable
actual fun localContext(): Any? = LocalActivity.current
actual val adBannerId: String = "ca-app-pub-8505602361918016/7688948014"
