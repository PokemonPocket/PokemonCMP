package com.tcgptrading.tcgptrading

import android.app.Application

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        initNapier()
    }
}