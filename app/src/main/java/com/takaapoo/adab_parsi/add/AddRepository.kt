package com.takaapoo.adab_parsi.add

import android.content.Context
import androidx.work.*
import com.takaapoo.adab_parsi.network.PoetProperty
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class AddRepository @Inject constructor(@ApplicationContext val context: Context) {

    fun downloadPoetOrCancel(poetItem: PoetProperty){
        val workManager = WorkManager.getInstance(context)
        val listOfWorkInfo = workManager.getWorkInfosForUniqueWork(poetItem.poetID.toString()).get()

        if (listOfWorkInfo.firstOrNull()?.state?.isFinished != false){
            val downloadPoetRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf("LargeImageURL" to poetItem.largeimageURL,
                    "ThumbnailURL" to poetItem.thumbnailURL,
                    "DatabaseURL" to poetItem.databaseURL,
                    "PoetID" to poetItem.poetID,
                    "PoetName" to poetItem.text.substringBefore('*'),
                    "Ancient" to poetItem.ancient)
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            workManager.enqueueUniqueWork(poetItem.poetID.toString(), ExistingWorkPolicy.REPLACE, downloadPoetRequest)
        } else {
            workManager.cancelUniqueWork(poetItem.poetID.toString())
        }
    }
}