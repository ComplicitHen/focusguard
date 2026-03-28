package com.complicithen.focusguard

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService

class CallScreener : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val fm = FocusManager(applicationContext)
        if (!fm.isEnabled()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart ?: ""

        // Emergency bypass: if the same number called within the last 2 minutes, let through
        if (fm.emergencyBypassEnabled && isEmergencyRetry(number)) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        recordCallAttempt(number)

        // During bedtime: block all calls regardless of whitelist
        if (fm.isBedtimeActive()) {
            StatsManager(applicationContext).incrementCallsBlocked()
            respondToCall(callDetails, buildBlockResponse())
            return
        }

        // Normal focus mode: allow whitelisted numbers
        val whitelisted = number.isNotEmpty() && WhitelistManager(applicationContext).isWhitelisted(number)
        if (whitelisted) {
            respondToCall(callDetails, CallResponse.Builder().build())
        } else {
            StatsManager(applicationContext).incrementCallsBlocked()
            respondToCall(callDetails, buildBlockResponse())
        }
    }

    private fun isEmergencyRetry(number: String): Boolean {
        if (number.isEmpty()) return false
        val prefs = applicationContext.getSharedPreferences("recent_calls", Context.MODE_PRIVATE)
        val last = prefs.getLong(number, 0L)
        return last > 0L && System.currentTimeMillis() - last < 2 * 60_000L
    }

    private fun recordCallAttempt(number: String) {
        if (number.isEmpty()) return
        applicationContext.getSharedPreferences("recent_calls", Context.MODE_PRIVATE)
            .edit().putLong(number, System.currentTimeMillis()).apply()
    }

    private fun buildBlockResponse() = CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build()
}
