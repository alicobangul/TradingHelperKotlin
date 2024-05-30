package com.basesoftware.tradinghelperkotlin.service

import com.basesoftware.tradinghelperkotlin.model.domain.api.ApiResponseModel
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