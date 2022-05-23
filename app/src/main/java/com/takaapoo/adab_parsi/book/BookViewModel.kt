package com.takaapoo.adab_parsi.book

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.takaapoo.adab_parsi.database.Content


class BookViewModel : ViewModel() {

//    val dao = PoemDatabase.getDatabase(application).dao()

    var listOpen = mutableMapOf<Int, Boolean>()
//    var sortedItems = List(0){Content()}

//    var bookContentHeight = mutableMapOf<Int, Int>()
    var bookFirstOpening = false

    lateinit var bookCurrentItem: Content
    var bookContentScrollPosition = mutableMapOf<Int, Content>()
//    var bookContentScrollHeight = mutableMapOf<Int, Int>()

    var offset = 0

    var poetLibContentShot: Bitmap? = null

    private val _showHelp = MutableLiveData<Int?>(null)
    val showHelp: LiveData<Int?>
        get() = _showHelp
    fun doShowHelp() { _showHelp.value = 1}
    fun doneShowHelp() { _showHelp.value = null}
    fun increaseShowHelp() { _showHelp.value = _showHelp.value?.plus(1) }

    var bookWidthMultiplier = 1f



}