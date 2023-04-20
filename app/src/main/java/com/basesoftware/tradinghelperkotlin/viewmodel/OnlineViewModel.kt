package com.basesoftware.tradinghelperkotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.tradinghelperkotlin.model.ResponseRecyclerModel

class OnlineViewModel(private val state : SavedStateHandle) : ViewModel() {

    fun getInit() = state.get("init") ?: false
    fun setInit(init : Boolean) = state.set("init", init)

    fun getOnlineList() = state.getLiveData<ArrayList<ResponseRecyclerModel>>("arrayOnlineList", arrayListOf()).value ?: arrayListOf()
    fun setOnlineList(dataList : ArrayList<ResponseRecyclerModel>) = state.set("arrayOnlineList", dataList)

}