package com.pocketpal

import android.app.Application
import com.pocketpal.data.local.AppDatabase

class PocketPalApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PocketPalApplication
            private set
    }
}
