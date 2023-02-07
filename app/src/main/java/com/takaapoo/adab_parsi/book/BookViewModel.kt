package com.takaapoo.adab_parsi.book

import android.os.Bundle
import androidx.lifecycle.*
import com.takaapoo.adab_parsi.database.Content
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


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

    private val _uiEvent = Channel<BookEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun reportEvent(event: BookEvent){
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

}