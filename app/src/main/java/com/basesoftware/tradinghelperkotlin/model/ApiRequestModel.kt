package com.basesoftware.tradinghelperkotlin.model

import com.google.gson.annotations.SerializedName

data class ApiRequestModel(
    @SerializedName("filter") var filter : ArrayList<Filter>,
    @SerializedName("options") var options : Options,
    @SerializedName("markets") var markets : ArrayList<String>,
    @SerializedName("symbols") var symbols : Symbols,
    @SerializedName("columns") var columns : ArrayList<String>,
    @SerializedName("sort") var sort : Sort,
    @SerializedName("price_conversion") var priceConversion : PriceConversion,
    @SerializedName("range") var range : ArrayList<Int>
)

data class Options (@SerializedName("lang") var lang : String)

data class PriceConversion (@SerializedName("to_symbol") var toSymbol : Boolean)

data class Query (@SerializedName("types") var types : ArrayList<String>)

data class Filter (
    @SerializedName("left") var left : String,
    @SerializedName("operation") var operation : String,
    @SerializedName("right") var right : Any?
)

data class Sort (
    @SerializedName("sortBy") var sortBy : String,
    @SerializedName("sortOrder") var sortOrder : String
)

data class Symbols (
    @SerializedName("query") var query : Query,
    @SerializedName("tickers") var tickers : ArrayList<String>
)