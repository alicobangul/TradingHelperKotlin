package com.basesoftware.tradinghelperkotlin.view

import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.tradinghelperkotlin.adapter.ShareAdapter
import com.basesoftware.tradinghelperkotlin.databinding.DialogLibraryBinding
import com.basesoftware.tradinghelperkotlin.databinding.FragmentOnlineBinding
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.settings
import com.basesoftware.tradinghelperkotlin.util.ExtensionUtil.toJsonText
import com.basesoftware.tradinghelperkotlin.util.WorkUtil
import com.basesoftware.tradinghelperkotlin.util.WorkUtil.searchFilterModel
import com.basesoftware.tradinghelperkotlin.viewmodel.OnlineViewModel
import com.basesoftware.tradinghelperkotlin.viewmodel.SharedViewModel
import com.google.android.material.textfield.TextInputEditText

class OnlineFragment : Fragment() {

    private var _binding : FragmentOnlineBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel by activityViewModels<SharedViewModel> { SharedViewModel.provideFactory(requireActivity(), this)}
    private val onlineViewModel : OnlineViewModel by viewModels()

    private lateinit var shareAdapter : ShareAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnlineBinding.inflate(inflater,container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialize()

        listener()

        checkAppInit()

    }

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }

    override fun onDetach() {

        onlineViewModel.setInit(false) // Init false yapıldı fragment tekrar açıldığında yeni istek atılacak

        super.onDetach()

    }



    private fun initialize() {

        shareAdapter = ShareAdapter(sharedViewModel) // Adaptör tanımlandı
        if(onlineViewModel.getInit()) shareAdapter.update(onlineViewModel.getOnlineList()) // Eğer fragment arka plandan geri açıldı ise (kill) state'ten listeyi al

        binding.recyclerOnline.settings(requireActivity(), shareAdapter) // RecyclerView ayarları yapıldı

    }

    private fun listener() {

        binding.apply {

            txtSearch.setOnEditorActionListener { view, action, _ ->

                if (action == EditorInfo.IME_ACTION_SEARCH) (view as TextInputEditText).text?.checkEmpty() // Klavyeden search'e tıklanırsa
                true
            }

            txtSearchLayout.setEndIconOnClickListener { txtSearch.text?.checkEmpty() }

            swipeOnline.setOnRefreshListener {

                txtSearch.text?.clear() // EditText içerisini temizle

                txtSearch.clearFocus() // Fokusu kaldır

                swipeOnline.isRefreshing = false // SwipeLayout animasyonunu kapat

                sharedViewModel.request(WorkUtil.defaultFilterModel().toJsonText()) // API'ye istek yapıldı

            }

        }

        sharedViewModel.mutableResponse.observe(viewLifecycleOwner) { response ->

            response?.let { sharedViewModel.showData(onlineViewModel, shareAdapter, it) } // Bir istek yapıldı ve response LiveData'ya gönderildi

        }

    }

    private fun checkAppInit() {

        // Uygulama ilk kez açılıyor kullanıcıya kullanmak istediği thread yöntemi & kütüphane sorulacak
        if(!sharedViewModel.getInit()) {

            AlertDialog.Builder(requireActivity()).apply {
                setMessage("Thread yönetimi için hangisi kullanılsın?")
                setCancelable(false)
                setPositiveButton("RXJAVA") { _, _ -> sharedViewModel.setIsRxJava(true)}
                setNegativeButton("COROUTINES") { _, _ -> sharedViewModel.setIsRxJava(false)}
                setOnDismissListener {

                    AlertDialog.Builder(requireActivity()).apply {
                        setMessage("API yönetimi için hangisi kullanılsın?")
                        val alertBinding = DialogLibraryBinding.inflate(layoutInflater)
                        setView(alertBinding.root)
                        setCancelable(false)
                        setPositiveButton("SEÇ") { _, _ ->

                            sharedViewModel.apply {

                                // Uygulamanın açıldığı ve kütüphanelerin ayarlandığı belirtildi
                                setInit(true)

                                // Daha sonra tekrar aynı kütüphaneyi kullanmak için, seçilen kütüphane kaydedildi
                                setSelectedLibrary(alertBinding.rdGroup.findViewById<RadioButton>(alertBinding.rdGroup.checkedRadioButtonId).text.toString())

                                request(WorkUtil.defaultFilterModel().toJsonText()) // API'ye istek yapıldı

                            }

                        }
                        show()
                    }

                }
                show()
            }

        }

        // Uygulama açıldı ama OnlineFragment ilk kez açıldı veya açıldı-değiştirildi ise API'ye istek yap
        else if(!onlineViewModel.getInit()) sharedViewModel.request(WorkUtil.defaultFilterModel().toJsonText())

        onlineViewModel.setInit(true) // OnlineFragment'ın açıldığı belirtildi, böylece arka plandan geri çağırılır ise state kullanılacak

    }


    private fun Editable.checkEmpty() {

        val checkSearchText = this.toString().replace(Regex(" "), "") // Metindeki boşlukları kaldır

        binding.txtSearch.setText(checkSearchText) // Boşlukları kaldırılmış metni EditText'e geri gönder

        if(checkSearchText.isNotEmpty()) sharedViewModel.request(searchFilterModel(checkSearchText).toJsonText()) // Eğer EditText boş değil ise API'ye istek yap

    }

}