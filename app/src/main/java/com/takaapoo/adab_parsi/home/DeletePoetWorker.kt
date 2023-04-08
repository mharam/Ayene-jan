package com.takaapoo.adab_parsi.home

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.add.imagePrefix
import com.takaapoo.adab_parsi.add.thumbnailPrefix
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.util.FileIO
import com.takaapoo.adab_parsi.util.NotificationChannelManager
import com.takaapoo.adab_parsi.util.engNumToFarsiNum
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException


@HiltWorker
class DeletePoetWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: Dao
) : CoroutineWorker(context, workerParams) {

    val poetID = inputData.getIntArray("POET_ID")?.toList()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        poetID ?: return@withContext Result.failure()
        try {
//            setForeground(getForegroundInfo())
            val catID = dao.getAllCatWithPoetId(poetID)
            dao.run {
                deleteCat(poetID)
                deletePoet(poetID)
                deleteVerse(catID)
                deletePoem(catID)
                vacuum(SimpleSQLiteQuery("VACUUM"))
            }
            val file = FileIO(context)
            try {
                poetID.forEach {
                    file.openFile(imagePrefix + "$it").delete()
                    file.openFile(thumbnailPrefix + "$it").delete()
                }
            } catch (_: IOException) { }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val poetCount = poetID?.size ?: 0
        val title = context.getString(
            R.string.delete_poet_notification_content_text,
            engNumToFarsiNum(poetCount)
        )

        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.mipmap.app_icon2)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(poetID?.first() ?: 0, notification)
    }

}