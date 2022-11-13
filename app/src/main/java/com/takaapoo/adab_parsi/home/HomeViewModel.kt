package com.takaapoo.adab_parsi.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigator
import androidx.sqlite.db.SimpleSQLiteQuery
import com.takaapoo.adab_parsi.add.imagePrefix
import com.takaapoo.adab_parsi.add.thumbnailPrefix
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.util.Destinations
import com.takaapoo.adab_parsi.util.FileIO
import com.takaapoo.adab_parsi.util.wrapEspressoIdlingResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(app: Application, private val dao: Dao) : AndroidViewModel(app) {

    val allCat: LiveData<List<Category>> = dao.getAllCat()

    var viewpagePosition = 0
    var firstOpenedAncient = -1
    var firstOpenedRecent = -1
    var count = 0
    var ancient = 0
    var enterPoetFragment = false
    var homePagerPosition = 0
//    var deleteDialogTitle = ""

    var poetFirstOpening = false
    var spanCount = 1

//    var selectedPoetCount = 0
    var navigatorExtra: FragmentNavigator.Extras? = null

    var ancientPoetIds = listOf<Int?>()
    var recentPoetIds = listOf<Int?>()



//    private val _showHelp = MutableLiveData<Int?>(null)
//    val showHelp: LiveData<Int?>
//        get() = _showHelp
//    fun doShowHelp() { _showHelp.value = 1}
//    fun doneShowHelp() { _showHelp.value = null}
//    fun increaseShowHelp() { _showHelp.value = _showHelp.value?.plus(1) }


    private val _actionModeState = MutableStateFlow(ActionModeState.GONE)
    val actionModeState = _actionModeState.asStateFlow()
    fun setActionModeState(state: ActionModeState){
         _actionModeState.value = state
    }

    private val _uiEvent = Channel<HomeEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun reportEvent(event: HomeEvent){
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
    // Defined just for data binding
    fun reportEvent(destination: Destinations){
        viewModelScope.launch {
            _uiEvent.send(HomeEvent.Navigate(destination))
        }
    }

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