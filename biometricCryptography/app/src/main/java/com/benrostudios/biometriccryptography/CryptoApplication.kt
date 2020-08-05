package com.benrostudios.biometriccryptography

import android.app.Application
import com.chamber.kotlin.library.SharedChamber

class CryptoApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        SharedChamber.initChamber(this)
    }
}