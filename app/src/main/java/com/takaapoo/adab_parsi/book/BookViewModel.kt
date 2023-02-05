package com.takaapoo.adab_parsi.book

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.takaapoo.adab_parsi.database.Content


class BookViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    var listOpen = HashMap<Int, Boolean>()
    var bookFirstOpening = false

    var bookCurrentItem: Content? = null
    var bookContentScrollPosition = mutableMapOf<Int, Content>()

    init {
        savedStateHandle.get<Bundle?>("book_state")?.let {
            bookCurrentItem = it.getParcelable("book_current_item")
            listOpen = (it.getSerializable("list_open") as HashMap<Int, Boolean>)
        }
        savedStateHandle.setSavedStateProvider("book_state"){
            Bundle().apply {
                putParcelable("book_current_item", bookCurrentItem)
                putSerializable("list_open", listOpen)
            }
        }
    }

    private val _showHelp = MutableLiveData<Int?>(null)
    val showHelp: LiveData<Int?>
        get() = _showHelp
    fun doShowHelp() { _showHelp.value = 1}
    fun doneShowHelp() { _showHelp.value = null}
    fun increaseShowHelp() { _showHelp.value = _showHelp.value?.plus(1) }




}