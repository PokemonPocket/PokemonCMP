package com.tcgptrading.tcgptrading

import android.app.Application
import com.tcgptrading.tcgptrading.app.data.AppInitializer

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        AppInitializer.onApplicationStart()
        initNapier()
    }
}