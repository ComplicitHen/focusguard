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

    // Lazy to avoid allocating on every notification
    private val focusManager by lazy { FocusManager(applicationContext) }
    private val whitelistManager by lazy { WhitelistManager(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
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
        val whitelist = whitelistManager.getAll()
        if (whitelist.isEmpty()) return false

        val extras = sbn.notification.extras

        // Collect all string fields that could contain a sender phone number.
        // Some apps put the raw number in EXTRA_TITLE (unknown contacts), others
        // in EXTRA_TEXT or the notification group key.
        // Note: for contacts stored by display name only (no digits), matching
        // requires READ_CONTACTS — not requested here, so those won't match.
        val candidates = buildList {
            extras.getString(Notification.EXTRA_TITLE)?.let { add(it) }
            extras.getString(Notification.EXTRA_TEXT)?.let { add(it) }
            extras.getString(Notification.EXTRA_SUB_TEXT)?.let { add(it) }
            sbn.notification.group?.let { add(it) }
        }

        return whitelist.any { number ->
            val digits = number.filter { it.isDigit() }
            if (digits.length < 4) return@any false
            val last8 = digits.takeLast(8)
            candidates.any { candidate ->
                candidate.filter { it.isDigit() }.contains(last8)
            }
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
