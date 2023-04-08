package com.takaapoo.adab_parsi.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.database.Poet
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class HomeRepository @Inject constructor(
    @ApplicationContext val context: Context,
    val dao: Dao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allCat: LiveData<List<Category>> = dao.getAllCat()
    val allPoet: LiveData<List<Poet>> = dao.getAllPoet()


    fun deletePoet(poetID: List<Int>){
//        withContext(Dispatchers.IO){
//            dao.run {
//                deleteVerse(poetID)
//                deletePoet(poetID)
//                deletePoem(poetID)
//                deleteCat(poetID)
//                vacuum(SimpleSQLiteQuery("VACUUM"))
//            }
//        }
        val deletePoetRequest = OneTimeWorkRequestBuilder<DeletePoetWorker>()
            .setInputData(workDataOf("POET_ID" to poetID.toIntArray()))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueue(deletePoetRequest)
    }

}