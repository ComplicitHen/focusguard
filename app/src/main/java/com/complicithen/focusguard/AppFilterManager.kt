package com.complicithen.focusguard

import android.content.Context

/**
 * Controls which apps have their notifications held during focus mode.
 *
 * selectiveMode = false (default): hold notifications from ALL apps
 * selectiveMode = true:            hold only notifications from apps in the filter list
 */
class AppFilterManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_filter", Context.MODE_PRIVATE)

    var selectiveMode: Boolean
        get() = prefs.getBoolean("selective", false)
        set(v) { prefs.edit().putBoolean("selective", v).apply() }

    fun getFilteredApps(): Set<String> = prefs.getStringSet("apps", emptySet()) ?: emptySet()

    fun add(packageName: String) {
        val set = getFilteredApps().toMutableSet().apply { add(packageName) }
        prefs.edit().putStringSet("apps", set).apply()
    }

    fun remove(packageName: String) {
        val set = getFilteredApps().toMutableSet().apply { remove(packageName) }
        prefs.edit().putStringSet("apps", set).apply()
    }

    /** Returns true if this app's notifications should be held during focus mode. */
    fun shouldFilter(packageName: String): Boolean {
        return if (selectiveMode) getFilteredApps().contains(packageName)
        else true
    }
}
