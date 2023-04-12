package com.basesoftware.tradinghelperkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.basesoftware.tradinghelperkotlin.databinding.ActivityMainBinding
import com.basesoftware.tradinghelperkotlin.viewmodel.SharedViewModel

class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel by viewModels<SharedViewModel>()

    private lateinit var navController : NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = (supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment).navController

        binding.navBottom.setupWithNavController(navController)

    }

}