package com.lezzwatch.app

import android.app.Application
import com.lezzwatch.app.di.AppContainer

class LezzwatchApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
