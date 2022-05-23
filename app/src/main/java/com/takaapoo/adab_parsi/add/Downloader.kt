package com.takaapoo.adab_parsi.add

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.*
import com.takaapoo.adab_parsi.network.PoetProperty
import com.takaapoo.adab_parsi.util.FileIO
import com.takaapoo.adab_parsi.util.tempCatToCat
import com.takaapoo.adab_parsi.util.getAllVerseBiErab
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val minFileLength = 4000
const val imagePrefix = "image"
const val thumbnailPrefix = "thumbnail"

class Downloader(private val vm: AddViewModel, private val context: Context,
                 private val poetItem: PoetProperty){

    private var cancel = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var filesSize = 0
    private var total: Long = 0
    private val fileCounts = 3


    init {
        vm.progress[poetItem.poetID] = vm.progress[poetItem.poetID] ?: MutableLiveData(-2)
        vm.installing[poetItem.poetID] = vm.installing[poetItem.poetID] ?: MutableLiveData(false)
    }

    private fun initialize(){
        cancel = false
        filesSize = 0
        total = 0
        vm.installing[poetItem.poetID]?.value = false
    }

    fun download(){
        if (vm.progress[poetItem.poetID]?.value!! <= -2) {
            initialize()
            vm.progress[poetItem.poetID]?.value = -1

            if (!isNetworkConnected()) {
                vm.progress[poetItem.poetID]?.value = -3
//                Toast.makeText(context, R.string.connection_failed, Toast.LENGTH_SHORT).show()
                vm.setMess(R.string.connection_failed)
            } else
                coroutineScope.launch {
                    val result: String?
                    try {
                        result = get()
                        result ?: throw IOException("No response received.")
                    } catch (e: IllegalStateException){
                        Timber.i("mess = ${e.message}")
                        if (vm.progress[poetItem.poetID]?.value!! >= -1 ){
                            vm.progress[poetItem.poetID]?.value = -3
                            vm.setMess(R.string.install_failed)
                        }
                        vm.installing[poetItem.poetID]?.postValue(false)
                    }
                    catch (e: Exception) {
                        Timber.i("mess = ${e.message}")
                        if (vm.progress[poetItem.poetID]?.value!! >= -1 ){
                            vm.progress[poetItem.poetID]?.value = -3
                            vm.setMess(R.string.download_failed)
                        }
                        vm.installing[poetItem.poetID]?.postValue(false)
                    }
//                    if (result == null)
//                        vm.progress[poetItem.poetID]?.value = -3
                }
        }
        else {
            vm.progress[poetItem.poetID]?.value = -3
            cancel = true
        }
    }

    @Throws(Exception::class)
    suspend fun get(): String?{
        val file = FileIO(context)
        val url = MutableList<URL?>(fileCounts){null}
        val fileLength = MutableList(fileCounts){-1}
        val connection = MutableList<HttpsURLConnection?>(fileCounts){null}
        val result = MutableList<String?>(fileCounts){null}
        val filename = mutableListOf(imagePrefix, thumbnailPrefix, "").map { it + "${poetItem.poetID}" }
        val poemDao = PoemDatabase.getDatabase(context).dao()

        return withContext(Dispatchers.IO) {
            val imageFile = file.openFile(filename[0])
            val thumbnailFile = file.openFile(filename[1])
            val databaseFile = file.openFile(filename[2])
            val outStream = filename.map { file.writeFile(it) }
//            mutableListOf<OutputStream?>(file.writeFile(filename[0]), file.writeFile(filename[1]))

            try {
                url[0] = if (!poetItem.largeimageURL.isNullOrEmpty()) URL(poetItem.largeimageURL) else null
                url[1] = if (!poetItem.thumbnailURL.isNullOrEmpty()) URL(poetItem.thumbnailURL) else null
                url[2] = URL(poetItem.databaseURL)
                for (i in url.indices){
                    connection[i] = url[i]?.openConnection() as? HttpsURLConnection
                    connection[i]?.run {
                        readTimeout = 5000
                        connectTimeout = 5000
                        requestMethod = "GET"
                        doInput = true
                        connect()
                        val lengthKey = headerFields.keys.find { it?.contains("Content-Length") == true }
                        fileLength[i] = getHeaderFieldInt(lengthKey, -1)
                        filesSize += fileLength[i]

                        if (responseCode != HttpsURLConnection.HTTP_OK) {
                            Timber.i("responseCode: $responseCode")
                            throw IOException("HTTP error code: $responseCode")
                        }
                    }
                    if (cancel)
                        throw Exception("Cancelled")
                }
                for (i in url.indices){
                    connection[i]?.run {
                        result[i] = inputStream?.let { stream ->
                            when {
//                                contentLength > 0 -> saveFile(stream, outStream[i], contentLength)
                                fileLength[i] > 0 -> saveFile(stream, outStream[i], fileLength[i])
                                else -> null
                            }
                        }
                    }
                    if (url[i] == null) {
                        file.openFile(filename[0]).delete()
                        result[i] = "Success"
                    }

                    if (connection[i] != null && result[i] == null) break
//                    result[i] ?: break
                }

                result[2]?.let {
                    if (it == "Success"){
                        delay(500)
                        vm.installing[poetItem.poetID]?.postValue(true)
                        val tempDao = TempDatabase.getDatabase(context, filename[2], false).dao()
                        poemDao.insertDatabase(
                            tempCatToCat(tempDao.getAllCat()),
                            tempDao.getAllPoem(),
                            tempDao.getAllPoet(),
                            getAllVerseBiErab(tempDao.getAllVerse())
                        )
                        poemDao.vacuum(SimpleSQLiteQuery("VACUUM"))
                        vm.modifyAllPoet(poetItem.poetID)
                        delay(800)
                        vm.progress[poetItem.poetID]?.postValue(-2)
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
                        outStream[i]?.close()
                        connection[i]?.disconnect()
                    }
                } catch (e: Exception){}
            }
        }
    }



    private fun saveFile(inStream: InputStream, outStream: OutputStream?, fileLen: Int): String?{
        val data = ByteArray(4*1024)
        var count: Int

        while (inStream.read(data).also{ count = it } != -1) {
            if (cancel) {
                vm.progress[poetItem.poetID]?.postValue(-3)
                return null
            }
            total += count.toLong()
            vm.progress[poetItem.poetID]?.postValue((total * 100f / filesSize).toInt())
            outStream?.write(data, 0, count)
        }

        if (total < fileLen || fileLen < minFileLength) {
            inStream.close()
            return null
        }
        return "Success"
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

//            result = when {
//                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
//                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
//                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
//                else -> false
//            }
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) &&
                if (Build.VERSION.SDK_INT >= 28)
                    actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                else true
    }

}
