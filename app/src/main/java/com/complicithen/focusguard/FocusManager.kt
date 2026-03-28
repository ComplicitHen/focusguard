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
        showStatusNotification()
    }

    fun disable() {
        prefs.edit().putBoolean("enabled", false).apply()
        cancelStatusNotification()
    }

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)

    fun syncStatusNotification() {
        if (isEnabled()) showStatusNotification() else cancelStatusNotification()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_STATUS) == null) {
                val channel = NotificationChannel(
                    CHANNEL_STATUS,
                    "Focus mode status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shown while focus mode is active"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun showStatusNotification() {
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Focus mode is active")
            .setContentText("Unknown callers and notifications are blocked.")
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_STATUS_ID, notif)
    }

    fun cancelStatusNotification() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIFICATION_STATUS_ID)
    }

    companion object {
        const val CHANNEL_STATUS = "focus_status"
        const val NOTIFICATION_STATUS_ID = 1001
    }
}
