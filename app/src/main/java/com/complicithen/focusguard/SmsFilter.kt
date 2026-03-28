package com.complicithen.focusguard

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Calendar

class SmsFilter : NotificationListenerService() {

    private val focusManager by lazy { FocusManager(applicationContext) }
    private val whitelistManager by lazy { WhitelistManager(applicationContext) }
    private val appFilterManager by lazy { AppFilterManager(applicationContext) }
    private val statsManager by lazy { StatsManager(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!focusManager.isEnabled()) return
        if (sbn.packageName == applicationContext.packageName) return

        // Check app filter: if in selective mode, only hold notifications from selected apps
        if (!appFilterManager.shouldFilter(sbn.packageName)) return

        // During normal focus mode, whitelisted senders/names get through immediately
        if (!focusManager.isBedtimeActive() && isFromWhitelistedSender(sbn)) return

        statsManager.incrementNotificationsHeld()
        snoozeNotification(sbn.key, msUntilNextHour())
    }

    private fun isFromWhitelistedSender(sbn: StatusBarNotification): Boolean {
        if (whitelistManager.getAll().isEmpty()) return false

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""

        // 1. Name-based match — works for Messenger, WhatsApp, Telegram, etc.
        if (title.isNotEmpty() && whitelistManager.isWhitelistedByName(title)) return true

        // 2. Phone-number match — works for SMS / apps that show numbers
        val candidates = buildList {
            add(title)
            extras.getString(Notification.EXTRA_TEXT)?.let { add(it) }
            extras.getString(Notification.EXTRA_SUB_TEXT)?.let { add(it) }
            sbn.notification.group?.let { add(it) }
        }
        return candidates.any { candidate ->
            val digits = candidate.filter { it.isDigit() }
            if (digits.length < 4) return@any false
            val last8 = digits.takeLast(8)
            whitelistManager.getAll()
                .filter { whitelistManager.looksLikePhone(it) }
                .any { it.filter { c -> c.isDigit() }.contains(last8) }
        }
    }

    private fun msUntilNextHour(): Long {
        val next = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (next.timeInMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
    }
}
