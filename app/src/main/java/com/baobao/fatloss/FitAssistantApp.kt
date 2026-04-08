package com.baobao.fatloss

import android.app.Application
import com.baobao.fatloss.di.AppContainer

class FitAssistantApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
