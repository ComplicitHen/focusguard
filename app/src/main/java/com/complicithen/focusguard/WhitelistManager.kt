package com.complicithen.focusguard

import android.content.Context
import android.content.SharedPreferences
import android.telephony.PhoneNumberUtils

class WhitelistManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("whitelist", Context.MODE_PRIVATE)

    fun getAll(): Set<String> = prefs.getStringSet("numbers", emptySet()) ?: emptySet()

    /**
     * Add a phone number or a contact name.
     * If the input has 4+ digits it is treated as a phone number and normalised.
     * Otherwise it is stored as a lowercase name (used to match notification titles
     * from apps like Messenger, WhatsApp, Telegram, etc.)
     */
    fun add(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return
        val entry = if (looksLikePhone(trimmed)) normalizePhone(trimmed) else trimmed.lowercase()
        if (entry.isBlank()) return
        prefs.edit().putStringSet("numbers", getAll().toMutableSet().apply { add(entry) }).apply()
    }

    fun remove(entry: String) {
        prefs.edit().putStringSet("numbers", getAll().toMutableSet().apply { remove(entry) }).apply()
    }

    /** Phone-number check used by CallScreener (names never match calls). */
    fun isWhitelisted(incomingNumber: String): Boolean {
        val normalized = normalizePhone(incomingNumber)
        return getAll()
            .filter { looksLikePhone(it) }
            .any { PhoneNumberUtils.compare(normalized, it) || normalized.takeLast(8) == it.takeLast(8) }
    }

    /**
     * Name-based check used by SmsFilter.
     * Returns true if [title] (the notification sender name) matches any name entry
     * in the whitelist (case-insensitive).
     */
    fun isWhitelistedByName(title: String): Boolean {
        val lower = title.trim().lowercase()
        return getAll()
            .filter { !looksLikePhone(it) }   // name entries only
            .any { lower == it || lower.contains(it) || it.contains(lower) }
    }

    fun looksLikePhone(entry: String): Boolean = entry.filter { it.isDigit() }.length >= 4

    private fun normalizePhone(number: String): String =
        number.trim().filter { it.isDigit() || it == '+' }
}
