package com.basesoftware.tradinghelperkotlin.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.tradinghelperkotlin.databinding.RowShareBinding
import com.basesoftware.tradinghelperkotlin.domain.model.ResponseRecyclerModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class TradingHelperAdapter @Inject constructor() : RecyclerView.Adapter<TradingHelperAdapter.TradingHelperHolder>() {

    private var mDiffer : AsyncListDiffer<ResponseRecyclerModel>

    private lateinit var itemListener : ItemListener

    init {

        val diffCallBack = object : DiffUtil.ItemCallback<ResponseRecyclerModel>() {

            override fun areItemsTheSame(oldItem: ResponseRecyclerModel, newItem: ResponseRecyclerModel): Boolean {

                // Aynı item olduğunu belirtmek için hisse kodu kontrol ediliyor

                return oldItem.shareCode!!.matches(Regex(newItem.shareCode!!))

            }

            override fun areContentsTheSame(oldItem: ResponseRecyclerModel, newItem: ResponseRecyclerModel): Boolean {

                // Eğer verilerden herhangi birisi farklı ise güncelleme gerekiyor [false dönecek]

                return oldItem.shareLogoId.equals(newItem.shareLogoId) &&
                        oldItem.shareCode.equals(newItem.shareCode) &&
                        oldItem.sharePrice?.equals(newItem.sharePrice) ?: true &&
                        oldItem.shareChangeAbs?.equals(newItem.shareChangeAbs) ?: true &&
                        oldItem.shareChangePrice?.equals(newItem.shareChangePrice) ?: true &&
                        oldItem.shareRsi7?.equals(newItem.shareRsi7) ?: true &&
                        oldItem.shareRsi14?.equals(newItem.shareRsi14) ?: true &&
                        oldItem.shareCci20?.equals(newItem.shareCci20) ?: true &&
                        oldItem.shareName?.equals(newItem.shareName) ?: true
            }

        }

        mDiffer = AsyncListDiffer(this, diffCallBack) // AsyncListDiffer'ı oluştur

    }

    class TradingHelperHolder(val binding : RowShareBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradingHelperHolder {

        val binding = RowShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val holder = TradingHelperHolder(binding) // Yer tutucu

        binding.root.setOnClickListener { itemListener.itemClick(mDiffer.currentList[holder.bindingAdapterPosition]) } // Item click aksiyonu

        return holder
    }

    fun itemListener(itemListener : ItemListener) { this.itemListener = itemListener }

    // Veriyi ekrana yaz
    override fun onBindViewHolder(holder: TradingHelperHolder, position: Int) { holder.binding.data = mDiffer.currentList[holder.bindingAdapterPosition] }

    override fun getItemCount(): Int = mDiffer.currentList.size // Veri sayısı

    fun update (dataList : ArrayList<ResponseRecyclerModel>) { mDiffer.submitList(dataList) }

    interface ItemListener { fun itemClick(data : ResponseRecyclerModel) }

}