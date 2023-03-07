package com.takaapoo.adab_parsi.home

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.takaapoo.adab_parsi.database.Dao
import kotlinx.coroutines.*
import javax.inject.Inject


class DeletePoetWorker @Inject constructor(
    context: Context,
    workerParams: WorkerParameters,
    private val dao: Dao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val poetID = inputData.getIntArray("POET_ID")?.toList() ?: return@withContext Result.failure()
        try {
            dao.run {
                deleteVerse(poetID)
                deletePoet(poetID)
                deletePoem(poetID)
                deleteCat(poetID)
                vacuum(SimpleSQLiteQuery("VACUUM"))
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {


        return super.getForegroundInfo()
    }



}