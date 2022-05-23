package com.takaapoo.adab_parsi.add

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.takaapoo.adab_parsi.database.PoemDatabase
import com.takaapoo.adab_parsi.network.PoetApi
import com.takaapoo.adab_parsi.network.PoetProperty
import com.takaapoo.adab_parsi.util.collator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch


enum class ListLoadStatus { LOAD, DONE, ERROR }

class AddViewModel (application: Application): AndroidViewModel(application) {

//    private val repository = AddRepository(application)
    private val _loadStatus = MutableLiveData<ListLoadStatus>()
    val loadStatus: LiveData<ListLoadStatus>
        get() = _loadStatus

    private val _snackMess = MutableLiveData<Int?>()
    val snackMess: LiveData<Int?>
        get() = _snackMess
    fun setMess(@StringRes mess: Int) { _snackMess.value = mess}
    fun snackMessShown() { _snackMess.value = null}

    private val handler = CoroutineExceptionHandler { _, exception ->
        _loadStatus.postValue(ListLoadStatus.ERROR)
    }

//    val allPoet: MutableLiveData<List<PoetProperty>> = liveData(Dispatchers.IO + handler) {
//        val poetList = PoetApi.retrofitService.getProperties(-1)
//        val catPoet = PoemDatabase.getDatabase(application).dao().getCatPoet()
//
//        poetList.removeAll { item ->  catPoet.any { it.poetID == item.poetID } }
//        _loadStatus.postValue(ListLoadStatus.DONE)
//
//        poetList.sortWith { one, two -> collator.compare(one.text, two.text) }
//        emit(poetList.toList())
//    } as MutableLiveData<List<PoetProperty>>

    private lateinit var poetList: MutableList<PoetProperty>
    val allPoet = MutableLiveData<List<PoetProperty>?>()
    val progress: MutableMap<Int, MutableLiveData<Int>> = mutableMapOf()
    val installing: MutableMap<Int, MutableLiveData<Boolean>> = mutableMapOf()

//    private val collator = Collator.getInstance(Locale("fa"))



    fun modifyAllPoet(poetId: Int){
        val newAllPoet = allPoet.value?.toMutableList()
        newAllPoet?.removeAll { it.poetID == poetId }
        allPoet.postValue(newAllPoet)
        installing.remove(poetId)
    }

    fun reloadAllPoet(){
//        allPoet.value.isNullOrEmpty()
        if (!::poetList.isInitialized){
            _loadStatus.postValue(ListLoadStatus.LOAD)

            viewModelScope.launch(handler) {
                poetList = PoetApi.retrofitService.getProperties(-1)
                val catPoet = PoemDatabase.getDatabase(getApplication()).dao().getCatPoet()

//                poetList.removeAll { item ->  catPoet.any { it.poetID == item.poetID } }
                val modifiedList = poetList.filterNot { item -> catPoet.any { it.poetID == item.poetID } }
                        as MutableList
                _loadStatus.postValue(ListLoadStatus.DONE)

                modifiedList.sortWith { one, two -> collator.compare(one.text, two.text) }
                allPoet.postValue(modifiedList)
            }
        }
    }

    fun determineAllPoet(){
        if (::poetList.isInitialized) {
            viewModelScope.launch {
                val catPoet = PoemDatabase.getDatabase(getApplication()).dao().getCatPoet()
                val modifiedList =
                    poetList.filterNot { item -> catPoet.any { it.poetID == item.poetID } }
                            as MutableList
                modifiedList.sortWith { one, two -> collator.compare(one.text, two.text) }
                allPoet.postValue(modifiedList)
            }
        }
    }


}