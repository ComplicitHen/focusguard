package com.complicithen.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val focusManager = FocusManager(context)
            if (focusManager.isEnabled()) {
                // Re-schedule hourly notifications after reboot
                focusManager.enable()
            }
        }
    }
}
