package com.takaapoo.adab_parsi.home

import android.app.Application
import androidx.lifecycle.*
import androidx.navigation.fragment.FragmentNavigator
import androidx.sqlite.db.SimpleSQLiteQuery
import com.takaapoo.adab_parsi.add.imagePrefix
import com.takaapoo.adab_parsi.add.thumbnailPrefix
import com.takaapoo.adab_parsi.database.*
import com.takaapoo.adab_parsi.util.FileIO
import com.takaapoo.adab_parsi.util.eventHandler.Event
import com.takaapoo.adab_parsi.util.wrapEspressoIdlingResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(app: Application, private val dao: Dao) : AndroidViewModel(app) {

//    val dao: Dao = PoemDatabase.getDatabase(application).dao()
//    private val repository: HomeRepository = HomeRepository(dao)
    val allCat: LiveData<List<Category>> = dao.getAllCat()
//    var allCategory = emptyList<Category>()

    var viewpagePosition = 0
    var firstOpenedAncient = -1
    var firstOpenedRecent = -1
    var count = 0
    var ancient = 0
    var enterPoetFragment = false
    var homePagerPosition = 0
    var deleteDialogTitle = ""

    var poetFirstOpening = false
    var spanCount = 1

    var selectedPoetCount = 0
    var navigatorExtra: FragmentNavigator.Extras? = null

    var ancientPoetIds = listOf<Int?>()
    var recentPoetIds = listOf<Int?>()



    private val _deletePoetDialogPosClick = MutableLiveData<Event<Unit>>()
    val deletePoetDialogPosClick: LiveData<Event<Unit>>
        get() = _deletePoetDialogPosClick
    fun deletePoet() { _deletePoetDialogPosClick.value = Event(Unit) }

    private val _deletePoetDialogNegClick = MutableLiveData<Event<Unit>>()
    val deletePoetDialogNegClick: LiveData<Event<Unit>>
        get() = _deletePoetDialogNegClick
    fun notDeletePoet() { _deletePoetDialogNegClick.value = Event(Unit) }

    private val _navigateToPoet = MutableLiveData<Event<Unit>>()
    val navigateToPoet: LiveData<Event<Unit>>
        get() = _navigateToPoet
    fun navigateToPoetFragment() { _navigateToPoet.value = Event(Unit) }


    private val _showHelp = MutableLiveData<Int?>(null)
    val showHelp: LiveData<Int?>
        get() = _showHelp
    fun doShowHelp() { _showHelp.value = 1}
    fun doneShowHelp() { _showHelp.value = null}
    fun increaseShowHelp() { _showHelp.value = _showHelp.value?.plus(1) }




//    fun verseCount() = viewModelScope.launch {
//        val count = dao.verseCount()
//        val myVerse = dao.myVerses()
//        Timber.i("verseCount = $count , myVerse = ${myVerse.count()}")
//    }


    fun deleteDatabase(poetID: List<Long>?) = viewModelScope.launch {
        wrapEspressoIdlingResource {
            poetID?.map { it.toInt() }?.let { poetIDList ->
                dao.run {
                    deleteVerse(poetIDList)
                    deletePoem(poetIDList)
                    deletePoet(poetIDList)
                    deleteCat(poetIDList)
                    vacuum(SimpleSQLiteQuery("VACUUM"))
                }
//                PoemDatabase.getDatabase(getApplication()).openHelper.writableDatabase.execSQL("VACUUM")

                val file = FileIO(getApplication())
                try {
                    poetIDList.forEach {
                        file.openFile(imagePrefix + "$it").delete()
                        file.openFile(thumbnailPrefix + "$it").delete()
                    }
                } catch (e: IOException) { }
            }
        }
    }

}