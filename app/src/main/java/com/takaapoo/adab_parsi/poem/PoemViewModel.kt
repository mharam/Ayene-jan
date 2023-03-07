package com.takaapoo.adab_parsi.poem

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.*
import com.takaapoo.adab_parsi.database.Content
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.network.DictionaryApi
import com.takaapoo.adab_parsi.network.DictionaryProperty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MeaningLoadStatus { LOADING, ERROR, DONE }

@HiltViewModel
class PoemViewModel @Inject constructor(
    val dao: Dao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var poemPosition = 0
    var poemCount = 0
    var poemList = emptyList<Content?>()
    var poemFirstOpening = false
    var bookContentScrollY = 0

    var contentShot: Bitmap? = null

    var poemContentHeight = mutableMapOf<Int, Int>()
    var x = 0f

    var shareOutFiles = booleanArrayOf(true, false, false)    // PDF, JPG, TXT
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

    var textMenuVisible = false

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

    var textHilight: String? = null
    var textVerseOrder = 0
    var textNoteVerseOrder = 0

    var gestureDetector: GestureDetectorCompat? = null

    init {
        savedStateHandle.get<Bundle?>("poem_state")?.let {
            poemPosition = it.getInt("poem_position")
            shareOutFiles = it.getBooleanArray("share_out_files") ?: booleanArrayOf(true, false, false)
        }
        savedStateHandle.setSavedStateProvider("poem_state"){
            Bundle().apply {
                putInt("poem_position", poemPosition)
                putBooleanArray("share_out_files", shareOutFiles)
            }
        }
    }

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

    val poemListLayoutCompleted = MutableLiveData(false)


    private val _uiEvent = Channel<PoemEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun reportEvent(event: PoemEvent){
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    private val _startPaging = MutableSharedFlow<Boolean>()
    val startPaging = _startPaging.asSharedFlow()

    fun startPaging(value: Boolean){
        viewModelScope.launch {
            _startPaging.emit(value)
        }
    }



    fun getVerseWithPoemID(poemId: Int) = dao.getVerseWithPoemID(poemId)
    fun getPoemBookmark(poemId: Int) = dao.getPoemBookmark(poemId)

    fun updateBookmark(state: Long?, poemId: Int){
//        Handler(Looper.getMainLooper()).postDelayed({
            viewModelScope.launch {
                dao.updateBookmark(state, poemId)
            }
//        }, 100)
    }

    fun updateFavorite(state: Long?, poemId: Int, vOrder: List<String>) = viewModelScope.launch {
        dao.updateFavorite(state, poemId, vOrder)
    }

    fun updateHilight(hilight: String?, poemId: Int, vOrder: Int) = viewModelScope.launch {
        dao.updateHilight(hilight, poemId, vOrder)
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