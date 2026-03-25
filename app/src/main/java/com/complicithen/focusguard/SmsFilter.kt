package com.complicithen.focusguard

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SmsFilter : NotificationListenerService() {

    // Messaging app packages to filter notifications from
    private val messagingPackages = setOf(
        "com.google.android.apps.messaging",   // Google Messages
        "com.samsung.android.messaging",        // Samsung Messages
        "com.android.mms",                      // Stock MMS
        "org.thoughtcrime.securesms",           // Signal
        "com.whatsapp",                         // WhatsApp
        "com.facebook.orca",                    // Messenger
        "org.telegram.messenger",               // Telegram
        "com.viber.voip",                       // Viber
        "com.snapchat.android"                  // Snapchat
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val focusManager = FocusManager(applicationContext)
        if (!focusManager.isEnabled()) return
        if (sbn.packageName !in messagingPackages) return

        val extras = sbn.notification.extras
        val whitelistManager = WhitelistManager(applicationContext)

        // Try to extract sender from notification extras (title usually has sender name/number)
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""

        val sender = if (title.isNotBlank()) title else subText
        if (sender.isBlank()) {
            // No sender info — cancel to be safe in focus mode
            cancelNotification(sbn.key)
            return
        }

        // Check if the sender matches a whitelisted number
        val isWhitelisted = whitelistManager.getAll().any { whitelisted ->
            sender.contains(whitelisted.takeLast(8)) ||
                whitelisted.contains(sender.filter { it.isDigit() }.takeLast(8))
        }

        if (!isWhitelisted) {
            cancelNotification(sbn.key)
        }
    }
}
