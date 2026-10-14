package br.com.helpdev.lapscounter

import android.app.Application
import io.realm.Realm

class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initRealm()
    }

    private fun initRealm() {
        Realm.init(this)
        Realm.setDefaultConfiguration(AppApplicationRealmConfiguration.create(this))
    }
}
