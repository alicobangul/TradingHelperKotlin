package com.basesoftware.tradinghelperkotlin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.tradinghelperkotlin.databinding.RowShareBinding
import com.basesoftware.tradinghelperkotlin.model.ResponseRecyclerModel

class ShareAdapter(private val arrayDataList : ArrayList<ResponseRecyclerModel>) : RecyclerView.Adapter<ShareAdapter.ShareHolder>() {

    class ShareHolder(val binding : RowShareBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareHolder {
        val binding = RowShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShareHolder(binding)
    }

    override fun onBindViewHolder(holder: ShareHolder, position: Int) {

        holder.binding.data = arrayDataList[holder.bindingAdapterPosition]

    }

    override fun getItemCount(): Int = arrayDataList.size
}