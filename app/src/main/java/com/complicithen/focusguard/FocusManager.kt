package com.complicithen.focusguard

import android.content.Context
import android.content.SharedPreferences
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class FocusManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus", Context.MODE_PRIVATE)

    fun enable() {
        prefs.edit().putBoolean("enabled", true).apply()
        scheduleHourlyNotifications()
    }

    fun disable() {
        prefs.edit().putBoolean("enabled", false).apply()
        WorkManager.getInstance(context).cancelUniqueWork("hourly_focus")
    }

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)

    private fun scheduleHourlyNotifications() {
        val request = PeriodicWorkRequestBuilder<HourlyWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "hourly_focus",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
