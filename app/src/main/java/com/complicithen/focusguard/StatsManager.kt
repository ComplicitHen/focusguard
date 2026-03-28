package com.complicithen.focusguard

import android.content.Context
import java.util.Calendar

class StatsManager(context: Context) {
    private val prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)

    fun recordFocusStart() {
        prefs.edit().putLong("start_$today", System.currentTimeMillis()).apply()
    }

    fun recordFocusEnd() {
        val start = prefs.getLong("start_$today", 0L)
        if (start > 0L) {
            val prev = prefs.getLong("minutes_$today", 0L)
            val added = (System.currentTimeMillis() - start) / 60_000L
            prefs.edit()
                .putLong("minutes_$today", prev + added)
                .remove("start_$today")
                .apply()
        }
    }

    fun incrementNotificationsHeld() {
        val key = "notifs_$today"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun incrementCallsBlocked() {
        val key = "calls_$today"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun getTodayMinutesActive(): Long {
        var minutes = prefs.getLong("minutes_$today", 0L)
        val activeStart = prefs.getLong("start_$today", 0L)
        if (activeStart > 0L) minutes += (System.currentTimeMillis() - activeStart) / 60_000L
        return minutes
    }

    fun getTodayNotificationsHeld(): Int = prefs.getInt("notifs_$today", 0)
    fun getTodayCallsBlocked(): Int = prefs.getInt("calls_$today", 0)

    private val today: String
        get() {
            val c = Calendar.getInstance()
            return "${c.get(Calendar.YEAR)}_${c.get(Calendar.MONTH)}_${c.get(Calendar.DAY_OF_MONTH)}"
        }
}
