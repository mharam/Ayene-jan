package com.takaapoo.adab_parsi.add

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.takaapoo.adab_parsi.database.Poem
import com.takaapoo.adab_parsi.database.Poet
import com.takaapoo.adab_parsi.database.TempCategory
import com.takaapoo.adab_parsi.database.TempVerse
import com.takaapoo.adab_parsi.util.FileIO

@Database(entities = [TempCategory::class , Poem::class, Poet::class, TempVerse::class],
    version = 1, exportSchema = false)
abstract class TempDatabase : RoomDatabase() {

    abstract fun dao(): TempDao

    companion object {
//        @Volatile
//        private var INSTANCE: TempDatabase? = null

        fun getDatabase(context: Context, filename: String, createFromAsset: Boolean): TempDatabase {
            val file = FileIO(context)
            val builder = Room.databaseBuilder(
                context.applicationContext, TempDatabase::class.java,
                "temporary_Database")

            return /*INSTANCE ?:*/ synchronized(this) {
                val instance = (if (createFromAsset)
                    builder.createFromAsset("database/hafez.db") else
                        builder.createFromFile(file.openFile(filename)))
                    .fallbackToDestructiveMigration()
                    .build()
//                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

