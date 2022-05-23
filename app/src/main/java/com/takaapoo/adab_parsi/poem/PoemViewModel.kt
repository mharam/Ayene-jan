package com.takaapoo.adab_parsi.poem

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.network.DictionaryApi
import com.takaapoo.adab_parsi.network.DictionaryProperty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MeaningLoadStatus { LOADING, ERROR, DONE }

@HiltViewModel
class PoemViewModel @Inject constructor(val dao: Dao) : ViewModel() {

//    val dao = PoemDatabase.getDatabase(application).dao()

    var poemPosition = 0
    var poemCount = 0
    var poemList = emptyList<Content?>()
    var poemFirstOpening = false
    var bookContentScrollY = 0
    var bPFcontentItem : Content? = null

    var contentShot: Bitmap? = null

//    var topPadding = 0

    var poemContentHeight = mutableMapOf<Int, Int>()
    var x = 0f

    var shareOutFiles = booleanArrayOf(false, false, false)    // PDF, JPG, TXT
    var exportOutFile = 0       // 0 = PDF, 1 = JPG, 2 = TXT

    var keyboardIsOpen = false
    var searchQuery: String? = ""
    var positionPointer = 0
    var resultVersePositions = mutableListOf<IntArray>()

    var resultRowId1 = emptyList<Int>()

    var selectedVerses = mutableMapOf<Int, MutableLiveData<MutableList<Int>>>()    // Int = poemID
    var touchedViewID = 0

    val noteOpenedVerses = mutableMapOf<Int, MutableList<Int>>()    // Int = poemID
    var commentTextFocused = false

    private val _refreshContent = MutableLiveData<Boolean?>()
    val refreshContent: LiveData<Boolean?>
        get() = _refreshContent
    fun refresh() { _refreshContent.value = true}
    fun doneRefreshing() { _refreshContent.value = null}

    var textMenuHide = false
    var textMenuVisible = false

//    var textMenuTextView: TextView? = null
    var textMenuText: String? = null
    var textMenuStart = 0
    var textMenuEnd = 0
    val textMenuLocation = IntArray(2){0}
    var textMenuX = 0
    var textMenuY = 0
    var leftHandleX = 0f
    var leftHandleY = 0f
    var rightHandleX = 0f
    var rightHandleY = 0f


//    var textMenuActionMode: android.view.ActionMode? = null
    var textHilight: String? = null
    var textVerseOrder = 0
    var textNoteVerseOrder = 0

    private val _refreshTextMenu = MutableLiveData<Boolean?>()
    val refreshTextMenu: LiveData<Boolean?>
        get() = _refreshTextMenu
    fun doRefreshTextMenu() { _refreshTextMenu.value = true}
    fun doneRefreshingTextMenu() { _refreshTextMenu.value = null}

//    val appBarExpanded = MutableLiveData(true)

    private val _meanLoadStatus = MutableLiveData<MeaningLoadStatus>()
    val meanLoadStatus: LiveData<MeaningLoadStatus>
        get() = _meanLoadStatus
    var meanWord = ""


    var poemFileUri: Uri? = null
    private val _savePoem = MutableLiveData<Boolean?>()
    val savePoem: LiveData<Boolean?>
        get() = _savePoem
    fun savePoemToFile() { _savePoem.value = true}
    fun notSavePoemToFile() { _savePoem.value = false}
    fun doneSavePoemToFile() { _savePoem.value = null}

    private val _showHelp = MutableLiveData<Int?>(null)
    val showHelp: LiveData<Int?>
        get() = _showHelp
    fun doShowHelp() { _showHelp.value = 1}
    fun doneShowHelp() { _showHelp.value = null}
    fun increaseShowHelp() { _showHelp.value = _showHelp.value?.plus(1) }

    val poemListLayoutCompleted = MutableLiveData(false)









//    fun getCatWithID(id: Int) = dao.getCatWithID(id)
//    fun getCatRootWithID(id: Int) = dao.getCatRootWithID(id)
    fun getVerseWithPoemID(poem_id: Int) = dao.getVerseWithPoemID(poem_id)
    fun getPoemBookmark(poem_id: Int) = dao.getPoemBookmark(poem_id)

    fun updateBookmark(state: Long?, poem_id: Int){
//        Handler(Looper.getMainLooper()).postDelayed({
            viewModelScope.launch {
                dao.updateBookmark(state, poem_id)
            }
//        }, 100)
    }

    fun updateFavorite(state: Long?, poem_id: Int, vOrder: List<String>) = viewModelScope.launch {
        dao.updateFavorite(state, poem_id, vOrder)
    }

    fun updateHilight(hilight: String?, poem_id: Int, vOrder: Int) = viewModelScope.launch {
        dao.updateHilight(hilight, poem_id, vOrder)
    }

    fun updateNote(text: String?, poemId: Int, vOrder: Int) = viewModelScope.launch {
        dao.updateNote(text, poemId, vOrder)
    }

    val meaning = MutableLiveData<List<DictionaryProperty>>()
    fun getMeaning(word: String) = viewModelScope.launch {
        try {
            _meanLoadStatus.postValue(MeaningLoadStatus.LOADING)
            val mean = DictionaryApi.retrofitService.getProperties(word)
            _meanLoadStatus.postValue(MeaningLoadStatus.DONE)
            meaning.postValue(mean)
        } catch (e: Exception){
            _meanLoadStatus.postValue(MeaningLoadStatus.ERROR)
            meaning.postValue(emptyList())
        }
    }
}