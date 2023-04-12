package com.basesoftware.tradinghelperkotlin.view

import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.basesoftware.tradinghelperkotlin.adapter.ShareAdapter
import com.basesoftware.tradinghelperkotlin.databinding.DialogLibraryBinding
import com.basesoftware.tradinghelperkotlin.databinding.FragmentOnlineBinding
import com.basesoftware.tradinghelperkotlin.model.ApiResponseModel
import com.basesoftware.tradinghelperkotlin.model.ResponseRecyclerModel
import com.basesoftware.tradinghelperkotlin.service.TradingAPI
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.connectionSettings
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toJsonObject
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toJsonText
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toResponseModel
import com.basesoftware.tradinghelperkotlin.util.WorkUtil
import com.basesoftware.tradinghelperkotlin.util.WorkUtil.BASE_URL
import com.basesoftware.tradinghelperkotlin.util.WorkUtil.PATH_URL
import com.basesoftware.tradinghelperkotlin.util.WorkUtil.REQUEST_URL
import com.basesoftware.tradinghelperkotlin.util.WorkUtil.defaultFilterModel
import com.basesoftware.tradinghelperkotlin.viewmodel.SharedViewModel
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

class OnlineFragment : Fragment() {

    private var _binding : FragmentOnlineBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private lateinit var compositeDisposable : CompositeDisposable

    private lateinit var retrofit : Retrofit
    private lateinit var retrofitAPI : TradingAPI

    private lateinit var okHttpClient : OkHttpClient
    private lateinit var okHttpRequest : okhttp3.Request

    private lateinit var volleyQueue : RequestQueue

    private lateinit var httpUrlConnection : HttpURLConnection

    private lateinit var shareAdapter : ShareAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnlineBinding.inflate(inflater,container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        if(!sharedViewModel.getInit()) {

            AlertDialog.Builder(requireActivity()).apply {
                setMessage("Thread yönetimi için hangisi kullanılsın?")
                setCancelable(false)
                setPositiveButton("RXJAVA") { _, _ -> sharedViewModel.setIsRxJava(true)}
                setNegativeButton("COROUTINES") { _, _ -> sharedViewModel.setIsRxJava(false)}
                setOnDismissListener {

                    AlertDialog.Builder(requireActivity()).apply {
                        setMessage("API yönetimi için hangisi kullanılsın?")
                        val alertBinding = DialogLibraryBinding.inflate(layoutInflater)
                        setView(alertBinding.root)
                        setCancelable(false)
                        setPositiveButton("SEÇ") { _, _ ->
                            alertBinding.apply {
                                sharedViewModel.setSelectedLibrary(rdGroup.findViewById<RadioButton>(rdGroup.checkedRadioButtonId).text.toString())
                                sharedViewModel.setInit(true)
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

    }

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }


    private fun init() {

        compositeDisposable = CompositeDisposable()

        shareAdapter = ShareAdapter(sharedViewModel.getDataList())

        binding.apply {

            recyclerData.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireActivity())
                adapter = shareAdapter
                adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
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

                when(sharedViewModel.getSelectedLibrary()) {
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
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create()) // String şeklinde body yollamak için
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        retrofitAPI = retrofit.create(TradingAPI::class.java)

        requestRetrofit(defaultFilterModel().toJsonText())

    }

    private fun requestRetrofit(requestBody : String) {

        compositeDisposable.clear()

        when(sharedViewModel.getIsRxJava()) {

            true -> compositeDisposable.add(
                retrofitAPI.getDataRxJava(PATH_URL , requestBody)
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

                        retrofitAPI.getData(PATH_URL, requestBody).enqueue(object : retrofit2.Callback<ApiResponseModel> {
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

        volleyQueue = Volley.newRequestQueue(requireContext())

        requestVolley(defaultFilterModel().toJsonText())

    }

    private fun requestVolley(requestBody: String) {

        compositeDisposable.clear()

        when(sharedViewModel.getIsRxJava()) {

            true -> compositeDisposable.add(
                Single
                    .create { single ->
                        volleyQueue.add(
                            JsonObjectRequest(
                                Request.Method.POST,
                                REQUEST_URL,
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

                        val request = JsonObjectRequest(
                            Request.Method.POST, REQUEST_URL, requestBody.toJsonObject(),
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

        okHttpRequest = okhttp3.Request.Builder()
            .method("POST", requestBody.toRequestBody())
            .url(REQUEST_URL)
            .build()

        when(sharedViewModel.getIsRxJava()) {

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

        when(sharedViewModel.getIsRxJava()) {

            true -> compositeDisposable.add(
                Single
                    .fromCallable {

                        httpUrlConnection = URL(REQUEST_URL).openConnection() as HttpURLConnection
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

                        httpUrlConnection = URL(REQUEST_URL).openConnection() as HttpURLConnection
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

            when(sharedViewModel.getSelectedLibrary()) {
                "Retrofit" -> requestRetrofit(WorkUtil.searchFilterModel(checkSearchText).toJsonText())
                "Volley" -> requestVolley(WorkUtil.searchFilterModel(checkSearchText).toJsonText())
                "OkHttp" -> requestOkHttp(WorkUtil.searchFilterModel(checkSearchText).toJsonText())
                "HttpUrlConnection" -> requestHttpUrlConnection(
                    WorkUtil.searchFilterModel(
                        checkSearchText
                    ).toJsonText())
            }

        }
    }

    private fun showData(response : ApiResponseModel) {

        Thread {

            CoroutineScope(Dispatchers.Default).launch {

                sharedViewModel.getDataList().clear()

                for (i in response.shareList.indices) {

                    response.shareList[i].apply {
                        sharedViewModel.getDataList().add(
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