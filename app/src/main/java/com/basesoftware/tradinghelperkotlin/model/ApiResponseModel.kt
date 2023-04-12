package com.basesoftware.tradinghelperkotlin.model

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
 * * d.0 => Logo id (String)
 * * d.1 => Hisse ismi-kodu (String)
 * * d.2 => Hisse fiyatı (Double)
 * * d.3 => Değişim yüzdesi (Double)
 * * d.4 => Değişen fiyat (Double)
 * * d.5 => RSI 7 (Double)
 * * d.6 => RSI 14 (Double)
 * * d.7 => CCI 20 (Double)
 * * d.8 => Şirket ismi (String)
 */
data class ShareData (
    @SerializedName("s") var shareName : String,
    @SerializedName("d") var shareInfo : List<Any?>
)
