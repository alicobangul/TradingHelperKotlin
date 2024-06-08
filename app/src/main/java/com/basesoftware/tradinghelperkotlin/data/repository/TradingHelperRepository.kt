package com.basesoftware.tradinghelperkotlin.data.repository

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.basesoftware.tradinghelperkotlin.data.model.ApiResponseModel
import com.basesoftware.tradinghelperkotlin.data.remote.TradingAPI
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toJsonObject
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toResponseModel
import com.basesoftware.tradinghelperkotlin.util.Constant
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@ActivityRetainedScoped
class TradingHelperRepository
@Inject constructor(
    private var compositeDisposable: CompositeDisposable,
    private var tradingAPI: TradingAPI,
    private var volleyQueue : RequestQueue,
    private var okHttpClient : OkHttpClient
) {

    private lateinit var okHttpRequest : okhttp3.Request // OkHttp için Request

    private lateinit var httpUrlConnection : HttpURLConnection // HttpUrlConnection için bağlantı

    var error : BehaviorSubject<String> = BehaviorSubject.create()

    var dataList : BehaviorSubject<ApiResponseModel> = BehaviorSubject.create()


    fun requestRetrofit(requestBody : String) {

        compositeDisposable.clear() // Disposable temizlendi

        compositeDisposable.add(

            tradingAPI.getData(Constant.PATH_URL, requestBody)
                .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                .observeOn(AndroidSchedulers.mainThread()) // MainThread'de gözlemlendi
                .subscribeWith(object : DisposableSingleObserver<ApiResponseModel>() {

                    override fun onSuccess(response: ApiResponseModel) { dataList.onNext(response) }

                    override fun onError(t: Throwable) { error.onNext("Retrofit ile veri alma başarısız") }

                })

        )


    }

    fun requestVolley(requestBody: String) {

        compositeDisposable.clear() // Disposable temizlendi

        compositeDisposable.add(

            Single
                .create { single ->
                    volleyQueue.add(
                        JsonObjectRequest( // JSON objesi ile istek atmak için JsonObjectRequest
                            Request.Method.POST, // Kullanılacak Method (GET-POST-DELETE vb)
                            Constant.REQUEST_URL, // İsteğin yapılacağı url
                            requestBody.toJsonObject(), // Request body (Json obje türünde request olduğu için string'i objeye çeviriyoruz)
                            { single.onSuccess(it) }, // Eğer başarılı ise onSuccess tetikle
                            { single.onError(IOException()) } // Eğer başarısız ise onError tetikle
                        )
                    )
                }
                .subscribeOn(Schedulers.io()) // I/O thread kullanıldı
                .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                .subscribeWith(object : DisposableSingleObserver<JSONObject>(){

                    override fun onSuccess(obj: JSONObject) { dataList.onNext(obj.toResponseModel()) } // İşlem başarılı

                    override fun onError(e: Throwable) { error.onNext("Volley ile veri alma başarısız") } // Başarısız ise Log yazdır

                })

        )

    }

    fun requestOkHttp(requestBody: String) {

        compositeDisposable.clear() // Disposable temizle

        okHttpRequest = okhttp3.Request.Builder() // OkHttp için Request Builder oluştur
            .method("POST", requestBody.toRequestBody()) // Kullanılacak method (GET-POST vb) & request body [boş olamaz]
            .url(Constant.REQUEST_URL) // İstek atılacak url
            .build()

        compositeDisposable.add(

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

                        response.body?.let {

                            dataList.onNext(JSONObject(it.string()).toResponseModel()) // Dönen response başarılı ise modele çevir

                            it.close() // Kaynakları bırak

                        }

                    }

                    override fun onError(t : Throwable) { error.onNext("OkHttp ile veri alma başarısız") } // Başarısız ise Log yazdır

                })

        )

    }

    fun requestHttpUrlConnection(requestBody: String) {

        compositeDisposable.clear() // Disposable temizle

        compositeDisposable.add(

            Single
                .fromCallable {

                    httpUrlConnection = URL(Constant.REQUEST_URL).openConnection() as HttpURLConnection // Url'i kullanarak bağlantıyı aç

                    // Bağlantıya ait ayarlar
                    httpUrlConnection.apply {

                        requestMethod = "POST" // Kullanılacak method
                        useCaches = false // Cache kullanılsın mı
                        allowUserInteraction = false // userInteraction değeri
                        doInput = true // Giriş/çıkış için url kullanımı
                        doOutput = true // Giriş/çıkış için url kullanımı

                    }

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
                        dataList.onNext(JSONObject(response).toResponseModel()) // Gelen veriyi modele çevir
                        httpUrlConnection.disconnect()  // Bağlantıyı kapat
                    }

                    override fun onError(e: Throwable) {
                        error.onNext("HttpURLConnection ile veri alma başarısız") // Başarısız ise Log yazdır
                        httpUrlConnection.disconnect() // Bağlantıyı kapat
                    }

                })

        )

    }

}