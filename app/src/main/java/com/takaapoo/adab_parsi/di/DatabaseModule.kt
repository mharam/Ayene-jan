package com.takaapoo.adab_parsi.di

import android.content.Context
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.database.PoemDatabase
import com.takaapoo.adab_parsi.search.RecentSearchDao
import com.takaapoo.adab_parsi.search.RecentSearchDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    fun provideDao(poemDatabase: PoemDatabase): Dao{
        return poemDatabase.dao()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): PoemDatabase{
        return PoemDatabase.getDatabase(appContext)
    }

    @Provides
    fun provideRecentSearchDao(recentSearchDatabase: RecentSearchDatabase): RecentSearchDao{
        return recentSearchDatabase.dao()
    }

    @Provides
    @Singleton
    fun provideRecentSearchDatabase(@ApplicationContext appContext: Context): RecentSearchDatabase{
        return RecentSearchDatabase.getDatabase(appContext)
    }
}