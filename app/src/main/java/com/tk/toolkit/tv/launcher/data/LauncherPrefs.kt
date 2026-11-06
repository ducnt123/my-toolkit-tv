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

    fun appColumns(): Int = prefs.getInt(KEY_APP_COLUMNS, 6).coerceIn(4, 6)

    fun setAppColumns(columns: Int) {
        prefs.edit { putInt(KEY_APP_COLUMNS, columns.coerceIn(4, 6)) }
    }

    fun isBackToAmbientEnabled(): Boolean = prefs.getBoolean(KEY_BACK_TO_AMBIENT, true)

    fun setBackToAmbientEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BACK_TO_AMBIENT, enabled) }
    }

    fun isOverrideHomeKeyEnabled(defaultValue: Boolean = false): Boolean =
        prefs.getBoolean(KEY_OVERRIDE_HOME, defaultValue)

    fun hasOverrideHomeKeyPref(): Boolean = prefs.contains(KEY_OVERRIDE_HOME)

    fun setOverrideHomeKeyEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_OVERRIDE_HOME, enabled) }
    }

    fun remoteButtonTarget(button: RemoteButton): String =
        prefs.getString("$KEY_REMOTE_TARGET_PREFIX${button.id}", RemoteButtons.TARGET_DEFAULT)
            ?: RemoteButtons.TARGET_DEFAULT

    fun setRemoteButtonTarget(button: RemoteButton, target: String) {
        prefs.edit { putString("$KEY_REMOTE_TARGET_PREFIX${button.id}", target) }
    }

    fun remoteButtonCustomKeyCode(button: RemoteButton): Int =
        prefs.getInt("$KEY_REMOTE_KEYCODE_PREFIX${button.id}", 0)

    fun remoteButtonCustomScanCode(button: RemoteButton): Int =
        prefs.getInt("$KEY_REMOTE_SCANCODE_PREFIX${button.id}", 0)

    fun setRemoteButtonCustomKey(button: RemoteButton, keyCode: Int, scanCode: Int) {
        val effectiveScan = if (keyCode > 0 || scanCode == 240) 0 else scanCode
        val remainingCustom = if (keyCode > 0) {
            customKeyMappings().filterNot { it.keyCode == keyCode }
        } else {
            customKeyMappings()
        }
        prefs.edit {
            if (keyCode > 0) {
                for (other in RemoteButton.entries) {
                    if (other != button && remoteButtonCustomKeyCode(other) == keyCode) {
                        putInt("$KEY_REMOTE_KEYCODE_PREFIX${other.id}", 0)
                        putInt("$KEY_REMOTE_SCANCODE_PREFIX${other.id}", 0)
                    }
                }
                putString(
                    KEY_CUSTOM_REMOTE_KEYS,
                    remainingCustom.joinToString("\n") { "${it.keyCode}:${it.scanCode}:${it.target}" },
                )
            }
            putInt("$KEY_REMOTE_KEYCODE_PREFIX${button.id}", keyCode)
            putInt("$KEY_REMOTE_SCANCODE_PREFIX${button.id}", effectiveScan)
        }
    }

    fun remoteButtonConfigs(): List<RemoteButtonConfig> =
        RemoteButton.entries.map { button ->
            RemoteButtonConfig(
                button = button,
                target = remoteButtonTarget(button),
                customKeyCode = remoteButtonCustomKeyCode(button),
                customScanCode = remoteButtonCustomScanCode(button),
            )
        }

    fun customKeyMappings(): List<CustomKeyMapping> {
        val raw = prefs.getString(KEY_CUSTOM_REMOTE_KEYS, null).orEmpty()
        return raw.split('\n').mapNotNull { line ->
            val parts = line.split(':', limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val keyCode = parts[0].toIntOrNull() ?: return@mapNotNull null
            val scanCode = parts[1].toIntOrNull() ?: return@mapNotNull null
            val target = parts[2].trim()
            if (target.isEmpty() || (keyCode <= 0 && scanCode <= 0)) return@mapNotNull null
            CustomKeyMapping(keyCode = keyCode, scanCode = scanCode, target = target)
        }
    }

    fun upsertCustomKeyMapping(keyCode: Int, scanCode: Int, target: String) {
        val effectiveScan = if (keyCode > 0 || scanCode == 240) 0 else scanCode
        val updated = customKeyMappings().filterNot { existing ->
            (keyCode > 0 && existing.keyCode == keyCode) ||
                (keyCode <= 0 && effectiveScan > 0 && existing.scanCode == effectiveScan)
        } + CustomKeyMapping(keyCode = keyCode, scanCode = effectiveScan, target = target)
        prefs.edit {
            if (keyCode > 0) {
                for (button in RemoteButton.entries) {
                    if (remoteButtonCustomKeyCode(button) == keyCode) {
                        putInt("$KEY_REMOTE_KEYCODE_PREFIX${button.id}", 0)
                        putInt("$KEY_REMOTE_SCANCODE_PREFIX${button.id}", 0)
                    }
                }
            }
            putString(
                KEY_CUSTOM_REMOTE_KEYS,
                updated.joinToString("\n") { "${it.keyCode}:${it.scanCode}:${it.target}" },
            )
        }
    }

    fun removeCustomKeyMapping(keyCode: Int, scanCode: Int) {
        val updated = customKeyMappings().filterNot { it.keyCode == keyCode && it.scanCode == scanCode }
        prefs.edit {
            putString(
                KEY_CUSTOM_REMOTE_KEYS,
                updated.joinToString("\n") { "${it.keyCode}:${it.scanCode}:${it.target}" },
            )
        }
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
        const val KEY_APP_COLUMNS = "app_columns"
        const val KEY_BACK_TO_AMBIENT = "back_to_ambient"
        const val KEY_OVERRIDE_HOME = "override_home"
        const val KEY_REMOTE_TARGET_PREFIX = "remote_target_"
        const val KEY_REMOTE_KEYCODE_PREFIX = "remote_keycode_"
        const val KEY_REMOTE_SCANCODE_PREFIX = "remote_scancode_"
        const val KEY_CUSTOM_REMOTE_KEYS = "custom_remote_keys"
    }
}
