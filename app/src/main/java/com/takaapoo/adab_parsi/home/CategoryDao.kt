package com.takaapoo.adab_parsi.home

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.takaapoo.adab_parsi.database.Category

@Dao
interface CategoryDao {
    @Query("SELECT * from cat WHERE parent_id == 0")
    suspend fun getCatPoet(): List<Category>

    @Query("SELECT * from cat ORDER BY text")
    fun getAllCat(): LiveData<List<Category>>

    @Query("SELECT * from cat ORDER BY text")
    suspend fun getAllCatSuspend(): List<Category>

    @Query("UPDATE cat SET last_open_date = :newDate WHERE id == :id")
    suspend fun updatePoetDate(newDate: Long, id: Int)

    @Query("DELETE from cat WHERE poet_id IN (:poetID)")
    suspend fun deleteCat(poetID: List<Int>)

    @Query("DELETE from cat")
    suspend fun deleteAllCat()
}