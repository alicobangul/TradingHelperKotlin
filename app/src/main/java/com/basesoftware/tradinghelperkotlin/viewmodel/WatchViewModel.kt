package com.basesoftware.tradinghelperkotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.tradinghelperkotlin.model.ResponseRecyclerModel
import com.basesoftware.tradinghelperkotlin.model.WatchModel

class WatchViewModel(private val state : SavedStateHandle) : ViewModel() {

    fun getInit() = state.get("init") ?: false
    fun setInit(init : Boolean) = state.set("init", init)

    fun getDbWatchList() = state.getLiveData<ArrayList<WatchModel>>("arrayDbWatchList", arrayListOf()).value ?: arrayListOf()
    fun setDbWatchList(dataList : ArrayList<WatchModel>) = state.set("arrayDbWatchList", dataList)

    fun getWatchList() = state.getLiveData<ArrayList<ResponseRecyclerModel>>("arrayWatchList", arrayListOf()).value ?: arrayListOf()
    fun setWatchList(dataList : ArrayList<ResponseRecyclerModel>) = state.set("arrayWatchList", dataList)

}