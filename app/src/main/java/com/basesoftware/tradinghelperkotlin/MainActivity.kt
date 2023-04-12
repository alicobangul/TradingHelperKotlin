package com.basesoftware.tradinghelperkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.basesoftware.tradinghelperkotlin.adapter.ShareAdapter
import com.basesoftware.tradinghelperkotlin.databinding.ActivityMainBinding
import com.basesoftware.tradinghelperkotlin.databinding.DialogLibraryBinding
import com.basesoftware.tradinghelperkotlin.model.*
import com.basesoftware.tradinghelperkotlin.service.TradingAPI
import com.basesoftware.tradinghelperkotlin.util.WorkUtil.defaultFilterModel
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.connectionSettings
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toJsonObject
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toJsonText
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toResponseModel
import com.basesoftware.tradinghelperkotlin.util.WorkUtil.searchFilterModel
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.GsonBuilder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val requesturl = "https://scanner.tradingview.com/turkey/scan"
    private val baseurl = "https://scanner.tradingview.com"
    private val pathurl = "/turkey/scan"

    private var library = "retrofit"
    private var isRxJava = true

    private lateinit var compositeDisposable : CompositeDisposable

    private lateinit var retrofit : Retrofit
    private lateinit var retrofitAPI : TradingAPI

    private lateinit var okHttpClient : OkHttpClient
    private lateinit var okHttpRequest : Request

    private lateinit var volleyQueue : RequestQueue

    private lateinit var httpUrlConnection : HttpURLConnection

    private lateinit var shareAdapter : ShareAdapter

    private lateinit var arrayDataList : ArrayList<ResponseRecyclerModel>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        AlertDialog.Builder(this).apply {
            setMessage("Thread yönetimi için hangisi kullanılsın?")
            setCancelable(false)
            setPositiveButton("RXJAVA") { _, _ -> isRxJava = true}
            setNegativeButton("COROUTINES") { _, _ -> isRxJava = false}
            setOnDismissListener {

                AlertDialog.Builder(this@MainActivity).apply {
                    setMessage("API yönetimi için hangisi kullanılsın?")
                    val alertBinding = DialogLibraryBinding.inflate(layoutInflater)
                    setView(alertBinding.root)
                    setCancelable(false)
                    setPositiveButton("SEÇ") { _, _ ->
                        alertBinding.apply {
                            library = rdGroup.findViewById<RadioButton>(rdGroup.checkedRadioButtonId).text.toString()
                            when {
                                radioRetrofit.isChecked -> initRetrofit()
                                radioVolley.isChecked -> initVolley()
                                radioOkHttp.isChecked -> initOkHttp()
                                radioHttpUrlConnection.isChecked -> requestHttpUrlConnection(defaultFilterModel().toJsonText()) // initialize gerektirmiyor
                            }
                        }
                    }
                    show()
                }

            }
            show()
        }

    }



    private fun init() {

        compositeDisposable = CompositeDisposable()

        arrayDataList = arrayListOf()

        shareAdapter = ShareAdapter(arrayDataList)

        binding.apply {

            recyclerData.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = shareAdapter
            }

            txtSearch.setOnEditorActionListener { view, action, _ ->

                if (action == EditorInfo.IME_ACTION_SEARCH) (view as TextInputEditText).text?.checkEmpty()
                true
            }

            txtSearchLayout.setEndIconOnClickListener { txtSearch.text?.checkEmpty() }

            swipeData.setOnRefreshListener {

                txtSearch.text?.clear()

                txtSearch.clearFocus()

                swipeData.isRefreshing = false

                when(library) {
                    "Retrofit" -> requestRetrofit(defaultFilterModel().toJsonText())
                    "Volley" -> requestVolley(defaultFilterModel().toJsonText())
                    "OkHttp" -> requestOkHttp(defaultFilterModel().toJsonText())
                    "HttpUrlConnection" -> requestHttpUrlConnection(defaultFilterModel().toJsonText())
                }

            }

        }

    }



    private fun initRetrofit() {

        retrofit = Retrofit.Builder()
            .baseUrl(baseurl)
            .addConverterFactory(ScalarsConverterFactory.create()) // String şeklinde body yollamak için
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        retrofitAPI = retrofit.create(TradingAPI::class.java)

        requestRetrofit(defaultFilterModel().toJsonText())

    }

    private fun requestRetrofit(requestBody : String) {

        compositeDisposable.clear()

        when(isRxJava) {

            true -> compositeDisposable.add(
                retrofitAPI.getDataRxJava(pathurl , requestBody)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<ApiResponseModel>(){
                        override fun onSuccess(response: ApiResponseModel) { showData(response) }
                        override fun onError(t: Throwable) { Log.e("RequestRetrofit", "Retrofit-RxJava Failure") }
                    })
            )

            false -> Thread {

                CoroutineScope(Dispatchers.IO).launch {

                    runCatching {

                        retrofitAPI.getData(pathurl, requestBody).enqueue(object : retrofit2.Callback<ApiResponseModel> {
                            override fun onResponse(call: retrofit2.Call<ApiResponseModel>, response: retrofit2.Response<ApiResponseModel>) {

                                when(response.isSuccessful) {
                                    true -> response.body()?.let { showData(it) }
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



    private fun initVolley() {

        volleyQueue = Volley.newRequestQueue(this@MainActivity)

        requestVolley(defaultFilterModel().toJsonText())

    }

    private fun requestVolley(requestBody: String) {

        compositeDisposable.clear()

        when(isRxJava) {

            true -> compositeDisposable.add(
                Single
                    .create { single ->
                        volleyQueue.add(
                            JsonObjectRequest(
                                com.android.volley.Request.Method.POST,
                                requesturl,
                                requestBody.toJsonObject(),
                                { single.onSuccess(it) },
                                { single.onError(IOException()) }
                            )
                        )
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<JSONObject>(){

                        override fun onSuccess(obj: JSONObject) { showData(obj.toResponseModel()) }

                        override fun onError(e: Throwable) { Log.e("RequestVolley", "Volley-RxJava Failure") }

                    })
            )

            false -> Thread {

                CoroutineScope(Dispatchers.IO).launch {

                    runCatching {

                        val request = JsonObjectRequest(com.android.volley.Request.Method.POST, requesturl, requestBody.toJsonObject(),
                            { showData(it.toResponseModel()) },
                            { Log.e("RequestVolley", "Volley Failure") }
                        )

                        volleyQueue.add(request)
                    }

                }

            }.start()

        }

    }



    private fun initOkHttp() {


        okHttpClient = OkHttpClient()

        requestOkHttp(defaultFilterModel().toJsonText())

    }

    private fun requestOkHttp(requestBody: String) {

        compositeDisposable.clear()

        okHttpRequest = Request.Builder()
            .method("POST", requestBody.toRequestBody())
            .url(requesturl)
            .build()

        when(isRxJava) {

            true -> compositeDisposable.add(
                Single
                    .fromCallable {
                        // Eğer throw atılmazsa (sadece response[execute] return ederse) hata olsa dahi onsuccess dönüyor
                        val response = okHttpClient.newCall(okHttpRequest).execute()
                        return@fromCallable if (response.isSuccessful) response else throw IOException()
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<Response>(){

                        override fun onSuccess(response : Response) {
                            response.body?.let { showData(JSONObject(it.string()).toResponseModel()) }
                            response.body?.close()
                        }

                        override fun onError(t : Throwable) { Log.e("RequestOkHttp", "OkHttp-RxJava Failure") }

                    })
            )

            false -> Thread {

                CoroutineScope(Dispatchers.IO).launch {

                    runCatching {

                        okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {

                            override fun onResponse(call: Call, response: Response) {

                                when(response.isSuccessful) {
                                    true -> response.body?.let { showData(JSONObject(it.string()).toResponseModel()) }
                                    else -> Log.e("RequestOkHttp", "OkHttp Failure in onResponse")
                                }

                                response.body?.close() // Bağlantıyı kapat

                            }

                            override fun onFailure(call: Call, e: IOException) { Log.e("RequestOkHttp", "OkHttp Failure") }

                        })

                    }

                }

            }.start()

        }

    }



    private fun requestHttpUrlConnection(requestBody: String) {

        compositeDisposable.clear()

        when(isRxJava) {

            true -> compositeDisposable.add(
                Single
                    .fromCallable {

                        httpUrlConnection = URL(requesturl).openConnection() as HttpURLConnection
                        httpUrlConnection.connectionSettings()

                        OutputStreamWriter(httpUrlConnection.outputStream).apply {
                            write(requestBody)
                            flush()
                        }

                        return@fromCallable when(httpUrlConnection.responseCode) {
                            200 -> httpUrlConnection.inputStream.bufferedReader().use { it.readText() }
                            else -> throw IOException()
                        }

                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<String>() {
                        override fun onSuccess(response : String) {
                            showData(JSONObject(response).toResponseModel()).also {  httpUrlConnection.disconnect() }
                        }

                        override fun onError(e: Throwable) {
                            Log.e("RequestHttpUrlCon", "HttpUrlConnection-RxJava Failure")
                            httpUrlConnection.disconnect()
                        }
                    })
            )

            else -> Thread {

                CoroutineScope(Dispatchers.IO).launch {

                    runCatching {

                        httpUrlConnection = URL(requesturl).openConnection() as HttpURLConnection
                        httpUrlConnection.connectionSettings()

                        OutputStreamWriter(httpUrlConnection.outputStream).apply {
                            write(requestBody)
                            flush()
                        }

                        if(httpUrlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            val response = httpUrlConnection.inputStream.bufferedReader().use { it.readText() }
                            showData(JSONObject(response).toResponseModel()) // Dönen veri modeli
                        }
                        else Log.e("RequestHttpUrlCon", "HttpUrlConnection Failure")

                        httpUrlConnection.disconnect()

                    }

                }

            }.start()

        }



    }




    private fun Editable.checkEmpty() {

        val checkSearchText = this.toString().replace(Regex(" "), "")

        binding.txtSearch.setText(checkSearchText)

        if(checkSearchText.isNotEmpty()) {

            when(library) {
                "Retrofit" -> requestRetrofit(searchFilterModel(checkSearchText).toJsonText())
                "Volley" -> requestVolley(searchFilterModel(checkSearchText).toJsonText())
                "OkHttp" -> requestOkHttp(searchFilterModel(checkSearchText).toJsonText())
                "HttpUrlConnection" -> requestHttpUrlConnection(searchFilterModel(checkSearchText).toJsonText())
            }

        }
    }

    private fun showData(response : ApiResponseModel) {

        Thread {

            CoroutineScope(Dispatchers.Default).launch {

                arrayDataList.clear()

                for (i in response.shareList.indices) {

                    response.shareList[i].apply {
                        arrayDataList.add(
                            ResponseRecyclerModel(
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
                        )
                    }

                    if (i == (response.shareList.size - 1)) withContext(Dispatchers.Main) { shareAdapter.notifyDataSetChanged() }

                }
            }

        }.start()

    }

}