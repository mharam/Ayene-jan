package com.takaapoo.adab_parsi.add

import androidx.lifecycle.*
import androidx.work.Data
import androidx.work.WorkInfo
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
data class PoetDownloadInfo(
    var progress: Int,
    var state: WorkInfo.State?,
    var installing: Boolean,
    var outputData: Data
)

@HiltViewModel
class AddViewModel @Inject constructor(
    dao: Dao,
    private val addRepository: AddRepository): ViewModel() {

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
    val downloadInfo: MutableMap<Int, LiveData<PoetDownloadInfo?>> = mutableMapOf()
    private val installedPoetId = dao.getAllPoetId()
    val notInstalledPoet = combine(allPoet.asFlow(), installedPoetId){ allPoetPropertyList, installedPoetIdList ->
        allPoetPropertyList?.filterNot { installedPoetIdList.contains(it.poetID) }
    }.stateIn(
        scope = viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.Eagerly
    )

    fun reloadAllPoet(){
        if (!::poetList.isInitialized){
            _loadStatus.postValue(ListLoadStatus.LOAD)

            viewModelScope.launch(handler) {
                val response = try {
                    PoetApi.retrofitService.getProperties(-1)
                } catch (e: Exception){
                    if (!::poetList.isInitialized)
                        _loadStatus.postValue(ListLoadStatus.ERROR)
                    return@launch
                }
                if (response.isSuccessful && response.body() != null){
                    poetList = response.body()!!
                    _loadStatus.postValue(ListLoadStatus.DONE)

                    poetList.sortWith { one, two -> collator.compare(one.text, two.text) }
                    allPoet.postValue(poetList)
                } else if (!::poetList.isInitialized)
                    _loadStatus.postValue(ListLoadStatus.ERROR)
            }
        }
    }

    fun downloadPoetOrCancel(poetItem: PoetProperty){
        addRepository.downloadPoetOrCancel(poetItem)
    }

}