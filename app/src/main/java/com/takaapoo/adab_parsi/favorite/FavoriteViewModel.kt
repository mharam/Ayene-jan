package com.takaapoo.adab_parsi.favorite

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.*
import com.takaapoo.adab_parsi.util.allCategory
import com.takaapoo.adab_parsi.util.allUpCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class FavoriteViewModel @Inject constructor(
    application: Application,
    private val dao: Dao,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    var allFavorites = mutableListOf<FavoriteContent>()
    var favoriteCount = 0

    val startMargin = application.resources.getDimensionPixelSize(R.dimen.search_result_item_margin)
    val endMargin = application.resources.getDimensionPixelSize(R.dimen.search_result_item_margin)
    val verseSeparation = application.resources.getDimensionPixelSize(R.dimen.verse_separation)
    val versePadding = application.resources.getDimensionPixelSize(R.dimen.verse_padding)
    var rootWidth = 0
    var mesraContainerWidth = 0

    var lastResultOpened = 0
    var bottomViewedResultHeight = 0
    var topViewedResultHeight = 0
    var poemPosition = 0
    var poemCount = 0
    var poemList = mutableListOf<Content>()

    var scroll = 0
    var favoriteHeight = 0
    var lastItem = 0
    var firstItem = 0

    var favoriteListDisplace = 0
    var favoriteListAddedScroll = 0
    //    var selectedFavoriteItem: FavoriteContent? = null
    var detailPagerPosition = -1

    var comeFromFavoriteFragment = false
    var openedFromFavoriteDetailFrag = false

    init {
        savedStateHandle.get<Bundle?>("favorite_state")?.let {
            poemPosition = it.getInt("poem_position")
            lastResultOpened = it.getInt("last_result_opened")
            favoriteListDisplace = it.getInt("favorite_list_displace")
            favoriteListAddedScroll = it.getInt("favorite_list_added_scroll")
            scroll = it.getInt("scroll")
            bottomViewedResultHeight = it.getInt("bottom_viewed_result_height")
            topViewedResultHeight = it.getInt("top_viewed_result_height")
            detailPagerPosition = it.getInt("detail_pager_position", -1)
        }
        savedStateHandle.setSavedStateProvider("favorite_state"){
            Bundle().apply {
                putInt("poem_position", poemPosition)
                putInt("last_result_opened", lastResultOpened)
                putInt("favorite_list_displace", favoriteListDisplace)
                putInt("favorite_list_added_scroll", favoriteListAddedScroll)
                putInt("scroll", scroll)
                putInt("bottom_viewed_result_height", bottomViewedResultHeight)
                putInt("top_viewed_result_height", topViewedResultHeight)
                putInt("detail_pager_position", detailPagerPosition)
            }
        }
    }



    fun getAllFavorite() = dao.getAllFavorite()
    suspend fun getAllFavoriteSuspend() = withContext(Dispatchers.IO){
        allFavorites = dao.getAllFavoriteSuspend().toMutableList().also {
            poemList = it.map { fvContent ->
                Content(
                    fvContent.poemm.id!!,
                    fvContent.poemm.catID, fvContent.poemm.title, 1
                )
            } as MutableList<Content>
            poemCount = it.size
        }
    }
    fun favoriteCount() = dao.favoriteCount()

    fun favoriteAddress(item: RankedFavoriteContent): String {
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