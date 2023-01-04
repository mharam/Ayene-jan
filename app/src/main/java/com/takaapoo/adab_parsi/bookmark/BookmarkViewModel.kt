package com.takaapoo.adab_parsi.bookmark

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.BookmarkContent
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.util.allCategory
import com.takaapoo.adab_parsi.util.allSubCategories
import com.takaapoo.adab_parsi.util.allUpCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import javax.inject.Inject

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    application: Application,
    private val dao: Dao,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    var allBookmarks = mutableListOf<BookmarkContent>()
    var bookmarkCount = 0

    val startMargin = application.resources.getDimensionPixelSize(R.dimen.search_result_item_margin)
    val endMargin = application.resources.getDimensionPixelSize(R.dimen.search_result_item_margin)
    val verseSeparation = application.resources.getDimensionPixelSize(R.dimen.verse_separation)
    val versePadding = application.resources.getDimensionPixelSize(R.dimen.verse_padding)
    var rootWidth = 0
    var mesraContainerWidth = 0

    var poemPosition = 0
    var poemCount = 0
    var poemList = emptyList<Content>()
    var selectedItemCatID = 0

    var scroll = 0
    var bookmarkHeight = 0
    var lastItem = 0
    var firstItem = 0

    var bookmarkListDisplace = 0
    var bookmarkListAddedScroll = 0
    var selectedBookmarkItem: BookmarkContent? = null

    init {
        savedStateHandle.get<Bundle?>("bookmark_state")?.let {
            selectedItemCatID = it.getInt("selected_item_cat_id")
            poemPosition = it.getInt("poem_position")
        }
        savedStateHandle.setSavedStateProvider("bookmark_state"){
            Bundle().apply {
                putInt("selected_item_cat_id", selectedItemCatID)
                putInt("poem_position", poemPosition)
            }
        }
    }



    fun getAllBookmark() = dao.getAllBookmark()
    fun bookmarkCount() = dao.bookmarkCount()
    suspend fun getPoemWithCatID(catId: Int?) = viewModelScope.async(Dispatchers.IO){
        val upCatList = allUpCategories(catId)
        val myPoemList = dao.getPoemWithCatID(
            if (upCatList.size == 1) upCatList
            else allSubCategories(upCatList[upCatList.size - 2])
        )
        poemList = myPoemList
        poemCount = myPoemList.size
        return@async myPoemList
    }.await()


    fun bookmarkAddress(item: BookmarkContent): String {
        val catText = allUpCategories(item.poemm.catID).map {
                elem -> allCategory.find { it.id == elem }?.text }.reversed()
        var address = ""

        catText.getOrNull(0)?.let { address += "${it.substringBefore('*')}، " }
        if (catText.size > 1){
            catText.subList(1, catText.size).forEach { elem -> address += "${elem}، " }
        }
        address += item.poemm.title
        return address
    }


}