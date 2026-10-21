package com.cardforge.studio

import android.app.Application
import com.cardforge.studio.di.AppContainer

class CardForgeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
