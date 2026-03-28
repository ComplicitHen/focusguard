package com.complicithen.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Restore the persistent status notification if focus mode was active before reboot.
        FocusManager(context).syncStatusNotification()
    }
}
