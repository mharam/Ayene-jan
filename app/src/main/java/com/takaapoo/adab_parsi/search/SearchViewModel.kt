package com.takaapoo.adab_parsi.search

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.*
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.*
import com.takaapoo.adab_parsi.util.*
import com.takaapoo.adab_parsi.util.eventHandler.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject


@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    private val dao: RecentSearchDao,
    private val poemDao: Dao,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

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
//    var resultLastItem = 0
//    var resultFirstItem = 0
    var resultListDisplace = 0
    var lastResultOpened = 0
    var bottomViewedResultHeight = 0
    var topViewedResultHeight = 0
    var detailPagerPosition = -1

    var openedFromDetailFrag = false

    var poemPosition = 0
    var poemCount = 0
    var poemList = emptyList<Content>()

    var comeFromResultFragment = false

    init {
        savedStateHandle.get<Bundle?>("search_state")?.let {
            submittedSearch = it.getString("submitted_search") ?: ""
            searchDomain = it.getString("search_domain") ?: ""
            poemID = it.getInt("poem_id", -1)
            catID = it.getInt("cat_id", -1)
            poemPosition = it.getInt("poem_position")
            detailPagerPosition = it.getInt("detail_pager_position", -1)
            scroll = it.getInt("scroll")
            resultListDisplace = it.getInt("result_list_displace")
            lastResultOpened = it.getInt("last_result_opened")
            bottomViewedResultHeight = it.getInt("bottom_viewed_result_height")
            topViewedResultHeight = it.getInt("top_viewed_result_height")
        }
        savedStateHandle.setSavedStateProvider("search_state"){
            Bundle().apply {
                putString("submitted_search", submittedSearch)
                putString("search_domain", searchDomain)
                putInt("poem_id", poemID)
                putInt("cat_id", catID)
                putInt("poem_position", poemPosition)
                putInt("detail_pager_position", detailPagerPosition)
                putInt("scroll", scroll)
                putInt("result_list_displace", resultListDisplace)
                putInt("last_result_opened", lastResultOpened)
                putInt("bottom_viewed_result_height", bottomViewedResultHeight)
                putInt("top_viewed_result_height", topViewedResultHeight)
            }
        }
    }

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
    suspend fun search() = viewModelScope.async(Dispatchers.IO) {
        val allowedChars = "\"»«"
        val input = submittedSearch.trim(' ', '‌' ).filter {
            it.isLetterOrDigit() || it.isWhitespace() || allowedChars.contains(it)
        }

        val finalQuery =
            if ((input.startsWith('\"') && input.endsWith('\"')) ||
                (input.startsWith('«') && input.endsWith('»'))) "\"${input.trim('\"', '»', '«')}\""
            else input.replace(" ", "* ").plus("*")

        val finalResult = sortedResults(
            if (poemID == -1 && catID == -1)
                poemDao.searchAll(makeTextBiErab(finalQuery))
            else {
                if (poemID == -2 && allCategory.find { it.id == catID }?.parentID == 0)
                    poemDao.search(makeTextBiErab(finalQuery), poemID, listOf(catID))
                else
                    poemDao.search(makeTextBiErab(finalQuery), poemID, allSubCategories(catID))
            }
        )
        comeFromDetailFragment = true
        searchResultList = finalResult
        poemList = finalResult.map {
            Content(it.poemm.id!!, it.poemm.catID, it.poemm.title, 1)
        }
        poemCount = finalResult.size

        return@async finalResult
    }.await()

    private fun resultRank(item: SearchContent): Float{
        val text = makeTextBiErab(when (item.position){
            0,2 -> "${item.majorText?.trim()} ${item.minorText?.trim()}"
            1,3 -> "${item.minorText?.trim()} ${item.majorText?.trim()}"
            else -> item.majorText?.trim() ?: ""
        })

        val splittingChars = charArrayOf(' ', '،', '!', '؛', ':', '؟', '.')
        val searchQuery =
            makeTextBiErab(submittedSearch.filter { it.isLetterOrDigit() || it.isWhitespace()})
        val splittedQuery = searchQuery.split(*splittingChars)
        val splittedText = text.split(*splittingChars)

        val indices = mutableListOf<List<Int>>()
        splittedQuery.forEach {
            val allIndex = splittedText.allIndexOf(it)
            if (allIndex.isNotEmpty())
                indices.add(allIndex)
        }

        val foundPhrases = indices.map { it.size }.sum()        // number of search phrases in this result
        var phraseCharCount = 0f                                // total number of found phrases characters in this result
        indices.forEach {
            phraseCharCount += (splittedText.filterIndexed { index, _ -> it.contains(index) }
                .map { it.length }).average().toFloat()
        }

        var distance = 0f
        for (i in 0 until indices.size - 1){
            val lengthArray = mutableListOf<Float>()
            indices[i].forEach { first ->
                indices[i+1].forEach { second ->
                    val minIndex = minOf(first, second)
                    val maxIndex = maxOf(first, second)
                    val charDistance =
                        (splittedText.filterIndexed{ind, _ -> ind < maxIndex}.map { it.length }.sum() + maxIndex) -
                                (splittedText.filterIndexed{ind, _ -> ind < minIndex}.map { it.length }.sum()
                                        + minIndex + splittedQuery[i].length)
                    lengthArray.add(if (first < second) charDistance.toFloat() else 1.5f * charDistance)
                }
            }
            distance += lengthArray.minOrNull() ?: 0f
        }

        val exactPhrasePoint = if (text.contains(searchQuery)) 100 else 0

        return when{
            indices.size <= 1 -> 4 * foundPhrases - phraseCharCount
            else -> 3*(foundPhrases - phraseCharCount) - distance + exactPhrasePoint
        }
    }

    private suspend fun sortedResults(result: List<SearchContent>): List<SearchContent>{
        return withContext(Dispatchers.Default){
            val distinctResult = result.distinctBy { it.rowId1 + it.rowId2 }
            val resultRanks = mutableMapOf<Int, Float>()
            for (i in distinctResult.indices){
                if (!isActive) break
                resultRanks[distinctResult[i].rowId1] = resultRank(distinctResult[i])
            }
            if (!isActive)
                distinctResult
            else
                distinctResult.sortedWith { r1, r2 ->
                    when {
                        resultRanks[r1.rowId1] == resultRanks[r2.rowId1] -> 0
                        resultRanks[r1.rowId1]!! > resultRanks[r2.rowId1]!! -> -1
                        else -> 1
                    }
                }
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