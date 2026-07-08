package br.com.sos.osmobile

import android.app.Application
import br.com.sos.osmobile.core.di.AppContainer

class OSMobileApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
