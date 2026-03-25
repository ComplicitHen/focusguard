package com.complicithen.focusguard

import android.content.Context
import android.content.SharedPreferences
import android.telephony.PhoneNumberUtils

class WhitelistManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("whitelist", Context.MODE_PRIVATE)

    fun getAll(): Set<String> = prefs.getStringSet("numbers", emptySet()) ?: emptySet()

    fun add(number: String) {
        val normalized = normalize(number)
        if (normalized.isBlank()) return
        val current = getAll().toMutableSet()
        current.add(normalized)
        prefs.edit().putStringSet("numbers", current).apply()
    }

    fun remove(number: String) {
        val current = getAll().toMutableSet()
        current.remove(number)
        prefs.edit().putStringSet("numbers", current).apply()
    }

    fun isWhitelisted(incomingNumber: String): Boolean {
        val normalized = normalize(incomingNumber)
        return getAll().any { stored ->
            PhoneNumberUtils.compare(normalized, stored) ||
                normalized.takeLast(8) == stored.takeLast(8)
        }
    }

    private fun normalize(number: String): String =
        number.trim().filter { it.isDigit() || it == '+' }
}
