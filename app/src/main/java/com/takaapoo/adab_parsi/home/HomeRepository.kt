package com.takaapoo.adab_parsi.home

import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.database.Poet


class HomeRepository(val dao: Dao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allCat: LiveData<List<Category>> = dao.getAllCat()
    val allPoet: LiveData<List<Poet>> = dao.getAllPoet()


    suspend fun deletePoet(poetID: List<Int>){
//        withContext(Dispatchers.IO){
            dao.run {
                deleteVerse(poetID)
                deletePoet(poetID)
                deletePoem(poetID)
                deleteCat(poetID)
                vacuum(SimpleSQLiteQuery("VACUUM"))
            }
//        }
    }



//    fun vacuum() = dao.vacuum(SimpleSQLiteQuery("VACUUM"))


}