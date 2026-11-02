package com.tk.toolkit.tv.launcher.data

import android.content.Context
import androidx.core.content.edit

/** Persists the user's launcher customizations: app order, hidden apps, Continue Watching order and removals. */
class LauncherPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun loadOrder(): List<String> =
        prefs.getString(KEY_ORDER, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    fun saveOrder(packages: List<String>) {
        prefs.edit { putString(KEY_ORDER, packages.joinToString("\n")) }
    }

    fun hiddenPackages(): Set<String> = prefs.getStringSet(KEY_HIDDEN, null).orEmpty()

    fun setHidden(packageName: String, hidden: Boolean) {
        val updated = hiddenPackages().toMutableSet().apply { if (hidden) add(packageName) else remove(packageName) }
        prefs.edit { putStringSet(KEY_HIDDEN, updated) }
    }

    fun removedWatchItems(): Set<String> = prefs.getStringSet(KEY_REMOVED_WATCH, null).orEmpty()

    fun removeWatchItem(key: String) {
        prefs.edit { putStringSet(KEY_REMOVED_WATCH, removedWatchItems() + key) }
    }

    fun removeAllWatchItems(keys: Iterable<String>) {
        prefs.edit { putStringSet(KEY_REMOVED_WATCH, removedWatchItems() + keys) }
    }

    fun isWatchRowVisible(): Boolean = prefs.getBoolean(KEY_SHOW_WATCH_ROW, true)

    fun setWatchRowVisible(visible: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_WATCH_ROW, visible) }
    }

    fun disabledWatchPackages(): Set<String> = prefs.getStringSet(KEY_DISABLED_WATCH_PKGS, null).orEmpty()

    fun setWatchPackageEnabled(packageName: String, enabled: Boolean) {
        val updated = disabledWatchPackages().toMutableSet().apply {
            if (enabled) remove(packageName) else add(packageName)
        }
        prefs.edit { putStringSet(KEY_DISABLED_WATCH_PKGS, updated) }
    }

    fun saveWatchOrder(keys: List<String>) {
        prefs.edit { putString(KEY_WATCH_ORDER, keys.joinToString("\n")) }
    }

    /** Continue Watching items in saved order first, then new ones in their default order. */
    fun sortWatchItems(items: List<WatchItem>): List<WatchItem> {
        val rank = prefs.getString(KEY_WATCH_ORDER, null)?.split('\n')
            ?.filter { it.isNotBlank() }.orEmpty()
            .withIndex().associate { (i, key) -> key to i }
        return items.sortedBy { rank[it.key] ?: Int.MAX_VALUE }
    }

    /** Apps in saved order first, then any apps not yet ordered, alphabetically. */
    fun sort(apps: List<LauncherApp>): List<LauncherApp> {
        val rank = loadOrder().withIndex().associate { (i, pkg) -> pkg to i }
        val (known, fresh) = apps.partition { it.packageName in rank }
        return known.sortedBy { rank.getValue(it.packageName) } +
            fresh.sortedBy { it.label.lowercase() }
    }

    private companion object {
        const val KEY_ORDER = "order"
        const val KEY_HIDDEN = "hidden"
        const val KEY_REMOVED_WATCH = "removed_watch"
        const val KEY_WATCH_ORDER = "watch_order"
        const val KEY_SHOW_WATCH_ROW = "show_watch_row"
        const val KEY_DISABLED_WATCH_PKGS = "disabled_watch_pkgs"
    }
}
