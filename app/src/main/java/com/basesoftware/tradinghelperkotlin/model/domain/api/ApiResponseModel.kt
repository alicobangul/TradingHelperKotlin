package com.basesoftware.tradinghelperkotlin.model.domain.api

import com.google.gson.annotations.SerializedName

/**
 * * totalDataCount => Gelen veri sayısı
 * * sharedList => Gelen veri listesi
 */
data class ApiResponseModel (
    @SerializedName("totalCount") var totalDataCount : Int,
    @SerializedName("data") var shareList : List<ShareData>
)

/**
 * * s => "BORSA:HISSE"
 * * d.0 => Hisse kodu (String)
 * * d.1 => Hisse ismi (String)
 * * d.2 => Logo id (String)
 * * d.6 => Hisse fiyatı (Double)
 * * d.12 => Hisse değişim oranı % (Double)
 * * d.13 => Hisse değişen fiyat (Double)
 * * d.14 => RSI 7 (Double)
 * * d.15 => RSI 14 (Double)
 * * d.16 => CCI 20 (Double)
 */
data class ShareData (
    @SerializedName("s") var shareName : String,
    @SerializedName("d") var shareInfo : List<Any?>
)