package io.github.teouk.fomo

import android.app.Application

/**
 * Created by Android before any screen, and lives as long as the app process.
 * That makes it the right owner for [AppContainer].
 */
class FomoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
