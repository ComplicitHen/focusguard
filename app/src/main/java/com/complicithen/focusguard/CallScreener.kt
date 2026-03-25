package com.complicithen.focusguard

import android.telecom.Call
import android.telecom.CallScreeningService

class CallScreener : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val focusManager = FocusManager(applicationContext)

        // If focus mode is off, allow everything through
        if (!focusManager.isEnabled()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart
        if (number == null) {
            // Unknown number — block it during focus mode
            respondToCall(callDetails, buildBlockResponse())
            return
        }

        val whitelistManager = WhitelistManager(applicationContext)
        val response = if (whitelistManager.isWhitelisted(number)) {
            CallResponse.Builder().build() // Allow
        } else {
            buildBlockResponse()
        }
        respondToCall(callDetails, response)
    }

    private fun buildBlockResponse() = CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipCallLog(false)   // Still log the blocked call
        .setSkipNotification(false) // Still show missed call notification
        .build()
}
