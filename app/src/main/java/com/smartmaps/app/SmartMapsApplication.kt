package com.smartmaps.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class SmartMapsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize MapLibre SDK
        MapLibre.getInstance(this)
    }
}
