package com.takaapoo.adab_parsi.search

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.takaapoo.adab_parsi.database.*


@Database(entities = [RecentSearch::class], version = 1, exportSchema = false)
abstract class RecentSearchDatabase : RoomDatabase() {

    abstract fun dao(): RecentSearchDao

    companion object {
        @Volatile
        private var INSTANCE: RecentSearchDatabase? = null

        fun getDatabase(context: Context): RecentSearchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, RecentSearchDatabase::class.java,
                    "Recent_Search_Database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}