package uet.app.mysound.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uet.app.mysound.common.SELECTED_LANGUAGE
import uet.app.mysound.data.dataStore.DataStoreManager
import uet.app.mysound.data.model.explore.mood.moodmoments.MoodsMomentObject
import uet.app.mysound.data.repository.MainRepository
import uet.app.mysound.utils.Resource
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class MoodViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application, private var dataStoreManager: DataStoreManager) : AndroidViewModel(application) {
    private val _moodsMomentObject: MutableLiveData<Resource<MoodsMomentObject>> = MutableLiveData()
    var moodsMomentObject: LiveData<Resource<MoodsMomentObject>> = _moodsMomentObject
    var loading = MutableLiveData<Boolean>()

    private var regionCode: String? = null
    private var language: String? = null
    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun getMood(params: String){
        loading.value = true
        viewModelScope.launch {
//            mainRepository.getMood(params, regionCode!!, SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language!!)]).collect{ values ->
//                _moodsMomentObject.value = values
//            }
            mainRepository.getMoodData(params).collect { values ->
                _moodsMomentObject.value = values
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }
}