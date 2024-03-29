package uet.app.mysound.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uet.app.mysound.common.SELECTED_LANGUAGE
import uet.app.mysound.common.SUPPORTED_LANGUAGE
import uet.app.mysound.data.dataStore.DataStoreManager
import uet.app.mysound.data.model.explore.mood.Mood
import uet.app.mysound.data.model.home.HomeItem
import uet.app.mysound.data.repository.MainRepository
import uet.app.mysound.utils.Resource
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    application: Application,
    private var dataStoreManager: DataStoreManager
) : AndroidViewModel(application) {
    private val _homeItemList: MutableLiveData<Resource<ArrayList<HomeItem>>> = MutableLiveData()
    val homeItemList: LiveData<Resource<ArrayList<HomeItem>>> = _homeItemList

    private val _homeItemListFromMySound: MutableLiveData<Resource<ArrayList<HomeItem>>> = MutableLiveData()
    val homeItemListFromMySound: LiveData<Resource<ArrayList<HomeItem>>> = _homeItemListFromMySound

    private val _exploreMoodItem: MutableLiveData<Resource<Mood>> = MutableLiveData()
    val exploreMoodItem: LiveData<Resource<Mood>> = _exploreMoodItem
    private val _accountInfo: MutableLiveData<Pair<String?, String?>?> = MutableLiveData()
    val accountInfo: LiveData<Pair<String?, String?>?> = _accountInfo

    val showSnackBarErrorState = MutableSharedFlow<String>()


    val loading = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()
    private var regionCode: String = ""
    private var language: String = ""

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError("Exception handled: ${throwable.localizedMessage}")
    }

    init {
        viewModelScope.launch {
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
            //  refresh when region change
            dataStoreManager.location.distinctUntilChanged().collect {
                regionCode = it
                getHomeItemList()
            }
            dataStoreManager.language.distinctUntilChanged().collect {
                language = it
                getHomeItemList()
            }
        }
    }

    fun getHomeItemList() {
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() ?: SUPPORTED_LANGUAGE.codes.first() }
        regionCode = runBlocking { dataStoreManager.location.first() }
        loading.value = true
        viewModelScope.launch {
            combine(
//                mainRepository.getHome(
//                    regionCode,
//                    SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language)]
//                ),
                mainRepository.getHomeData(),
                mainRepository.getHomeDataFromMySound(),
            ) { home, homeItemListFromMySound ->
                Pair(home, homeItemListFromMySound)
            }.collect { result ->
                val home = result.first
                Log.d("home size", "${home.data?.size}")
                val homeItemListFromMySound = result.second
                _homeItemList.value = home
                _homeItemListFromMySound.value = homeItemListFromMySound

                Log.d("HomeViewModel", "getHomeItemList: $result")
                loading.value = false
                dataStoreManager.cookie.first().let {
                    if (it != "") {
                        _accountInfo.postValue(Pair(dataStoreManager.getString("AccountName").first(), dataStoreManager.getString("AccountThumbUrl").first()))
                    }
                }
                when {
                    home is Resource.Error -> home.message
                    homeItemListFromMySound is Resource.Error -> homeItemListFromMySound.message
                    else -> null
                }?.let {
                    showSnackBarErrorState.emit(it)
                    Log.w("Error", "getHomeItemList: ${home.message}")
                    Log.w("Error", "getHomeItemList: ${homeItemListFromMySound.message}")
                }
            }
        }
    }

    private fun onError(message: String) {
        errorMessage.value = message
        loading.value = false
    }

}