package com.takaapoo.adab_parsi.home

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigator
import androidx.sqlite.db.SimpleSQLiteQuery
import com.takaapoo.adab_parsi.add.imagePrefix
import com.takaapoo.adab_parsi.add.thumbnailPrefix
import com.takaapoo.adab_parsi.database.Category
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.util.Destinations
import com.takaapoo.adab_parsi.util.FileIO
import com.takaapoo.adab_parsi.util.allCategory
import com.takaapoo.adab_parsi.util.wrapEspressoIdlingResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    private val dao: Dao,
    private val homeRepository: HomeRepository) : AndroidViewModel(app) {

    val allCat: LiveData<List<Category>> = dao.getAllCat()

    var viewpagePosition = 0
    var firstOpenedAncient = -1
    var firstOpenedRecent = -1
    var count = 0
    var ancient = 0
    var enterPoetFragment = false
    var homePagerPosition = 0

    var poetFirstOpening = false
    var navigatorExtra: FragmentNavigator.Extras? = null

    var ancientPoetIds = listOf<Int?>()
    var recentPoetIds = listOf<Int?>()

    init {
        savedStateHandle.get<Bundle?>("home_state")?.let {
            count = it.getInt("number_of_poets")
            ancient = it.getInt("ancient")
            viewpagePosition = it.getInt("viewpage_position")
        }
        savedStateHandle.setSavedStateProvider("home_state"){
            Bundle().apply {
                putInt("number_of_poets", count)
                putInt("ancient", ancient)
                putInt("viewpage_position", viewpagePosition)
            }
        }
    }

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
        homeRepository.deletePoet(poetID?.map { it.toInt() }!!)
//        wrapEspressoIdlingResource {
//            poetID?.map { it.toInt() }?.let { poetIDList ->
//                dao.run {
//                    deleteVerse(poetIDList)
//                    deletePoem(poetIDList)
//                    deletePoet(poetIDList)
//                    deleteCat(poetIDList)
//                    vacuum(SimpleSQLiteQuery("VACUUM"))
//                }
////                PoemDatabase.getDatabase(getApplication()).openHelper.writableDatabase.execSQL("VACUUM")
//
//                val file = FileIO(getApplication())
//                try {
//                    poetIDList.forEach {
//                        file.openFile(imagePrefix + "$it").delete()
//                        file.openFile(thumbnailPrefix + "$it").delete()
//                    }
//                } catch (_: IOException) { }
//            }
//        }
    }

    suspend fun getAllCatSuspend() = withContext(Dispatchers.IO){
        allCategory = dao.getAllCatSuspend()
    }


}