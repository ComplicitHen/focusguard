package com.complicithen.focusguard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.complicithen.focusguard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), TAG_HOME)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(
                    supportFragmentManager.findFragmentByTag(TAG_HOME) ?: HomeFragment(),
                    TAG_HOME
                )
                R.id.nav_whitelist -> loadFragment(
                    supportFragmentManager.findFragmentByTag(TAG_WHITELIST) ?: WhitelistFragment(),
                    TAG_WHITELIST
                )
                R.id.nav_block -> loadFragment(
                    supportFragmentManager.findFragmentByTag(TAG_BLOCK) ?: BlocklistFragment(),
                    TAG_BLOCK
                )
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_WHITELIST = "whitelist"
        private const val TAG_BLOCK = "block"
    }
}
