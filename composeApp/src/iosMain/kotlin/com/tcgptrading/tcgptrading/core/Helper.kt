package com.tcgptrading.tcgptrading.core

import com.tcgptrading.tcgptrading.app.data.appModule
import com.tcgptrading.tcgptrading.core.util.CommonLogger
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

fun initKoin() {
    startKoin {
        modules(appModule)
        logger(CommonLogger(level = Level.DEBUG))
        createEagerInstances()
    }
}

fun debugBuild() {
    Napier.base(DebugAntilog())
}

//fun initNotifierManager() {
//    NotifierManager.initialize(
//        configuration =
//            NotificationPlatformConfiguration.Ios(
//                showPushNotification = true,
//                askNotificationPermissionOnStart = false
//            )
//    )
//}
//
//fun onApplicationDidReceiveRemoteNotification(userInfo: Map<Any?, *>) {
//    NotifierManager.onApplicationDidReceiveRemoteNotification(userInfo)
//}