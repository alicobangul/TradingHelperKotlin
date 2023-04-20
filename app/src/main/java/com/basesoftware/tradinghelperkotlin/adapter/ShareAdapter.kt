package com.basesoftware.tradinghelperkotlin.adapter

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.tradinghelperkotlin.databinding.DialogInfoBinding
import com.basesoftware.tradinghelperkotlin.databinding.RowShareBinding
import com.basesoftware.tradinghelperkotlin.model.ResponseRecyclerModel
import com.basesoftware.tradinghelperkotlin.model.WatchModel
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.isNotNull
import com.basesoftware.tradinghelperkotlin.viewmodel.SharedViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareAdapter(private val sharedViewModel: SharedViewModel) : RecyclerView.Adapter<ShareAdapter.ShareHolder>() {

    private var adapterDataList : ArrayList<ResponseRecyclerModel> = arrayListOf()

    private lateinit var dialog : Dialog
    private lateinit var dialogBinding : DialogInfoBinding

    class ShareHolder(val binding : RowShareBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareHolder {

        val binding = RowShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val holder = ShareHolder(binding) // Yer tutucu

        // Item'a tıklandığında oluşacak aksiyon
        binding.root.setOnClickListener {

            // Dialog oluştur
            dialog = Dialog(holder.itemView.context).apply {

                dialogBinding = DialogInfoBinding.inflate(LayoutInflater.from(parent.context)) // Dialog binding'i bağla
                window?.setBackgroundDrawable(ColorDrawable(android.R.color.transparent)) // Dialog arkaplan'ı transparan yap
                dialogBinding.data = adapterDataList[holder.bindingAdapterPosition] // DataBinding variable ile Dialog içerisinde gösterilecek veriyi gönder

                dialogBinding.btnAction.setOnClickListener {

                    // Eğer butonda: İzleme listesine ekle yazıyorsa veritabanına ekle İzleme listesinden çıkar yazıyorsa veritabanından sil
                    when((it as Button).text.toString()){

                        "İzleme listesine ekle" -> addWatchDb(WatchModel(adapterDataList[holder.bindingAdapterPosition].shareCode!!))

                        "İzleme listesinden çıkar" -> deleteWatchDb(WatchModel(adapterDataList[holder.bindingAdapterPosition].shareCode!!))

                    }

                    hide() // Dialog kapat

                }

                when(sharedViewModel.getIsRxJava()) {

                    true -> sharedViewModel.dao.getWatchDataRxjava(adapterDataList[holder.bindingAdapterPosition].shareCode!!) // Veritabanında veriyi sorgula
                        .subscribeOn(Schedulers.io()) // I/O thread kullan
                        .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                        .subscribeWith(object : DisposableSingleObserver<WatchModel?>(){

                            override fun onSuccess(t: WatchModel) { showDialog(true) } // Veri mevcut dialog gösterilecek

                            override fun onError(e: Throwable) { showDialog(false) } // Veri yok dialog gösterilmeyecek

                        })

                    false -> CoroutineScope(Dispatchers.IO).launch {
                        val exist = sharedViewModel.dao.getWatchDataCoroutines(adapterDataList[holder.bindingAdapterPosition].shareCode!!) // Veritabanında veriyi sorgula
                        withContext(Dispatchers.Main) { showDialog(exist.isNotNull()) } // Duruma göre dialog göster/gösterme
                    }

                }

            }

        }

        return holder
    }

    override fun onBindViewHolder(holder: ShareHolder, position: Int) {

        holder.binding.data = adapterDataList[holder.bindingAdapterPosition] // Veriyi ekrana yaz

    }

    override fun getItemCount(): Int = adapterDataList.size // RecyclerView item sayısı

    private fun addWatchDb(watchModel: WatchModel) {

        when(sharedViewModel.getIsRxJava()) {

            true -> sharedViewModel.dao.setWatchDataRxjava(watchModel) // Veriyi database'e kaydet
                .subscribeOn(Schedulers.io()) // I/O thread kullan
                .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                .subscribeWith(object : DisposableCompletableObserver(){

                    override fun onComplete() { Log.i("TradingHelperKotlin", "Rxjava - veri ekleme başarılı") }

                    override fun onError(e: Throwable) { Log.i("TradingHelperKotlin", "Rxjava - veri ekleme başarısız") }

                })

            else -> CoroutineScope(Dispatchers.IO).launch { sharedViewModel.dao.setWatchDataCoroutines(watchModel) } // Veriyi database'e kaydet

        }

    }

    private fun deleteWatchDb(watchModel: WatchModel) {

        when(sharedViewModel.getIsRxJava()) {

            true -> sharedViewModel.dao.deleteWatchDataRxjava(watchModel) // Veriyi database'den sil
                .subscribeOn(Schedulers.io()) // I/O thread kullan
                .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                .subscribeWith(object : DisposableCompletableObserver() {

                    override fun onComplete() { Log.i("TradingHelperKotlin", "Rxjava - veri silme başarılı") }

                    override fun onError(e: Throwable) { Log.i("TradingHelperKotlin", "Rxjava - veri silme başarısız") }

                })

            false -> CoroutineScope(Dispatchers.IO).launch { sharedViewModel.dao.deleteWatchDataCoroutines(watchModel) } // Veriyi database'den sil

        }

    }

    private fun showDialog(exist : Boolean) {

        /**
         * Verinin veritabanında mevcut olup/olmamasına göre butonun text'i değiştiriliyor
         * Eğer veri var ise İzleme listesinden çıkar, veri yok ise İzleme listesine ekle
         */
        dialogBinding.btnAction.text = if(exist) "İzleme listesinden çıkar" else "İzleme listesine ekle"

        dialog.apply {

            setContentView(dialogBinding.root) // Görünümü ekle

            show() // Dialog kutusunu göster

        }

    }

    fun update (dataList : ArrayList<ResponseRecyclerModel>) {
        adapterDataList = dataList // Mevcut veri listesini güncelle
        notifyDataSetChanged() // Adaptöre verilerin güncellendiği bildir (DiffUtil veya AsyncListDiffer kullanılabilir)
    }

}