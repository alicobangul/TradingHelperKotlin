package com.basesoftware.tradinghelperkotlin.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.tradinghelperkotlin.model.ResponseRecyclerModel

class SharedViewModel(private val state : SavedStateHandle) : ViewModel() {

    fun getInit() = state.get("init") ?: false
    fun setInit(init : Boolean) = state.set("init", init)

    fun getSelectedLibrary() = state.get("selectedLibrary") ?: "Retrofit"
    fun setSelectedLibrary(library : String) = state.set("selectedLibrary", library)

    fun getIsRxJava() = state.get("isRxjava") ?: true
    fun setIsRxJava(isRxjava : Boolean) = state.set("isRxjava", isRxjava)

    private val arrayDataList : MutableLiveData<ArrayList<ResponseRecyclerModel>> = state.getLiveData("arrayDataList", arrayListOf())
    fun getDataList() = arrayDataList.value ?: arrayListOf()

}