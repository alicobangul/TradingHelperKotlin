package com.basesoftware.tradinghelperkotlin.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.tradinghelperkotlin.adapter.ShareAdapter
import com.basesoftware.tradinghelperkotlin.databinding.FragmentWatchBinding
import com.basesoftware.tradinghelperkotlin.model.WatchModel
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.settings
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toJsonText
import com.basesoftware.tradinghelperkotlin.util.WorkUtil
import com.basesoftware.tradinghelperkotlin.viewmodel.SharedViewModel
import com.basesoftware.tradinghelperkotlin.viewmodel.WatchViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class WatchFragment : Fragment() {

    private var _binding : FragmentWatchBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel by activityViewModels<SharedViewModel> { SharedViewModel.provideFactory(requireActivity(), this)}
    private val watchViewModel : WatchViewModel by viewModels()

    private lateinit var shareAdapter : ShareAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

        listener()

        initDbControl()

    }

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }

    override fun onDetach() {

        watchViewModel.setInit(false) // Init false yapıldı fragment tekrar açıldığında Database'e ve API'ye yeni istek atılacak

        super.onDetach()
    }



    private fun initialize() {

        shareAdapter = ShareAdapter(sharedViewModel) // Adaptör tanımlandı
        if(watchViewModel.getInit()) shareAdapter.update(watchViewModel.getWatchList()) // Eğer fragment arka plandan geri açıldı ise (kill) state'ten listeyi al

        binding.recyclerWatch.settings(requireActivity(), shareAdapter) // RecyclerView ayarları yapıldı

    }

    private fun listener() {

        binding.swipeWatch.setOnRefreshListener {

            binding.swipeWatch.isRefreshing = false // SwipeLayout animasyonunu kapat

            sharedViewModel.request(WorkUtil.defaultFilterModel().toJsonText()) // API'ye istek yapıldı

        }

        sharedViewModel.mutableResponse.observe(viewLifecycleOwner) { response ->

            response?.let { sharedViewModel.showData(watchViewModel, shareAdapter, it) } // Bir istek yapıldı ve response LiveData'ya gönderildi

        }

    }

    private fun initDbControl() {

        when(watchViewModel.getInit()) {

            true -> shareAdapter.update(watchViewModel.getWatchList()) // Fragment arka plandan geri çağırıldı (kill) state'ten listeyi al

            false -> {

                watchViewModel.setInit(true) // WatchFragment'ın açıldığı belirtildi, böylece arka plandan geri çağırılır ise state kullanılacak

                when(sharedViewModel.getIsRxJava()) {

                    true -> {

                        sharedViewModel.dao.getWatchListRxjava()
                            .subscribeOn(Schedulers.io()) // I'O thread kullanılacak
                            .observeOn(AndroidSchedulers.mainThread()) // MainThread gözlemledi
                            .subscribeWith(object : DisposableSingleObserver<List<WatchModel>>() {

                                override fun onSuccess(list: List<WatchModel>) {

                                    // Gelen listeyi ViewModel'a gönder ve API'ye istek yap
                                    list.updateDbWatchList().also { sharedViewModel.request(WorkUtil.defaultFilterModel().toJsonText()) }

                                }

                                override fun onError(e: Throwable) { Log.e("WatchFragment-FirstDb", "Database kontrolü yapılamadı") } // İşlem başarısız Log yazdır

                            })
                    }

                    false -> {

                        CoroutineScope(Dispatchers.IO).launch {

                            val list = sharedViewModel.dao.getWatchListCoroutines() // Database'den verileri al

                            withContext(Dispatchers.Main) {

                                // Gelen listeyi ViewModel'a gönder ve API'ye istek yap
                                list.updateDbWatchList().also { sharedViewModel.request(WorkUtil.defaultFilterModel().toJsonText()) }

                            }

                        }

                    }

                }

            }

        }

    }

    // Liste varsa & dolu ise viewModel'da db watch listesini update et
    private fun List<WatchModel>?.updateDbWatchList() = this?.let { list -> if(list.isNotEmpty()) watchViewModel.setDbWatchList(ArrayList(list)) }


}