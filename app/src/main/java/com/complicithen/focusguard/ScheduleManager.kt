package com.complicithen.focusguard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class ScheduleManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("schedule", Context.MODE_PRIVATE)

    var focusEnabled: Boolean
        get() = prefs.getBoolean("focus_on", false)
        set(v) { prefs.edit().putBoolean("focus_on", v).apply() }

    var focusStartHour: Int
        get() = prefs.getInt("focus_sh", 9)
        set(v) { prefs.edit().putInt("focus_sh", v).apply() }
    var focusStartMin: Int
        get() = prefs.getInt("focus_sm", 0)
        set(v) { prefs.edit().putInt("focus_sm", v).apply() }
    var focusEndHour: Int
        get() = prefs.getInt("focus_eh", 17)
        set(v) { prefs.edit().putInt("focus_eh", v).apply() }
    var focusEndMin: Int
        get() = prefs.getInt("focus_em", 0)
        set(v) { prefs.edit().putInt("focus_em", v).apply() }

    var bedtimeEnabled: Boolean
        get() = prefs.getBoolean("bed_on", false)
        set(v) { prefs.edit().putBoolean("bed_on", v).apply() }

    var bedtimeStartHour: Int
        get() = prefs.getInt("bed_sh", 22)
        set(v) { prefs.edit().putInt("bed_sh", v).apply() }
    var bedtimeStartMin: Int
        get() = prefs.getInt("bed_sm", 0)
        set(v) { prefs.edit().putInt("bed_sm", v).apply() }
    var bedtimeEndHour: Int
        get() = prefs.getInt("bed_eh", 7)
        set(v) { prefs.edit().putInt("bed_eh", v).apply() }
    var bedtimeEndMin: Int
        get() = prefs.getInt("bed_em", 0)
        set(v) { prefs.edit().putInt("bed_em", v).apply() }

    fun scheduleAll() {
        cancelAll()
        if (focusEnabled) {
            schedule(ScheduleReceiver.ACTION_FOCUS_ON, focusStartHour, focusStartMin)
            schedule(ScheduleReceiver.ACTION_FOCUS_OFF, focusEndHour, focusEndMin)
        }
        if (bedtimeEnabled) {
            schedule(ScheduleReceiver.ACTION_BEDTIME_ON, bedtimeStartHour, bedtimeStartMin)
            schedule(ScheduleReceiver.ACTION_BEDTIME_OFF, bedtimeEndHour, bedtimeEndMin)
        }
    }

    fun cancelAll() {
        val am = context.getSystemService(AlarmManager::class.java)
        listOf(
            ScheduleReceiver.ACTION_FOCUS_ON,
            ScheduleReceiver.ACTION_FOCUS_OFF,
            ScheduleReceiver.ACTION_BEDTIME_ON,
            ScheduleReceiver.ACTION_BEDTIME_OFF
        ).forEach { am.cancel(pendingIntent(it)) }
    }

    private fun schedule(action: String, hour: Int, minute: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.setRepeating(
            AlarmManager.RTC_WAKEUP,
            nextOccurrence(hour, minute),
            AlarmManager.INTERVAL_DAY,
            pendingIntent(action)
        )
    }

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }

    private fun pendingIntent(action: String): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
