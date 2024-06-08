package com.basesoftware.tradinghelperkotlin.data.remote

import com.basesoftware.tradinghelperkotlin.data.model.ApiResponseModel
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TradingAPI {

    @POST("{path}")
    fun getData(
        @Path("path") path : String, // API tarafındaki yol
        @Body parameter : String // Yollanan requestBody
    ) : Single<ApiResponseModel>

}