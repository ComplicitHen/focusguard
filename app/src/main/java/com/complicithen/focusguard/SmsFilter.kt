package com.complicithen.focusguard

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Calendar

/**
 * Holds all incoming notifications and releases them in a batch at the top of each hour.
 * Notifications from whitelisted numbers (in messaging/SMS apps) are let through immediately.
 * Our own FocusGuard notifications are never held.
 */
class SmsFilter : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val focusManager = FocusManager(applicationContext)
        if (!focusManager.isEnabled()) return

        // Never hold our own notifications (the hourly release signal)
        if (sbn.packageName == applicationContext.packageName) return

        // Let whitelisted senders through immediately
        if (isFromWhitelistedSender(sbn)) return

        // Snooze until the next complete hour (:00)
        val delay = msUntilNextHour()
        snoozeNotification(sbn.key, delay)
    }

    private fun isFromWhitelistedSender(sbn: StatusBarNotification): Boolean {
        val whitelist = WhitelistManager(applicationContext)
        if (whitelist.getAll().isEmpty()) return false

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return false

        // Match against trailing digits — handles both full numbers and display names
        // that contain a phone number
        return whitelist.getAll().any { number ->
            val digits = number.filter { it.isDigit() }
            digits.length >= 4 && title.filter { it.isDigit() }.contains(digits.takeLast(8))
        }
    }

    private fun msUntilNextHour(): Long {
        val next = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // At least 1 second, in case we're right on the hour boundary
        return (next.timeInMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
    }
}
