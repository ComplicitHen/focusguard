package com.complicithen.focusguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat

class FocusManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus", Context.MODE_PRIVATE)

    fun enable() {
        prefs.edit().putBoolean("enabled", true).apply()
        StatsManager(context).recordFocusStart()
        showStatusNotification()
    }

    fun disable() {
        prefs.edit().putBoolean("enabled", false).apply()
        setBedtimeActive(false)
        StatsManager(context).recordFocusEnd()
        cancelStatusNotification()
    }

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)

    /** True during bedtime: whitelisted callers are also blocked (only emergency bypass gets through). */
    fun setBedtimeActive(active: Boolean) {
        prefs.edit().putBoolean("bedtime_active", active).apply()
        if (active) showStatusNotification() // update notification text
    }

    fun isBedtimeActive(): Boolean = prefs.getBoolean("bedtime_active", false)

    /** If true: a second call from the same number within 2 minutes always gets through. */
    var emergencyBypassEnabled: Boolean
        get() = prefs.getBoolean("emergency_bypass", true)
        set(v) { prefs.edit().putBoolean("emergency_bypass", v).apply() }

    fun syncStatusNotification() {
        if (isEnabled()) showStatusNotification() else cancelStatusNotification()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_STATUS) == null) {
                NotificationChannel(CHANNEL_STATUS, "Focus mode status", NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) }
                    .let { nm.createNotificationChannel(it) }
            }
        }
    }

    fun showStatusNotification() {
        ensureChannel()
        val pi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val body = if (isBedtimeActive())
            "Bedtime mode — all calls and notifications are blocked."
        else
            "Unknown callers and notifications are blocked."

        val notif = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Focus mode is active")
            .setContentText(body)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_STATUS_ID, notif)
    }

    fun cancelStatusNotification() {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_STATUS_ID)
    }

    companion object {
        const val CHANNEL_STATUS = "focus_status"
        const val NOTIFICATION_STATUS_ID = 1001
    }
}
