package com.takaapoo.adab_parsi.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.*
import com.takaapoo.adab_parsi.util.allCategory
import com.takaapoo.adab_parsi.util.allSubCategories
import com.takaapoo.adab_parsi.util.allUpCategories
import com.takaapoo.adab_parsi.util.eventHandler.Event
import com.takaapoo.adab_parsi.util.makeTextBiErab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SearchViewModel @Inject constructor(application: Application, private val dao: RecentSearchDao,
                                          private val poemDao: Dao) : AndroidViewModel(application) {

//    val dao = RecentSearchDatabase.getDatabase(application).dao()
//    private val poemDao = PoemDatabase.getDatabase(application).dao()

    val searchQuery = MutableLiveData("")
    var searchSubmit = false

    var submittedSearch = ""
    var searchDomain = ""
    var poemID = -1
    var catID = -1

    var searchResultList = emptyList<SearchContent>()

    val startMargin = application.resources.getDimensionPixelSize(R.dimen.search_result_item_margin)
    val endMargin = application.resources.getDimensionPixelSize(R.dimen.search_result_item_margin)
//    val guidWidth = application.resources.getDimensionPixelSize(R.dimen.poem_item_guid_width)
    val verseSeparation = application.resources.getDimensionPixelSize(R.dimen.verse_separation)
    val versePadding = application.resources.getDimensionPixelSize(R.dimen.verse_padding)
    var mesraContainerWidth = 0
    var rootWidth = 0
//    var textHeight = 0
//    var spaceWidth = 0f
//    lateinit var paint: Paint

    var comeFromDetailFragment = false
    var scroll = 0
//    var resultHeight = 0
    var resultLastItem = 0
    var resultFirstItem = 0
    var resultListDisplace = 0
    var lastResultOpened = 0
    var bottomViewedResultHeight = 0
    var topViewedResultHeight = 0
    var detailPagerPosition = -1

    var openedFromDetailFrag = false

    var poemPosition = 0
    var poemCount = 0
    var poemList = emptyList<Content>()

    var comeFromResultFragment = true


    private val _longSearchDialogPosClick = MutableLiveData<Event<Unit>>()
    val longSearchDialogPosClick: LiveData<Event<Unit>>
        get() = _longSearchDialogPosClick
    fun runSearch() { _longSearchDialogPosClick.value = Event(Unit) }



    fun insertOrUpdate(search: RecentSearch) = viewModelScope.launch {
        dao.insertOrUpdate(search)
    }

//    fun getPoemWithID(poem_id: Int) = poemDao.getPoemWithID(poem_id)
//    fun getPoemRootWithID(poem_id: Int) = poemDao.getPoemRootWithID(poem_id)
//    fun getCatRootWithID(id: Int) = poemDao.getCatRootWithID(id)


    fun getAllSearchSuggest(search: String?, tokenCount: Int) =
        poemDao.getAllSearchSuggest(if (search.isNullOrEmpty()) "/"
            else "\"${search}*\"")

    fun getSearchSuggest(search: String?, poemID: Int, catID: Int, tokenCount: Int) =
        poemDao.getSearchSuggest(if (search.isNullOrEmpty()) "/"
            else "\"${search}*\"", poemID, allSubCategories(catID))

    fun getHistorySuggest(search: String?) = dao.getHistorySuggest(search ?: "")

//    fun searchAll(query: String) = poemDao.searchAll(makeTextBiErab(query))
    fun search(query: String, poemId: Int, catId: Int): LiveData<List<SearchContent>> {
        val allowedChars = "\"»«"
        val input = query.filter { it.isLetterOrDigit() || it.isWhitespace() || allowedChars.contains(it) }

        val finalQuery =
            if ((input.startsWith('\"') && input.endsWith('\"')) ||
                (input.startsWith('«') && input.endsWith('»'))) "\"${input.trim('\"', '»', '«')}\""
            else input.replace(" ", "* ").plus("*")

        return if (poemID == -1 && catID == -1)
            poemDao.searchAll(makeTextBiErab(finalQuery))
        else{
            if (poemId == -2 && allCategory.find { it.id == catId }?.parentID == 0)
                poemDao.search(makeTextBiErab(finalQuery), poemId, listOf(catId))
            else
                poemDao.search(makeTextBiErab(finalQuery), poemId, allSubCategories(catId))
        }
    }


    fun resultAddress(item: SearchContent): String {
        val catText = allUpCategories(item.poemm.catID).map { elem ->
            allCategory.find { it.id == elem }?.text }.reversed()
        var address = ""
        when {
            catID == -1 && poemID == -1 -> {
                catText.getOrNull(0)?.let { address += "${it.substringBefore('*')}، " }
                if (catText.size > 1){
                    catText.subList(1, catText.size).forEach { elem -> address += "${elem}، " }
                }
                address += item.poemm.title
            }
            poemID == -1 -> {
                if (catText.size > 1){
                    catText.subList(1, catText.size).forEach { elem -> address += "${elem}، " }
                }
                address += item.poemm.title
            }
            poemID == -2 -> {
                if (catText.size > 2){
                    catText.subList(2, catText.size).forEach { elem -> address += "${elem}، " }
                }
                address += item.poemm.title
            }
        }
        return address
    }

}