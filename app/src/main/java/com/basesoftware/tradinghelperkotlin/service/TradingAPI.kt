package com.basesoftware.tradinghelperkotlin.service

import com.basesoftware.tradinghelperkotlin.model.ApiResponseModel
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TradingAPI {

    @POST("{path}")
    fun getData(
        @Path("path") path : String,
        @Body parameter : String
    ) : Call<ApiResponseModel>

    @POST("{path}")
    fun getDataRxJava(
        @Path("path") path : String,
        @Body parameter : String
    ) : Single<ApiResponseModel>

}