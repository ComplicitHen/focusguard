package com.complicithen.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Focus mode state is stored in SharedPreferences and survives reboot.
// The NotificationListenerService is re-bound automatically by Android after boot.
// This receiver exists to trigger any future post-boot setup if needed.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: NotificationListenerService and CallScreeningService
        // are automatically restarted by Android after boot.
        // FocusManager state persists in SharedPreferences.
    }
}
