package com.complicithen.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fm = FocusManager(context)
        when (intent.action) {
            ACTION_FOCUS_ON -> fm.enable()
            ACTION_FOCUS_OFF -> {
                fm.setBedtimeActive(false)
                fm.disable()
            }
            ACTION_BEDTIME_ON -> {
                fm.setBedtimeActive(true)
                fm.enable()
            }
            ACTION_BEDTIME_OFF -> {
                fm.setBedtimeActive(false)
                fm.disable()
            }
        }
    }

    companion object {
        const val ACTION_FOCUS_ON   = "com.complicithen.focusguard.FOCUS_ON"
        const val ACTION_FOCUS_OFF  = "com.complicithen.focusguard.FOCUS_OFF"
        const val ACTION_BEDTIME_ON = "com.complicithen.focusguard.BEDTIME_ON"
        const val ACTION_BEDTIME_OFF = "com.complicithen.focusguard.BEDTIME_OFF"
    }
}
