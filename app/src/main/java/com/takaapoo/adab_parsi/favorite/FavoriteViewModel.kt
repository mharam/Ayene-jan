package com.takaapoo.adab_parsi.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.takaapoo.adab_parsi.R
import com.takaapoo.adab_parsi.database.*
import com.takaapoo.adab_parsi.util.allCategory
import com.takaapoo.adab_parsi.util.allUpCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class FavoriteViewModel @Inject constructor(application: Application, private val dao: Dao) :
    AndroidViewModel(application) {

//    private val dao = PoemDatabase.getDatabase(application).dao()

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

    var comeFromFavoriteFragment = true
    var openedFromFavoriteDetailFrag = false



    fun getAllFavorite() = dao.getAllFavorite()
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