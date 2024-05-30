package com.basesoftware.tradinghelperkotlin.view

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.tradinghelperkotlin.adapter.TradingHelperAdapter
import com.basesoftware.tradinghelperkotlin.databinding.ActivityMainBinding
import com.basesoftware.tradinghelperkotlin.databinding.DialogInfoBinding
import com.basesoftware.tradinghelperkotlin.databinding.SelectedLibraryBinding
import com.basesoftware.tradinghelperkotlin.model.domain.ResponseRecyclerModel
import com.basesoftware.tradinghelperkotlin.viewmodel.TradingViewModel
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), TradingHelperAdapter.ItemListener {

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var tradingHelperAdapter : TradingHelperAdapter

    private val viewmodel by viewModels<TradingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialize() // Initialize işlemier yapıldı

        listener() // View listener açıldı

        openObservers() // Viewmodel dinleyici açıldı

        startApp() // Uygulama başlatıldı

    }

    private fun closeKeyboard() {

        currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

    }

    private fun initialize() {

        binding.lifecycleOwner = this

        binding.recyclerTrading.apply {
            setHasFixedSize(true) // Boyutunun değişmeyeceği bildirildi (performans için)
            layoutManager = LinearLayoutManager(context) // LayoutManager ayarlandı
            adapter = tradingHelperAdapter // Adaptör bağlandı
            (adapter as TradingHelperAdapter).itemListener(this@MainActivity) // Adaptöre listener verildi
            adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY // Recyclerview state kaydedildi, eğer veri var ise eski konuma gidilecek
        }

        viewmodel.openObservers() // Viewmodel gözlemcileri açıldı

    }

    private fun showLibraryDialog (requestBody : String) {

        val libraryDialog = AlertDialog.Builder(this)

        val dialogBinding: SelectedLibraryBinding = SelectedLibraryBinding.inflate(layoutInflater)

        libraryDialog.setView(dialogBinding.getRoot())

        libraryDialog.setPositiveButton("TAMAM") { _: DialogInterface?, _: Int ->

            val selectedLibraryView: RadioButton = dialogBinding.root.findViewById(dialogBinding.rdGroupLibrary.checkedRadioButtonId)

            val selectedLibrary = selectedLibraryView.text.toString()

            viewmodel.newRequest(selectedLibrary, requestBody)

        }

        libraryDialog.show();

    }

    private fun listener() {

        binding.apply {

            txtSearch.setOnEditorActionListener { view, action, _ ->

                // Klavyeden search'e tıklanırsa alınacak aksiyon
                if (action == EditorInfo.IME_ACTION_SEARCH) viewmodel.getData((view as TextInputEditText).text.toString())

                true

            }

            // EditText'e sondaki ikona tıklandığında gerçekleştirilecek aksiyon
            txtSearchLayout.setEndIconOnClickListener { viewmodel.getData(txtSearch.text.toString()) }

            swipeOnline.setOnRefreshListener {

                swipeOnline.isRefreshing = false // SwipeLayout animasyonunu kapat

                viewmodel.getData("") // Swipe aksiyon viewmodel'a aktarıldı

                txtSearch.apply {
                    text?.clear() // EditText içerisini temizle
                    clearFocus() // Fokusu kaldır
                }

            }

        }
    }

    private fun openObservers() {

        viewmodel.dataList.observe(this) { (binding.recyclerTrading.adapter as TradingHelperAdapter).update(it) } // Veri adaptöre verildi

        viewmodel.error.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() } // Hata kullanıcıya gösterildi

        viewmodel.libraryDialog.observe(this) { showLibraryDialog(it) } // Seçim dialog ekranı açıldı

        viewmodel.keyboardStatus.observe(this) { closeKeyboard() } // Klavye kapatıldı

    }

    private fun startApp() { viewmodel.getData("") } // Uygulama default filtre ile başlatıldı

    // Recyclerview item click
    override fun itemClick(data: ResponseRecyclerModel) {

        Dialog(this@MainActivity).apply {

            val dialogBinding = DialogInfoBinding.inflate(this@MainActivity.layoutInflater) // Dialog binding'i bağla
            window?.setBackgroundDrawable(ContextCompat.getDrawable(this@MainActivity, android.R.color.transparent)) // Dialog arkaplan'ı transparan yap
            dialogBinding.data = data // DataBinding variable ile Dialog içerisinde gösterilecek veriyi gönder
            setContentView(dialogBinding.root)
            show()

        }

    }

}