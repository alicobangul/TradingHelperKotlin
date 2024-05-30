package com.basesoftware.tradinghelperkotlin.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.basesoftware.tradinghelperkotlin.model.domain.api.ApiResponseModel
import com.basesoftware.tradinghelperkotlin.model.domain.ResponseRecyclerModel
import com.basesoftware.tradinghelperkotlin.model.TradingHelperRepository
import com.basesoftware.tradinghelperkotlin.model.domain.api.ApiRequestModel
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
    fun newRequest(requestLibrary : String, requestBody : String) {

        when(requestLibrary) {

            "Retrofit" -> repository.requestRetrofit(requestBody)

            "Volley" -> repository.requestVolley(requestBody)

            "OkHttp" -> repository.requestOkHttp(requestBody)

            "HttpUrlConnection" -> repository.requestHttpUrlConnection(requestBody)

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
                shareLogoId = (it.shareInfo[2] ?: "null").toString(),
                shareCode = (it.shareInfo[0] ?: "-").toString(),
                sharePrice = (it.shareInfo[6] ?: 0.00) as Double,
                shareChangeAbs = (it.shareInfo[12]?: 0.00) as Double,
                shareChangePrice = (it.shareInfo[13] ?: 0.00) as Double,
                shareRsi7 = (it.shareInfo[14]?: 0.00) as Double,
                shareRsi14 = (it.shareInfo[15]?: 0.00) as Double,
                shareCci20 = (it.shareInfo[16]?: 0.00) as Double,
                shareName = (it.shareInfo[1]?: "-").toString()
            )
        }

        dataList.value = ArrayList(outputList)

    }

}