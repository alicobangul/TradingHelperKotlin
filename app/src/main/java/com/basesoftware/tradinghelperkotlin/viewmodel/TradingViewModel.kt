package com.basesoftware.tradinghelperkotlin.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.basesoftware.tradinghelperkotlin.data.model.ApiResponseModel
import com.basesoftware.tradinghelperkotlin.domain.model.ResponseRecyclerModel
import com.basesoftware.tradinghelperkotlin.data.repository.TradingHelperRepository
import com.basesoftware.tradinghelperkotlin.data.model.ApiRequestModel
import com.basesoftware.tradinghelperkotlin.util.Library
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

@HiltViewModel
class TradingViewModel @Inject constructor(private var repository: TradingHelperRepository) : ViewModel() {

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    val error = MutableLiveData<String>()

    val dataList = MutableLiveData<ArrayList<ResponseRecyclerModel>>()

    val searchText = MutableLiveData("")

    val libraryDialog = MutableLiveData<String>()

    val keyboardStatus = MutableLiveData(false)

    override fun onCleared() {

        compositeDisposable.clear() // Disposable temizlendi

        super.onCleared()

    }

    fun getData(searchData : String?) {

        keyboardStatus.value = false // Klavyeyi kapat

        searchData?.let {

            val text = it.replace(Regex(" "), "") // Metindeki boşlukları kaldır

            searchText.value = text // Boşlukları kaldırılmış metni EditText'e geri gönder

            libraryDialog.value = ApiRequestModel().toJsonText(text)

        }

    }

    // Seçilen kütüphaneye göre isteği at
    fun newRequest(requestLibrary : Library, requestBody : String) {

        when(requestLibrary) {

            Library.RETROFIT -> repository.requestRetrofit(requestBody)

            Library.VOLLEY -> repository.requestVolley(requestBody)

            Library.OKHTTP-> repository.requestOkHttp(requestBody)

            Library.HTTPURLCONNECTION -> repository.requestHttpUrlConnection(requestBody)

        }

    }

    fun openObservers() {

        compositeDisposable.addAll(
            repository.error.subscribe(error::setValue),
            repository.dataList.subscribe(this::convertData)
        )

    }

    private fun convertData(data : ApiResponseModel) {

        val outputList : List<ResponseRecyclerModel> = data.shareList.map {

            ResponseRecyclerModel(
                shareLogoId = it.shareInfo[2].toDataString(),
                shareCode = it.shareInfo[0].toDataString(),
                sharePrice = it.shareInfo[6].toDataDouble(),
                shareChangeAbs = it.shareInfo[12].toDataDouble(),
                shareChangePrice = it.shareInfo[13].toDataDouble(),
                shareRsi7 = it.shareInfo[14].toDataDouble(),
                shareRsi14 = it.shareInfo[15].toDataDouble(),
                shareCci20 = it.shareInfo[16].toDataDouble(),
                shareName = it.shareInfo[1].toDataString()
            )

        }

        dataList.value = ArrayList(outputList)

    }

    private fun Any?.toDataDouble() = (this ?: 0.00) as Double

    private fun Any?.toDataString() = (this ?: "-").toString()

}