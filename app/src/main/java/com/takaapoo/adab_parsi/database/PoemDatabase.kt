package com.takaapoo.adab_parsi.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.takaapoo.adab_parsi.home.CategoryDao


@Database(
    entities = [Category::class , Poem::class, Poet::class, Verse::class, VerseFts::class],
    version = 1,
    exportSchema = true
//    autoMigrations = [
//        AutoMigration (from = 1, to = 2)
//    ]
)
abstract class PoemDatabase : RoomDatabase() {

    abstract fun dao(): Dao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: PoemDatabase? = null

        fun getDatabase(context: Context): PoemDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext, PoemDatabase::class.java,
                        "Poetry_Database").build()
                INSTANCE = instance
                instance
            }
        }

        fun getTestDatabase(context: Context): PoemDatabase {
            return Room.inMemoryDatabaseBuilder(
                    context.applicationContext, PoemDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }

}