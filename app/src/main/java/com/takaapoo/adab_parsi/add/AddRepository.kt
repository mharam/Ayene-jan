package com.takaapoo.adab_parsi.add

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.takaapoo.adab_parsi.database.PoemDatabase
import com.takaapoo.adab_parsi.network.PoetApi
import com.takaapoo.adab_parsi.network.PoetProperty
import kotlinx.coroutines.*
import java.text.Collator
import java.util.*

private const val minFileLength = 10000

class AddRepository(context: Context) {

    private val _loadStatus = MutableLiveData<ListLoadStatus>()
    val loadStatus: LiveData<ListLoadStatus>
        get() = _loadStatus

    private val handler = CoroutineExceptionHandler { _, exception ->
        _loadStatus.postValue(ListLoadStatus.ERROR)
    }

    private val collator = Collator.getInstance(Locale("fa"))
    val allPoet: MutableLiveData<List<PoetProperty>> = liveData(Dispatchers.IO + handler) {
        val poetList = PoetApi.retrofitService.getProperties(-1).body()
        val catPoet = PoemDatabase.getDatabase(context).dao().getCatPoet()

        poetList?.removeAll { item ->  catPoet.any { it.poetID == item.poetID } }
        _loadStatus.postValue(ListLoadStatus.DONE)

        poetList?.sortWith { one, two -> collator.compare(one.text, two.text) }
        emit(poetList?.toList())
    } as MutableLiveData<List<PoetProperty>>

    init {
        _loadStatus.value = ListLoadStatus.LOAD
    }

//    fun modifyAllPoet(poetId: Int){
//        val newAllPoet = allPoet.value?.toMutableList()
//        newAllPoet?.removeAll { it.poetID == poetId }
//        allPoet.postValue(newAllPoet)
//    }

}