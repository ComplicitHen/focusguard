package com.complicithen.focusguard

import android.content.Context
import android.content.SharedPreferences

class FocusManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus", Context.MODE_PRIVATE)

    fun enable() {
        prefs.edit().putBoolean("enabled", true).apply()
    }

    fun disable() {
        prefs.edit().putBoolean("enabled", false).apply()
    }

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)
}
