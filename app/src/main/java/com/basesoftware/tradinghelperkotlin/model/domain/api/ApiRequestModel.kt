package com.basesoftware.tradinghelperkotlin.model.domain.api

import com.google.gson.Gson

data class ApiRequestModel (
    val columns: List<String> = listOf(
        "name",
        "description",
        "logoid",
        "update_mode",
        "type",
        "typespecs",
        "close",
        "pricescale",
        "minmov",
        "fractional",
        "minmove2",
        "currency",
        "change",
        "change_abs",
        "RSI7",
        "RSI",
        "CCI20",
        "exchange"),
    var filter: List<Filter> = arrayListOf(Filter()),
    val options: Options = Options(),
    val range: List<Int> = listOf(0, 1000),
    val sort : Sort = Sort(),
    val markets: List<String> = listOf("turkey")
) {

    fun toJsonText(searchText: String) : String {

        if(searchText.isNotEmpty()) filter[0].right = searchText

        return Gson().toJson(this)

    }

    data class Filter(val left : String = "name", val operation : String = "match", var right : String = "")

    data class Sort(val sortBy : String = "RSI7", val sortOrder : String = "desc")
    data class Options(val lang: String = "tr")

}