package com.takaapoo.adab_parsi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import com.bumptech.glide.Glide
import com.takaapoo.adab_parsi.util.PreferenceRepository
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject


@HiltAndroidApp
class AyeneJanApplication: Application(), Configuration.Provider {

//    val dao: Dao by lazy { PoemDatabase.getDatabase(this).dao() }
//    val dao: Dao
//        get() = ServiceLocator.provideDao(this)

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        PreferenceRepository(PreferenceManager.getDefaultSharedPreferences(this))
    }

    override fun onTrimMemory(level: Int) {
        Glide.with(applicationContext).onTrimMemory(TRIM_MEMORY_MODERATE)
        super.onTrimMemory(level)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}