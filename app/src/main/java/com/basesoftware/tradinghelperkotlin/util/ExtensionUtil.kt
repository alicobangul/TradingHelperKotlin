package com.basesoftware.tradinghelperkotlin.util

import com.basesoftware.tradinghelperkotlin.data.model.ApiResponseModel
import com.google.gson.Gson
import org.json.JSONObject

object ExtensionUtil {

    fun String.toJsonObject() : JSONObject = JSONObject(this) // JSON stilindeki stringleri JSON objesine çevir [Volley JsonObjectRequest için gerekli]

    fun JSONObject.toResponseModel() : ApiResponseModel = Gson().fromJson(this.toString(), ApiResponseModel::class.java) // Gelen JSONObject türündeki veriyi modele çevir

}