package com.izzdarki.minimalexpense.ui

import android.os.Bundle
import android.view.MotionEvent
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import com.izzdarki.minimalexpense.R
import com.izzdarki.minimalexpense.databinding.ActivityMainBinding

import androidx.navigation.fragment.NavHostFragment
import com.izzdarki.minimalexpense.ui.home.HomeFragment


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations
        // (The Up button will not be displayed when on these destinations)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    /**
     * Needed for HomeFragments EditLabelsComponent
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Every touch event goes through this function
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?
        val activeFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
        val homeFragment = activeFragment as? HomeFragment
        return if (homeFragment != null && homeFragment.dispatchTouchEvent(ev))
            true
        else
            super.dispatchTouchEvent(ev)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}