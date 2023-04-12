package com.basesoftware.tradinghelperkotlin.adapter

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.basesoftware.tradinghelperkotlin.databinding.DialogInfoBinding
import com.basesoftware.tradinghelperkotlin.databinding.RowShareBinding
import com.basesoftware.tradinghelperkotlin.db.WatchDao
import com.basesoftware.tradinghelperkotlin.db.WatchDatabase
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

    private lateinit var recyclerView: RecyclerView

    private lateinit var db : WatchDatabase
    private lateinit var dao : WatchDao

    private lateinit var dialog : Dialog
    private lateinit var dialogBinding : DialogInfoBinding

    class ShareHolder(val binding : RowShareBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
        db = Room.databaseBuilder(recyclerView.context.applicationContext, WatchDatabase::class.java, "TradingHelperKotlin").allowMainThreadQueries().build()
        dao = db.watchDao()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareHolder {

        val binding = RowShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val holder = ShareHolder(binding)

        binding.root.setOnClickListener {

            dialog = Dialog(holder.itemView.context).apply {

                dialogBinding = DialogInfoBinding.inflate(LayoutInflater.from(parent.context))
                window?.setBackgroundDrawable(ColorDrawable(android.R.color.transparent))
                dialogBinding.data = sharedViewModel.getDataList()[holder.bindingAdapterPosition]

                dialogBinding.btnAction.setOnClickListener {
                    when((it as Button).text.toString()){
                        "İzleme listesine ekle" -> addWatch(WatchModel(sharedViewModel.getDataList()[holder.bindingAdapterPosition].shareCode!!))
                        "İzleme listesinden çıkar" -> deleteWatch(WatchModel(sharedViewModel.getDataList()[holder.bindingAdapterPosition].shareCode!!))
                    }
                    hide()
                }

                when(sharedViewModel.getIsRxJava()) {
                    true -> {
                        dao.getWatchDataRxjava(sharedViewModel.getDataList()[holder.bindingAdapterPosition].shareCode!!)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(object : DisposableSingleObserver<WatchModel?>(){

                                override fun onSuccess(t: WatchModel) { checkWatch(true) }

                                override fun onError(e: Throwable) { checkWatch(false) }

                            })


                    }

                    false -> CoroutineScope(Dispatchers.IO).launch {
                        val exist = dao.getWatchDataCoroutines(sharedViewModel.getDataList()[holder.bindingAdapterPosition].shareCode!!)
                        withContext(Dispatchers.Main) { checkWatch(exist.isNotNull()) }
                    }

                }

            }

        }

        return holder
    }

    override fun onBindViewHolder(holder: ShareHolder, position: Int) {

        holder.binding.data = sharedViewModel.getDataList()[holder.bindingAdapterPosition]

    }

    override fun getItemCount(): Int = sharedViewModel.getDataList().size

    private fun addWatch(watchModel: WatchModel) {

        when(sharedViewModel.getIsRxJava()) {
            true -> {
                dao.setWatchDataRxjava(watchModel)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableCompletableObserver(){

                        override fun onComplete() { Log.i("TradingHelperKotlin", "Rxjava - veri ekleme başarılı") }

                        override fun onError(e: Throwable) { Log.i("TradingHelperKotlin", "Rxjava - veri ekleme başarısız") }

                    })
            }
            else -> CoroutineScope(Dispatchers.IO).launch { dao.setWatchDataCoroutines(watchModel) }
        }

    }

    private fun deleteWatch(watchModel: WatchModel) {

        when(sharedViewModel.getIsRxJava()) {
            true -> {
                dao.deleteWatchDataRxjava(watchModel)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableCompletableObserver() {

                        override fun onComplete() { Log.i("TradingHelperKotlin", "Rxjava - veri silme başarılı") }

                        override fun onError(e: Throwable) { Log.i("TradingHelperKotlin", "Rxjava - veri silme başarısız") }

                    })
            }

            false -> { CoroutineScope(Dispatchers.IO).launch { dao.deleteWatchDataCoroutines(watchModel) } }

        }

    }

    private fun checkWatch(exist : Boolean) {
        dialogBinding.btnAction.text = if(exist) "İzleme listesinden çıkar" else "İzleme listesine ekle"
        dialog.apply {
            setContentView(dialogBinding.root)
            show()
        }
    }

}