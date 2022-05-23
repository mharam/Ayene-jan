package com.takaapoo.adab_parsi.poet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.util.allSubCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoetViewModel @Inject constructor(private val dao: Dao) : ViewModel() {

//    val dao= PoemDatabase.getDatabase(application).dao()

    var bookPosition = 0
    var count = 0
    var bookListItems = emptyList<Content?>()

    var poemListItems = mutableMapOf<Int, List<Content>>()    //emptyList<Content>()
    var poemListSubItems = mutableMapOf<Int, List<Content>>()

    var enterBookFragment = false

    var bookShelfSpanCount = 2

    private val _showHelp = MutableLiveData<Int?>(null)
    val showHelp: LiveData<Int?>
        get() = _showHelp
    fun doShowHelp() { _showHelp.value = 1}
    fun doneShowHelp() { _showHelp.value = null}
    fun increaseShowHelp() { _showHelp.value = _showHelp.value?.plus(1) }





    fun getPoemWithCatID(catId: Int) = dao.getAllPoemWithCatID(catId)
    suspend fun getAllPoemWithCatID(catId: Int) = dao.getAllPoemWithCatID(allSubCategories(catId))
    fun getPoet(id: Int) = dao.getPoet(id)
    fun updatePoetDate(newDate: Long, id: Int) = viewModelScope.launch {
        dao.updatePoetDate(newDate, id)
    }


}