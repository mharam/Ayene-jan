package com.takaapoo.adab_parsi.add

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.util.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.*
import java.util.concurrent.CancellationException
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration.Companion.milliseconds


private const val minFileLength = 4000
const val INSTALLING = "Installing"
const val PROGRESS = "Progress"
const val Error = "ERROR"
const val imagePrefix = "image"
const val thumbnailPrefix = "thumbnail"
const val LargeIconSize = 128

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val poemDao: Dao
) : CoroutineWorker(context, workerParams) {

    private var filesSize = 0
    private var total: Long = 0
    private val fileCounts = 3
    private lateinit var filename: List<String>
    val file = FileIO(context)
    val poetID = inputData.getInt("PoetID", -1)
    private val poetName = inputData.getString("PoetName")
    private val thumbnailURL = inputData.getString("ThumbnailURL")
    private val ancient = inputData.getInt("Ancient", 0)
    private var largeIconBitmap: Bitmap? = GlideApp.with(context)
        .asBitmap()
        .load(if (thumbnailURL.isNullOrEmpty()) {
                if (ancient == 0) R.drawable.tomb else R.drawable.person
            } else thumbnailURL)
        .override(LargeIconSize)
        .transform(RoundedCorners(LargeIconSize/9))
        .apply(
            RequestOptions().error( if (ancient == 0) R.drawable.tomb else R.drawable.person )
        )
        .submit()
        .get()

    private suspend fun initialize(){
        filesSize = 0
        total = 0
        setProgress(workDataOf(INSTALLING to false, PROGRESS to -1))
    }


    override suspend fun doWork(): Result = withContext(Dispatchers.IO){
        try {
            val largeImageURL = inputData.getString("LargeImageURL")
            val databaseURL = inputData.getString("DatabaseURL") ?: throw Exception("database url not provided!")
            filename = mutableListOf(imagePrefix, thumbnailPrefix, "").map { it + "$poetID" }

            initialize()
            delay(300.milliseconds)
            if (!isNetworkConnected()) {
                throw Exception(context.getString(R.string.connection_failed))
            } else {
                startDownload(largeImageURL, thumbnailURL, databaseURL) ?:
                throw Exception(context.getString(R.string.install_failed))
            }
            Result.success()
        } catch (e: Exception){
            if (e is CancellationException)
                cleanUp()

            setProgress(workDataOf(INSTALLING to false, PROGRESS to -3))
            Result.failure(workDataOf(Error to e.message))
        }
    }


    @Throws(Exception::class)
    suspend fun startDownload(largeImageURL: String?,
                    thumbnailURL: String?,
                    databaseURL: String): String?{

        val url = MutableList<URL?>(fileCounts){null}
        val fileLength = MutableList(fileCounts){-1}
        val connection = MutableList<HttpsURLConnection?>(fileCounts){null}
        val result = MutableList<String?>(fileCounts){null}

        return withContext(Dispatchers.IO) {
            val imageFile = file.openFile(filename[0])
            val thumbnailFile = file.openFile(filename[1])
            val databaseFile = file.openFile(filename[2])
            val outStream = filename.map { file.writeFile(it) }

            try {
                url[0] = if (!largeImageURL.isNullOrEmpty()) URL(largeImageURL) else null
                url[1] = if (!thumbnailURL.isNullOrEmpty()) URL(thumbnailURL) else null
                url[2] = URL(databaseURL)
                for (i in url.indices){
                    connection[i] = url[i]?.openConnection() as? HttpsURLConnection
                    connection[i]?.run {
                        readTimeout = 20000
                        connectTimeout = 20000
                        requestMethod = "GET"
                        doInput = true
                        connect()
                        val lengthKey = headerFields.keys.find {
                            it?.lowercase(Locale.US)?.contains("content-length") == true
                        }
                        fileLength[i] = getHeaderFieldInt(lengthKey, -1)
                        filesSize += fileLength[i]

                        if (responseCode != HttpsURLConnection.HTTP_OK) {
                            throw IOException("HTTP error code: $responseCode")
                        }
                    }
                }
                for (i in url.indices){
                    connection[i]?.run {
                        result[i] = inputStream?.let { stream ->
                            when {
                                fileLength[i] > 0 -> saveFile(stream, outStream[i], fileLength[i])
                                else -> null
                            }
                        }
                    }
                    if (url[i] == null) {
                        file.openFile(filename[i]).delete()
                        result[i] = "Success"
                    }

                    if (connection[i] != null && result[i] == null) break
                }

                result[2]?.let {
                    if (it == "Success"){
                        delay(800.milliseconds)
                        setProgress(workDataOf(INSTALLING to true, PROGRESS to 100))
                        val tempDatabase = TempDatabase.getDatabase(context, filename[2], false)
                        val tempDao = tempDatabase.dao()
                        poemDao.insertDatabase(
                            tempCatToCat(tempDao.getAllCat()),
                            tempDao.getAllPoem(),
                            tempDao.getAllPoet(),
                            getAllVerseBiErab(tempDao.getAllVerse())
                        )
                        poemDao.vacuum(SimpleSQLiteQuery("VACUUM"))
                        tempDatabase.close()
                        deleteDatabaseFile("temporary_Database")
                        delay(800.milliseconds)
                        setProgress(workDataOf(INSTALLING to false, PROGRESS to -2))
                    }
                }

                if (!result.contains(null))
                    "success"
                else {
                    imageFile.delete()
                    thumbnailFile.delete()
                    null
                }
            } catch (e: Exception) {
                imageFile.delete()
                thumbnailFile.delete()
                throw e
            } finally {
                // Close Stream and disconnect HTTPS connection.
                try {
                    databaseFile.delete()
                    for (i in url.indices){
                        connection[i]?.inputStream?.close()
                        outStream[i].close()
                        connection[i]?.disconnect()
                    }
                } catch (_: Exception){}
            }
        }
    }

    private suspend fun saveFile(inStream: InputStream, outStream: OutputStream?, fileLen: Int): String?{
        val data = ByteArray(4*1024)
        var count: Int
        var progress: Int

        while (inStream.read(data).also{ count = it } != -1) {
            total += count.toLong()
            progress = (total * 100f / filesSize).toInt()
            setProgress(workDataOf(PROGRESS to progress))
            outStream?.write(data, 0, count)
        }

        if (total < fileLen || fileLen < minFileLength) {
            inStream.close()
            return null
        }
        return "Success"
    }

    private fun cleanUp(){
        file.openFile(filename[0]).delete()
        file.openFile(filename[1]).delete()
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) &&
                if (Build.VERSION.SDK_INT >= 28)
                    actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                else true
    }

    private fun deleteDatabaseFile(databaseName: String) {
        val databases = File(context.applicationInfo.dataDir + "/databases")
        try {
            File(databases, databaseName).delete()
            File(databases, "$databaseName-shm").delete()
            File(databases, "$databaseName-wal").delete()
        } catch (_: Exception){ }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = context.getString(R.string.add_poet_notification_content_text, poetName)

        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(largeIconBitmap)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(poetID, notification)
    }

}