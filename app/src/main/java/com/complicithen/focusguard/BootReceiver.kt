package com.complicithen.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FocusManager(context).syncStatusNotification()
        ScheduleManager(context).scheduleAll()
    }
}
