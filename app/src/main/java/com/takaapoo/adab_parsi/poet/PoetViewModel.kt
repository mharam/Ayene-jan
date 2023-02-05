package com.takaapoo.adab_parsi.poet

import android.graphics.Bitmap
import android.os.Bundle
import androidx.lifecycle.*
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.util.allSubCategories
import com.takaapoo.adab_parsi.util.allUpCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoetViewModel @Inject constructor(
    private val dao: Dao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var bookPosition = 0
    var count = 0
    var bookListItems: List<Content?> = emptyList()

    var poemListItems = mutableMapOf<Int, List<Content>>()    //emptyList<Content>()
    var poemListSubItems = mutableMapOf<Int, List<Content>>()

    var enterBookFragment = false
    var bookShelfSpanCount = 2
    var poetViewAspectRatio = 0f
    var poetLibContentShot: Bitmap? = null

    init {
        savedStateHandle.get<Bundle?>("poet_state")?.let {
            bookPosition = it.getInt("book_position")
            count = it.getInt("book_count")
            bookListItems = (it.getParcelableArray("booklist_items")?.toList() ?: emptyList()) as List<Content?>
        }
        savedStateHandle.setSavedStateProvider("poet_state"){
            Bundle().apply {
                putInt("book_position", bookPosition)
                putInt("book_count", count)
                putParcelableArray("booklist_items", bookListItems.toTypedArray())
            }
        }
    }

    private val _uiEvent = Channel<PoetEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    fun reportEvent(event: PoetEvent){
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    fun getPoemWithCatID(catId: Int) = dao.getAllPoemWithCatID(catId)
    private suspend fun getAllPoemWithCatID(catId: Int) = dao.getAllPoemWithCatID(allSubCategories(catId))
    fun getPoet(id: Int) = dao.getPoet(id)
    fun updatePoetDate(newDate: Long, id: Int) = viewModelScope.launch {
        dao.updatePoetDate(newDate, id)
    }

    suspend fun sortedPoemItems(contentItem: Content = bookListItems[bookPosition]!!) =
        viewModelScope.async(Dispatchers.IO) {
            val sortedItems = sortContent(getAllPoemWithCatID(contentItem.id), contentItem.id)
            poemListItems[contentItem.id] =
                if (contentItem.parentID == 0)
                    sortedItems.filter { item ->
                        item.parentID == contentItem.id && item.rowOrder == 1 }
                else
                    sortedItems.filter { item -> item.rowOrder == 1 }
            return@async sortedItems
        }.await()

    private fun sortContent(input: List<Content>, contentItemId: Int): List<Content>{
        val outList = input.filter { elem -> elem.rowOrder == 2 }.toMutableList()
        var upCats = outList.map { allUpCategories(it.id).toMutableList() }
        val maxUpCatsLength = upCats.map { it.size }.maxOrNull() ?: 0
        upCats = upCats.map {
            it.addAll(0, MutableList(maxUpCatsLength - it.size){0})
            it
        }
        val mapUpCats = mutableMapOf(*Array(upCats.size){i -> Pair(outList[i], upCats[i])})

        for (i in 0 until maxUpCatsLength){
            outList.sortBy { mapUpCats[it]?.get(i) }
        }
        outList.addAll(0, input.filter { it.parentID == contentItemId && it.rowOrder == 1 })

        outList.filter { elem -> elem.rowOrder == 2 }.forEach { item ->
            val subItems = input.filter { elem -> elem.parentID == item.id && elem.rowOrder == 1 }
            outList.addAll(outList.indexOfFirst { elem -> elem.id == item.id } + 1, subItems)
        }
        return outList
    }
}