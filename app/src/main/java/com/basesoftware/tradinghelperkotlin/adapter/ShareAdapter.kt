package com.basesoftware.tradinghelperkotlin.adapter

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.tradinghelperkotlin.databinding.DialogInfoBinding
import com.basesoftware.tradinghelperkotlin.databinding.RowShareBinding
import com.basesoftware.tradinghelperkotlin.model.ResponseRecyclerModel

class ShareAdapter(private val arrayDataList : ArrayList<ResponseRecyclerModel>) : RecyclerView.Adapter<ShareAdapter.ShareHolder>() {

    private lateinit var recyclerView: RecyclerView

    class ShareHolder(val binding : RowShareBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareHolder {
        val binding = RowShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ShareHolder(binding)

        binding.root.setOnClickListener {

            Dialog(holder.itemView.context).apply {
                val dialogBinding = DialogInfoBinding.inflate(LayoutInflater.from(parent.context))
                dialogBinding.data = arrayDataList[holder.bindingAdapterPosition]
                setContentView(dialogBinding.root)
                this.window?.setBackgroundDrawable(ColorDrawable(android.R.color.transparent))
                show()
            }

        }

        return holder
    }

    override fun onBindViewHolder(holder: ShareHolder, position: Int) {

        holder.binding.data = arrayDataList[holder.bindingAdapterPosition]

    }

    override fun getItemCount(): Int = arrayDataList.size
}