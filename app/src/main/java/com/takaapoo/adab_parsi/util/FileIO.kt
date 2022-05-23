package com.takaapoo.adab_parsi.util

import android.content.Context
import java.io.*

class FileIO (private val context: Context) {

    @Throws(IOException::class)
    fun readFile(fileName: String): InputStream {
        return FileInputStream("${context.filesDir}/$fileName")
    }

    @Throws(IOException::class)
    fun writeFile(fileName: String): OutputStream {
        return FileOutputStream("${context.filesDir}/$fileName")
    }

    @Throws(IOException::class)
    fun openFile(child: String): File {
        return File(context.filesDir, child)
    }

}