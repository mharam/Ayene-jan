package com.takaapoo.adab_parsi.add

import androidx.room.Dao
import androidx.room.Query
import com.takaapoo.adab_parsi.database.*

@Dao
interface TempDao {

    @Query("SELECT * from cat")
    suspend fun getAllCat(): List<TempCategory>

    @Query("SELECT * from poem")
    suspend fun getAllPoem(): List<Poem>

    @Query("SELECT * from poet")
    suspend fun getAllPoet(): List<Poet>

    @Query("SELECT * from verse")
    suspend fun getAllVerse(): List<TempVerse>

}