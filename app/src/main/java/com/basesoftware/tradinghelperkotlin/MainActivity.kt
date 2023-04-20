package com.basesoftware.tradinghelperkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.room.Room
import com.basesoftware.tradinghelperkotlin.databinding.ActivityMainBinding
import com.basesoftware.tradinghelperkotlin.db.WatchDatabase
import com.basesoftware.tradinghelperkotlin.viewmodel.SharedViewModel

class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel by viewModels<SharedViewModel> { SharedViewModel.provideFactory(this, this) }

    private lateinit var navController : NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedViewModel.apply {
            db = Room.databaseBuilder(applicationContext, WatchDatabase::class.java, "TradingHelperKotlin").allowMainThreadQueries().build() // Veritabanı tanımlandı
            dao = db.watchDao() // Dao tanımlandı
        }

        navController = (supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment).navController // NavController tanımlandı

        /**
         * setupWithNavController kullanılmamasının sebebi: setPopUpTo kullanarak başlangıçta açılan fragment'ta onDestroy tetikleyebilmek
         * OnlineFragment:
         * - Her açılışında yeni veri isteği yapacak böylece güncel veri oluşturulacak
         * WatchFrgment:
         * - Her açılışında veritabanından güncel listeyi çekecek aynı zamanda sunucudan yeni veriyi alacak
         *
         * Uygulama arka plan işlem sınırı vb durumdan ötürü öldürülürse:
         * Tekrar açıldığında state kullanılacak yeni veritabanı/api isteği atılmayacak
         */
        binding.navBottom.setOnItemSelectedListener {
            navController.navigate(it.itemId, null, NavOptions.Builder().setPopUpTo(it.itemId, true).build())
            return@setOnItemSelectedListener true
        }

    }

}