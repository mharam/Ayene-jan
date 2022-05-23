package com.takaapoo.adab_parsi.search

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.takaapoo.adab_parsi.database.RecentSearch
import com.takaapoo.adab_parsi.database.SearchSuggest


@Dao
interface RecentSearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearch)

    @Query("SELECT * from recent_search WHERE text == :query")
    suspend fun getSearchByText(query: String): List<RecentSearch>

    @Query("UPDATE recent_search SET date = :newDate WHERE text == :query")
    suspend fun updateSearchByText(newDate: Long, query: String)

    suspend fun insertOrUpdate(search: RecentSearch) {
        val searchesFromDB = getSearchByText(search.text)
        if (searchesFromDB.isEmpty())
            insertSearch(search)
        else {
            updateSearchByText(search.date, search.text)
        }
    }

    @Query("""SELECT text, 1 AS isRecentSearch from recent_search WHERE text LIKE :query || '%' 
        OR text LIKE '% ' || :query || '%' ORDER By date DESC""")
    fun getHistorySuggest(query: String): LiveData<List<SearchSuggest>>

    @Query("DELETE from recent_search")
    suspend fun deleteSearchHistory()


}

