package com.takaapoo.adab_parsi

import android.app.Application
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.takaapoo.adab_parsi.util.PreferenceRepository
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


@HiltAndroidApp
class AyeneJanApplication: Application() {

//    val dao: Dao by lazy { PoemDatabase.getDatabase(this).dao() }
//    val dao: Dao
//        get() = ServiceLocator.provideDao(this)

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        PreferenceRepository(PreferenceManager.getDefaultSharedPreferences(this))
    }

    override fun onTrimMemory(level: Int) {
        Glide.with(applicationContext).onTrimMemory(TRIM_MEMORY_MODERATE)
        super.onTrimMemory(level)
    }
}