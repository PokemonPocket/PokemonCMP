package com.tcgptrading.tcgptrading.app.data

import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import com.tcgptrading.tcgptrading.onApplicationStartPlatformSpecific

object AppInitializer {
    fun onApplicationStart() {
        onApplicationStartPlatformSpecific()
        GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = "304535122874-dlavetui8dbvf1g9c4g81cm5b8ejpvut.apps.googleusercontent.com"))
    }
}