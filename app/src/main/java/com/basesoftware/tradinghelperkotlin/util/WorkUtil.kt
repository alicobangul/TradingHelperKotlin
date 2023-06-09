package com.basesoftware.tradinghelperkotlin.util

import com.basesoftware.tradinghelperkotlin.model.*

object WorkUtil {

    const val REQUEST_URL = "https://scanner.tradingview.com/turkey/scan"
    const val BASE_URL = "https://scanner.tradingview.com"
    const val PATH_URL = "/turkey/scan"

    // Tüm verilerin gösterilmesi için kullanılan requestBody'nin model hali
    fun defaultFilterModel() : ApiRequestModel {
        return ApiRequestModel(
            arrayListOf(
                Filter("type","in_range", listOf("stock","dr","fund")),
                Filter("subtype","in_range", listOf("common", "foreign-issuer", "", "etf", "etf,odd", "etf,otc", "etf,cfd")),
                Filter("is_primary","equal",true),
                Filter("active_symbol", "equal", true)
            ),
            Options("tr"),
            arrayListOf("turkey"),
            Symbols(Query(arrayListOf()), arrayListOf()),
            arrayListOf(
                "logoid",
                "name",
                "close",
                "change",
                "change_abs",
                "RSI7",
                "RSI",
                "CCI20",
                "description",
                "type",
                "subtype",
                "update_mode",
                "pricescale",
                "minmov",
                "fractional",
                "minmove2",
                "RSI7[1]",
                "RSI[1]",
                "CCI20[1]",
                "currency",
                "fundamental_currency_code"
            ),
            Sort("market_cap_basic","desc"),
            PriceConversion(false),
            arrayListOf(0,999)
        )
    }

    // Spesifik olarak arama yapmak için requestBody modeline ekstra olarak eklenen model
    fun searchFilterModel(query : String) : ApiRequestModel {

        return defaultFilterModel().apply {

            filter.add((Filter("name", "match", query)))

        }

    }

}