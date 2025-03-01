package com.tcgptrading.tcgptrading.core.util

import io.github.aakira.napier.Napier
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import org.koin.core.logger.KOIN_TAG

class CommonLogger(level: Level = Level.INFO) : Logger(level) {

    override fun display(level: Level, msg: MESSAGE) {
        when (level) {
            Level.DEBUG -> Napier.d(tag = KOIN_TAG, message = msg)
            Level.INFO -> Napier.i(tag = KOIN_TAG, message = msg)
            Level.WARNING -> Napier.w(tag = KOIN_TAG, message = msg)
            Level.ERROR -> Napier.e(tag = KOIN_TAG, message = msg)
            else -> Napier.e(tag = KOIN_TAG, message = msg)
        }
    }
}
