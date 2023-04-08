package com.takaapoo.adab_parsi.add

import android.app.Application
import androidx.lifecycle.*
import com.takaapoo.adab_parsi.database.Dao
import com.takaapoo.adab_parsi.network.PoetApi
import com.takaapoo.adab_parsi.network.PoetProperty
import com.takaapoo.adab_parsi.util.collator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


enum class ListLoadStatus { LOAD, DONE, ERROR }

@HiltViewModel
class AddViewModel @Inject constructor(
    application: Application,
    dao: Dao): AndroidViewModel(application) {

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

    private val handler = CoroutineExceptionHandler { _: CoroutineContext, _: Throwable ->
        if (!::poetList.isInitialized)
            _loadStatus.postValue(ListLoadStatus.ERROR)
    }

    private lateinit var poetList: MutableList<PoetProperty>
    val allPoet = MutableLiveData<List<PoetProperty>?>()
    val progress: MutableMap<Int, MutableLiveData<Int>> = mutableMapOf()
    val installing: MutableMap<Int, MutableLiveData<Boolean>> = mutableMapOf()
    private val downloader: MutableMap<Int, Downloader> = mutableMapOf()
    private val installedPoetId = dao.getAllPoetId()
    val notInstalledPoet = combine(allPoet.asFlow(), installedPoetId){ allPoetPropertyList, installedPoetIdList ->
        allPoetPropertyList?.filterNot { installedPoetIdList.contains(it.poetID) }
    }.stateIn(
        scope = viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.Eagerly
    )

    fun modifyAllPoet(poetId: Int){
//        val newAllPoet = allPoet.value?.toMutableList()
//        newAllPoet?.removeAll { it.poetID == poetId }
//        allPoet.postValue(newAllPoet)
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
//                    val catPoet = PoemDatabase.getDatabase(getApplication()).dao().getCatPoet()
//
//                    val modifiedList = poetList.filterNot { item -> catPoet.any { it.poetID == item.poetID } }
//                            as MutableList
                    _loadStatus.postValue(ListLoadStatus.DONE)

                    poetList.sortWith { one, two -> collator.compare(one.text, two.text) }
                    allPoet.postValue(poetList)
                } else
                    _loadStatus.postValue(ListLoadStatus.ERROR)
            }
        }
    }

//    fun determineAllPoet(){
//        if (::poetList.isInitialized) {
//            viewModelScope.launch {
//                val catPoet = PoemDatabase.getDatabase(getApplication()).dao().getCatPoet()
//                val modifiedList =
//                    poetList.filterNot { item -> catPoet.any { it.poetID == item.poetID } }
//                            as MutableList
//                modifiedList.sortWith { one, two -> collator.compare(one.text, two.text) }
//                allPoet.postValue(modifiedList)
//            }
//        }
//    }

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