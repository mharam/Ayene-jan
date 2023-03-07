package com.takaapoo.adab_parsi.home

import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.workDataOf
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.database.Poet


class HomeRepository(val dao: Dao) {

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
        val deletePoetWorker = OneTimeWorkRequestBuilder<DeletePoetWorker>()
            .setInputData(workDataOf("POET_ID" to poetID))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }



//    fun vacuum() = dao.vacuum(SimpleSQLiteQuery("VACUUM"))


}