package com.basesoftware.tradinghelperkotlin.model.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * * shareLogoId => Logo id (String)
 * * shareCode => Hisse ismi-kodu (String)
 * * shareName => Şirket ismi (String)
 * * sharePrice => Hisse fiyatı (Double)
 * * shareChangeAbs => Değişim yüzdesi (Double)
 * * shareChangePrice => Değişen fiyat (Double)
 * * shareRsi7 => RSI 7 (Double)
 * * shareRsi14 => RSI 14 (Double)
 * * shareCci20 => CCI 20 (Double)
 */

@Parcelize
data class ResponseRecyclerModel(
    var shareLogoId : String? = null,
    var shareCode : String? = null,
    var sharePrice : Double? = null,
    var shareChangeAbs : Double? = null,
    var shareChangePrice : Double? = null,
    var shareRsi7 : Double? = null,
    var shareRsi14 : Double? = null,
    var shareCci20 : Double? = null,
    var shareName : String? = null
) : Parcelable
