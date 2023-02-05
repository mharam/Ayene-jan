package com.takaapoo.adab_parsi.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.takaapoo.adab_parsi.database.PoemDatabase
import com.takaapoo.adab_parsi.network.PoetApi
import com.takaapoo.adab_parsi.network.PoetProperty
import com.takaapoo.adab_parsi.util.collator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


enum class ListLoadStatus { LOAD, DONE, ERROR }

class AddViewModel (application: Application): AndroidViewModel(application) {

    private val _loadStatus = MutableLiveData<ListLoadStatus>()
    val loadStatus: LiveData<ListLoadStatus>
        get() = _loadStatus

    private val _uiEvent = Channel<AddEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun reportEvent(event: AddEvent){
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    private val handler = CoroutineExceptionHandler { _, exception ->
        if (!::poetList.isInitialized)
            _loadStatus.postValue(ListLoadStatus.ERROR)
    }

    private lateinit var poetList: MutableList<PoetProperty>
    val allPoet = MutableLiveData<List<PoetProperty>?>()
    val progress: MutableMap<Int, MutableLiveData<Int>> = mutableMapOf()
    val installing: MutableMap<Int, MutableLiveData<Boolean>> = mutableMapOf()
    private val downloader: MutableMap<Int, Downloader> = mutableMapOf()


    fun modifyAllPoet(poetId: Int){
        val newAllPoet = allPoet.value?.toMutableList()
        newAllPoet?.removeAll { it.poetID == poetId }
        allPoet.postValue(newAllPoet)
        installing.remove(poetId)
        downloader.remove(poetId)
    }

    fun reloadAllPoet(){
        if (!::poetList.isInitialized){
            _loadStatus.postValue(ListLoadStatus.LOAD)

            viewModelScope.launch(handler) {
                val response = try {
                    PoetApi.retrofitService.getProperties(-1)
                } catch (e: Exception){
                    _loadStatus.postValue(ListLoadStatus.ERROR)
                    return@launch
                }
                if (response.isSuccessful && response.body() != null){
                    poetList = response.body()!!
                    val catPoet = PoemDatabase.getDatabase(getApplication()).dao().getCatPoet()

                    val modifiedList = poetList.filterNot { item -> catPoet.any { it.poetID == item.poetID } }
                            as MutableList
                    _loadStatus.postValue(ListLoadStatus.DONE)

                    modifiedList.sortWith { one, two -> collator.compare(one.text, two.text) }
                    allPoet.postValue(modifiedList)
                } else
                    _loadStatus.postValue(ListLoadStatus.ERROR)
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

    fun downloadPoet(poetItem: PoetProperty){
        downloader.getOrPut(
            key = poetItem.poetID,
            defaultValue = {
                Downloader(
                    vm = this,
                    context = getApplication<Application>().applicationContext,
                    poetItem = poetItem
                )
            }
        ).download()
    }

}