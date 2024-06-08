package com.basesoftware.tradinghelperkotlin.di

import android.content.Context
import com.android.volley.toolbox.Volley
import com.basesoftware.tradinghelperkotlin.data.remote.TradingAPI
import com.basesoftware.tradinghelperkotlin.util.Constant
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.reactivex.rxjava3.disposables.CompositeDisposable
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

@Module
@InstallIn(ActivityRetainedComponent::class)
class TradingHelperModule {

    @ActivityRetainedScoped
    @Provides
    fun compositeDisposableProvider() = CompositeDisposable()

    @ActivityRetainedScoped
    @Provides
    fun tradingAPIProvider() : TradingAPI {

        return Retrofit.Builder()
            .baseUrl(Constant.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create()) // String şeklinde body yollamak için ScalarsConverterFactory
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
            .create(TradingAPI::class.java)

    }

    @ActivityRetainedScoped
    @Provides
    fun volleyProvider(@ApplicationContext context : Context) = Volley.newRequestQueue(context)

    @ActivityRetainedScoped
    @Provides
    fun okHttpClientProvider() = OkHttpClient()

    @ActivityRetainedScoped
    @Provides
    fun okHttpRequestBuilderProvider() = Request.Builder()

}