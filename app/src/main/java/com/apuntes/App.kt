package com.apuntes

import android.app.Application
import com.apuntes.shared.TournamentStorage
import com.google.android.gms.ads.MobileAds

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TournamentStorage.init(this)
    }
}

