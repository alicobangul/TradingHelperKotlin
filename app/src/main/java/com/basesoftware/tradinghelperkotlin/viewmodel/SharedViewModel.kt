package com.basesoftware.tradinghelperkotlin.viewmodel

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.basesoftware.tradinghelperkotlin.adapter.ShareAdapter
import com.basesoftware.tradinghelperkotlin.db.WatchDao
import com.basesoftware.tradinghelperkotlin.db.WatchDatabase
import com.basesoftware.tradinghelperkotlin.model.ApiResponseModel
import com.basesoftware.tradinghelperkotlin.model.ResponseRecyclerModel
import com.basesoftware.tradinghelperkotlin.service.TradingAPI
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.connectionSettings
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toJsonObject
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toResponseModel
import com.basesoftware.tradinghelperkotlin.util.WorkUtil
import com.google.gson.GsonBuilder
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableMaybeObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SharedViewModel(context: Context, private val state : SavedStateHandle) : ViewModel() {

    lateinit var db : WatchDatabase
    lateinit var dao : WatchDao

    private var retrofit : Retrofit = Retrofit.Builder()
        .baseUrl(WorkUtil.BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create()) // String şeklinde body yollamak için ScalarsConverterFactory
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    private var retrofitAPI : TradingAPI = retrofit.create(TradingAPI::class.java)

    private var okHttpClient : OkHttpClient
    private lateinit var okHttpRequest : okhttp3.Request

    private var volleyQueue : RequestQueue

    private lateinit var httpUrlConnection : HttpURLConnection

    private var compositeDisposable : CompositeDisposable = CompositeDisposable()

    val mutableResponse : MutableLiveData<ApiResponseModel?> by lazy { MutableLiveData<ApiResponseModel?>() }



    init {

        volleyQueue = Volley.newRequestQueue(context) // Volley queue tanımlandı

        okHttpClient = OkHttpClient() // OkHttpClient oluşturuldu

    }

    override fun onCleared() {

        compositeDisposable.clear() // CompositeDisposable temizlendi

        super.onCleared()

    }

    companion object {
        /**
         * ViewModel'ı istediğimiz parametrelerle başlatmak için viewmodel factory
         * Volley tanımlamasını context ile yapabilmek kullanıldı
         * SavedStateHandle içinde ViewModelProvider.Factory yerine AbstractSavedStateViewModelFactory kullanıldı
         */
        fun provideFactory(context: Context, owner: SavedStateRegistryOwner, defaultArgs: Bundle? = null) : AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(key: String, modelClass: Class<T>, state: SavedStateHandle): T = SharedViewModel(context, state) as T
            }

    }



    fun getInit() = state.get("init") ?: false // Uygulama initialize edildi mi?
    fun setInit(init : Boolean) = state.set("init", init)

    private fun getSelectedLibrary() = state.get("selectedLibrary") ?: "Retrofit" // Kullanıcının seçtiği kütüphane
    fun setSelectedLibrary(library : String) = state.set("selectedLibrary", library)

    fun getIsRxJava() = state.get("isRxjava") ?: true // RxJava mı kullanılacak Coroutine mi ?
    fun setIsRxJava(isRxjava : Boolean) = state.set("isRxjava", isRxjava)

    // Seçilen kütüphaneye göre isteği at
    fun request(requestBody : String) {

        when(getSelectedLibrary()) {

            "Retrofit" -> requestRetrofit(requestBody)

            "Volley" -> requestVolley(requestBody)

            "OkHttp" -> requestOkHttp(requestBody)

            "HttpUrlConnection" -> requestHttpUrlConnection(requestBody)

        }

    }

    private fun requestRetrofit(requestBody : String) {

        compositeDisposable.clear() // Disposable temizlendi

        when(getIsRxJava()) {

            // Kullanıcı RxJava seçti
            true -> compositeDisposable.add(
                retrofitAPI.getDataRxJava(WorkUtil.PATH_URL, requestBody)
                    .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                    .observeOn(AndroidSchedulers.mainThread()) // MainThread'de gözlemlendi
                    .subscribeWith(object : DisposableSingleObserver<ApiResponseModel>() {

                        /**
                         * İstek sonucu response LiveData tetiklemesi için post edildi
                         * setValue = Ana iş parçacağından çağrılmalı
                         * postValue = Arka plan iş parçacağından çağrılabilir
                         */
                        override fun onSuccess(response: ApiResponseModel) { mutableResponse.postValue(response) }

                        override fun onError(t: Throwable) { Log.e("RequestRetrofit", "Retrofit-RxJava Failure") }

                    })
            )

            // Kullanıcı Coroutine seçti
            false -> Thread {

                CoroutineScope(Dispatchers.IO).launch {

                    runCatching {

                        // I/O thread kullanılarak api isteği yapıldı
                        retrofitAPI.getData(WorkUtil.PATH_URL, requestBody).enqueue(object : retrofit2.Callback<ApiResponseModel> {
                            override fun onResponse(call: retrofit2.Call<ApiResponseModel>, response: retrofit2.Response<ApiResponseModel>) {

                                when(response.isSuccessful) {

                                    true -> response.body()?.let { mutableResponse.postValue(it) } // İstek sonucu response, LiveData tetiklemesi için post edildi

                                    else -> Log.e("RequestRetrofit", "Retrofit Failure in onResponse")

                                }

                            }

                            override fun onFailure(call: retrofit2.Call<ApiResponseModel>, t: Throwable) { Log.e("RequestRetrofit", "Retrofit Failure") }

                        })

                    }

                }

            }.start()

        }

    }

    private fun requestVolley(requestBody: String) {

        compositeDisposable.clear() // Disposable temizlendi

        when(getIsRxJava()) {

            true -> compositeDisposable.add(
                Single
                    .create { single ->
                        volleyQueue.add(
                            JsonObjectRequest( // JSON objesi ile istek atmak için JsonObjectRequest
                                Request.Method.POST, // Kullanılacak Method (GET-POST-DELETE vb)
                                WorkUtil.REQUEST_URL, // İsteğin yapılacağı url
                                requestBody.toJsonObject(), // Request body (Json obje türünde request olduğu için string'i objeye çeviriyoruz)
                                { single.onSuccess(it) }, // Eğer başarılı ise onSuccess tetikle
                                { single.onError(IOException()) } // Eğer başarısız ise onError tetikle
                            )
                        )
                    }
                    .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                    .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                    .subscribeWith(object : DisposableSingleObserver<JSONObject>(){

                        override fun onSuccess(obj: JSONObject) { mutableResponse.postValue(obj.toResponseModel()) } // Gelen objeyi modele çevirerek LiveData aktar

                        override fun onError(e: Throwable) { Log.e("RequestVolley", "Volley-RxJava Failure") } // Başarısız ise Log yazdır

                    })
            )

            false -> Thread {

                CoroutineScope(Dispatchers.IO).launch {

                    runCatching {

                        volleyQueue.add(JsonObjectRequest( // JSON objesi ile istek atmak için JsonObjectRequest
                            Request.Method.POST, // Kullanılacak Method (GET-POST-DELETE vb)
                            WorkUtil.REQUEST_URL, // İsteğin yapılacağı url
                            requestBody.toJsonObject(), // Request body (Json obje türünde request olduğu için string'i objeye çeviriyoruz)
                            { mutableResponse.postValue(it.toResponseModel()) }, // Eğer başarılı ise, gelen objeyi modele çevirerek LiveData aktar
                            { Log.e("RequestVolley", "Volley Failure") } // Başarısız ise Log yazdır
                        ))
                    }

                }

            }.start()

        }

    }

    private fun requestOkHttp(requestBody: String) {

        compositeDisposable.clear() // Disposable temizle

        okHttpRequest = okhttp3.Request.Builder() // OkHttp için Request Builder oluştur
            .method("POST", requestBody.toRequestBody()) // Kullanılacak method (GET-POST vb) & request body [boş olamaz]
            .url(WorkUtil.REQUEST_URL) // İstek atılacak url
            .build()

        when(getIsRxJava()) {

            true -> compositeDisposable.add(
                Single
                    .fromCallable {
                        // Eğer throw atılmazsa (sadece response[execute] return ederse) hata olsa dahi onSuccess dönüyor
                        val response = okHttpClient.newCall(okHttpRequest).execute() // OkHttpClient kullanarak istek yapıldı
                        return@fromCallable if (response.isSuccessful) response else throw IOException()
                    }
                    .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                    .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                    .subscribeWith(object : DisposableSingleObserver<Response>(){

                        override fun onSuccess(response : Response) {

                            // Dönen response başarılı ise modele çevir ve LiveData gönder
                            response.body?.let { mutableResponse.postValue(JSONObject(it.string()).toResponseModel()) }

                            // Kaynakları bırak
                            response.body?.close()
                        }

                        override fun onError(t : Throwable) { Log.e("RequestOkHttp", "OkHttp-RxJava Failure") } // Başarısız ise Log yazdır

                    })
            )

            false -> Thread {

                CoroutineScope(Dispatchers.IO).launch {

                    runCatching {

                        // OkHttpClient kullanarak istek yapıldı
                        okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {

                            override fun onResponse(call: Call, response: Response) {

                                when(response.isSuccessful) {
                                    // Dönen response başarılı ise modele çevir ve LiveData gönder
                                    true -> response.body?.let { mutableResponse.postValue(JSONObject(it.string()).toResponseModel()) }

                                    else -> Log.e("RequestOkHttp", "OkHttp Failure in onResponse") // Başarısız ise Log yazdır
                                }

                                response.body?.close() // Kaynakları bırak

                            }

                            override fun onFailure(call: Call, e: IOException) { Log.e("RequestOkHttp", "OkHttp Failure") } // Başarısız ise Log yazdır

                        })

                    }

                }

            }.start()

        }

    }

    private fun requestHttpUrlConnection(requestBody: String) {

        compositeDisposable.clear() // Disposable temizle

        when(getIsRxJava()) {

            true -> compositeDisposable.add(
                Single
                    .fromCallable {

                        httpUrlConnection = URL(WorkUtil.REQUEST_URL).openConnection() as HttpURLConnection // Url'i kullanarak bağlantıyı aç
                        httpUrlConnection.connectionSettings() // Bağlantıya ait ayarları yap

                        OutputStreamWriter(httpUrlConnection.outputStream).apply { // OutputStreamWriter oluştur
                            write(requestBody) // Yazılacak requestbody
                            flush()
                        }

                        return@fromCallable when(httpUrlConnection.responseCode) {
                            200 -> httpUrlConnection.inputStream.bufferedReader().use { it.readText() } // Eğer istek başarılı ise dönen veriyi onSuccess gönder
                            else -> throw IOException() // Eğer istek başarısız ise throw ile onError tetikle
                        }

                    }
                    .subscribeOn(Schedulers.io()) // I/O thread kullanımı
                    .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                    .subscribeWith(object : DisposableSingleObserver<String>() {
                        override fun onSuccess(response : String) {
                            // Gelen veriyi modele çevir LiveData gönder & bağlantıyı kapat
                            mutableResponse.postValue(JSONObject(response).toResponseModel()).also {  httpUrlConnection.disconnect() }
                        }

                        override fun onError(e: Throwable) {
                            Log.e("RequestHttpUrlCon", "HttpUrlConnection-RxJava Failure") // Başarısız ise Log yazdır
                            httpUrlConnection.disconnect() // Bağlantıyı kapat
                        }
                    })
            )

            else -> Thread {

                CoroutineScope(Dispatchers.IO).launch {

                    runCatching {

                        httpUrlConnection = URL(WorkUtil.REQUEST_URL).openConnection() as HttpURLConnection // Url'i kullanarak bağlantıyı aç
                        httpUrlConnection.connectionSettings() // Bağlantıya ait ayarları yap

                        OutputStreamWriter(httpUrlConnection.outputStream).apply {
                            write(requestBody) // Yazılacak requestbody
                            flush()
                        }

                        if(httpUrlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            val response = httpUrlConnection.inputStream.bufferedReader().use { it.readText() }
                            mutableResponse.postValue(JSONObject(response).toResponseModel()) // İstek başarılı ise dönen veriyi model çevir & LiveData gönder
                        }
                        else Log.e("RequestHttpUrlCon", "HttpUrlConnection Failure") // İstek başarısız ise Log yazdır

                        httpUrlConnection.disconnect() // Bağlantıyı kapat

                    }

                }

            }.start()

        }

    }



    fun showData(fragmentViewModel : Any, shareAdapter : ShareAdapter, response : ApiResponseModel) {

        when(getIsRxJava()) {

            true -> Maybe.create { maybe -> showDataProcess(fragmentViewModel, shareAdapter, response, maybe) }
                .subscribeOn(Schedulers.computation()) // İşlem gücü için thread kullan
                .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                .subscribeWith(object : DisposableMaybeObserver<ArrayList<ResponseRecyclerModel>?>() {

                    override fun onSuccess(array: ArrayList<ResponseRecyclerModel>) {

                        // Açık olan fragment & gelen viewmodel hangi sınıfa ait ise veri listesini oraya gönder
                        when(fragmentViewModel) {

                            is OnlineViewModel -> fragmentViewModel.setOnlineList(array)

                            is WatchViewModel -> fragmentViewModel.setWatchList(array)

                        }

                        shareAdapter.update(array) // Fragment'tan gelen RecyclerView adaptöründe veriyi güncelle

                        mutableResponse.value = null // Fragment değişiminde observe tetiklenmemesi için null

                    }

                    override fun onError(e: Throwable) {}

                    override fun onComplete() {}

                })

            false -> Thread { CoroutineScope(Dispatchers.Default).launch { showDataProcess(fragmentViewModel, shareAdapter, response, null) } }.start()

        }

    }

    private fun showDataProcess(fragmentViewModel: Any, shareAdapter: ShareAdapter, response : ApiResponseModel, maybe : MaybeEmitter<ArrayList<ResponseRecyclerModel>>?) {

        Runnable {

            val mapData : HashMap<String, ResponseRecyclerModel> = hashMapOf() // Gelen veriler hashmap içerisinde tutuluyor

            val arrayOnlineList : ArrayList<ResponseRecyclerModel> = arrayListOf() // OnlineViewModel'a giden liste
            val arrayWatchList : ArrayList<ResponseRecyclerModel> = arrayListOf() // WatchViewModel'a giden liste [Db içerisinde saklama sırasına göre düzenlenecek]
            var model : ResponseRecyclerModel // For içerisinde oluşturulan model

            for (i in response.shareList.indices) {

                response.shareList[i].apply {

                    // Gelen response verilere göre ayrıştırılarak modelleniyor
                    model = ResponseRecyclerModel(
                        shareInfo[0]?.let { shareInfo[0] as String },
                        shareInfo[1]?.let { shareInfo[1] as String },
                        shareInfo[2]?.let { shareInfo[2] as Double },
                        shareInfo[3]?.let { shareInfo[3] as Double },
                        shareInfo[4]?.let { shareInfo[4] as Double },
                        shareInfo[5]?.let { shareInfo[5] as Double },
                        shareInfo[6]?.let { shareInfo[6] as Double },
                        shareInfo[7]?.let { shareInfo[7] as Double },
                        shareInfo[8]?.let { shareInfo[8] as String }
                    )

                    model.shareCode?.let { mapData[model.shareCode!!] = model } // Veri Hashmap'e ekleniyor

                    arrayOnlineList.add(model) // OnlineViewModel için ArrayList'e ekleniyor

                }

                if (i == (response.shareList.size - 1)) {

                    when (fragmentViewModel) {

                        /**
                         * OnlineFragment açık ve OnlineViewModel gönderildi
                         * Eğer maybe boş değil ise (RxJava kullanılıyor ise) onSuccess tetikle ve onSuccess içerisine arrayOnlineList'i gönder
                         * Eğer maybe boş ise (Coroutine kullanılıyor ise):
                         * arrayOnlineList'i gönder, RecyclerView adaptör verisini güncelle ve fragment değişiminde observe tetiklenmemesi için LiveData null yap
                         */
                        is OnlineViewModel -> {
                            maybe?.let { maybe.onSuccess(arrayOnlineList) } ?: CoroutineScope(Dispatchers.Main).launch {
                                fragmentViewModel.setOnlineList(arrayOnlineList)
                                shareAdapter.update(arrayOnlineList)
                                mutableResponse.value = null
                            }
                        }

                        /**
                         * Api'den alınan verileri veritabanındaki kayıt sırasına göre düzenleyerek al ve viewmodel'a gönder
                         * Örneğin API'den gelen veri sırası: SASA-THYAO-ASELS-MGRS şeklinde
                         * Kullanıcının veritabanına kayıt sırası: THYAO-MGRS-ASELS-SASA olabilir
                         * arrayWatchList'e database veri sırasına göre hashmap'ten çekerek veriyi ekle
                         */
                        is WatchViewModel -> {

                            for (x in fragmentViewModel.getDbWatchList().indices) {

                                mapData[fragmentViewModel.getDbWatchList()[x].shareCode]?.let { arrayWatchList.add(it) }

                                if(x == (fragmentViewModel.getDbWatchList().size - 1)) {

                                    maybe?.let { maybe.onSuccess(arrayWatchList) } ?: CoroutineScope(Dispatchers.Main).launch {
                                        fragmentViewModel.setWatchList(arrayWatchList)
                                        shareAdapter.update(arrayWatchList)
                                        mutableResponse.value = null
                                    }

                                }

                            }

                        }

                    }

                }

            }

        }.run()

    }

}