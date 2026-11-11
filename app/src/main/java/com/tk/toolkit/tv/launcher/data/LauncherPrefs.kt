package com.tk.toolkit.tv.launcher.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Persists the user's launcher customizations: app order, hidden apps, Continue Watching, remote keys, theme, and auto-launch. */
class LauncherPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun loadOrder(): List<String> =
        prefs.getString(KEY_ORDER, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()

    fun saveOrder(packages: List<String>) {
        prefs.edit { putString(KEY_ORDER, packages.joinToString("\n")) }
    }

    fun hiddenPackages(): Set<String> = prefs.getStringSet(KEY_HIDDEN, null).orEmpty()

    fun unhiddenNonTvPackages(): Set<String> = prefs.getStringSet(KEY_UNHIDDEN_NON_TV, null).orEmpty()

    /**
     * Sideloaded / non-TV apps ([LauncherApp.isLeanback] == false) are hidden by default
     * and placed in "More Apps" unless the user explicitly unhides them.
     */
    fun isAppHidden(app: LauncherApp): Boolean {
        val explicitlyHidden = hiddenPackages()
        if (app.packageName in explicitlyHidden) return true
        if (!app.isLeanback) {
            return app.packageName !in unhiddenNonTvPackages()
        }
        return false
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        val updatedHidden = hiddenPackages().toMutableSet()
        val updatedUnhiddenNonTv = unhiddenNonTvPackages().toMutableSet()
        if (hidden) {
            updatedHidden.add(packageName)
            updatedUnhiddenNonTv.remove(packageName)
        } else {
            updatedHidden.remove(packageName)
            updatedUnhiddenNonTv.add(packageName)
        }
        prefs.edit {
            putStringSet(KEY_HIDDEN, updatedHidden)
            putStringSet(KEY_UNHIDDEN_NON_TV, updatedUnhiddenNonTv)
        }
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

    fun language(): String = prefs.getString(KEY_LANGUAGE, AppLanguage.DEFAULT.code) ?: AppLanguage.DEFAULT.code

    fun setLanguage(code: String) {
        prefs.edit { putString(KEY_LANGUAGE, AppLanguage.fromCode(code).code) }
    }

    fun themePalette(): AppThemePalette =
        AppThemePalette.fromId(prefs.getString(KEY_THEME_PALETTE, AppThemePalette.DEFAULT.id))

    fun setThemePalette(palette: AppThemePalette) {
        prefs.edit { putString(KEY_THEME_PALETTE, palette.id) }
    }

    fun isDynamicAmbientEnabled(): Boolean = prefs.getBoolean(KEY_DYNAMIC_AMBIENT, false)

    fun setDynamicAmbientEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_AMBIENT, enabled) }
    }

    fun wallpaperDim(): Int = prefs.getInt(KEY_WALLPAPER_DIM, DEFAULT_WALLPAPER_DIM).coerceIn(0, 100)

    fun setWallpaperDim(dim: Int) {
        prefs.edit { putInt(KEY_WALLPAPER_DIM, dim.coerceIn(0, 100)) }
    }

    fun autoLaunchTarget(): String = prefs.getString(KEY_AUTO_LAUNCH_TARGET, "").orEmpty()

    fun setAutoLaunchTarget(target: String) {
        prefs.edit { putString(KEY_AUTO_LAUNCH_TARGET, target) }
    }

    fun isAutoLaunchOnBootEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LAUNCH_BOOT, true)

    fun setAutoLaunchOnBootEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_LAUNCH_BOOT, enabled) }
    }

    fun isAutoLaunchOnWakeEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LAUNCH_WAKE, true)

    fun setAutoLaunchOnWakeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_LAUNCH_WAKE, enabled) }
    }

    fun updateCheckInterval(): UpdateCheckInterval =
        UpdateCheckInterval.fromId(prefs.getString(KEY_UPDATE_CHECK_INTERVAL, UpdateCheckInterval.DEFAULT.id))

    fun setUpdateCheckInterval(interval: UpdateCheckInterval) {
        prefs.edit { putString(KEY_UPDATE_CHECK_INTERVAL, interval.id) }
    }

    fun lastUpdateCheckTimeMs(): Long = prefs.getLong(KEY_LAST_UPDATE_CHECK_MS, 0L)

    fun setLastUpdateCheckTimeMs(timeMs: Long) {
        prefs.edit { putLong(KEY_LAST_UPDATE_CHECK_MS, timeMs) }
    }

    fun availableUpdateVersion(): String =
        prefs.getString(KEY_AVAILABLE_UPDATE_VERSION, null).orEmpty()

    fun setAvailableUpdateVersion(version: String) {
        prefs.edit { putString(KEY_AVAILABLE_UPDATE_VERSION, version) }
    }

    fun isOverrideHomeKeyEnabled(defaultValue: Boolean = false): Boolean =
        prefs.getBoolean(KEY_OVERRIDE_HOME, defaultValue)

    fun isAdvancedSettingsVisible(): Boolean = prefs.getBoolean(KEY_ADVANCED_VISIBLE, false)

    fun setAdvancedSettingsVisible(visible: Boolean) {
        prefs.edit { putBoolean(KEY_ADVANCED_VISIBLE, visible) }
    }

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

    fun exportToJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put(KEY_ORDER, JSONArray(loadOrder()))
        root.put(KEY_HIDDEN, JSONArray(hiddenPackages().toList()))
        root.put(KEY_UNHIDDEN_NON_TV, JSONArray(unhiddenNonTvPackages().toList()))
        root.put(KEY_REMOVED_WATCH, JSONArray(removedWatchItems().toList()))
        root.put(
            KEY_WATCH_ORDER,
            JSONArray(prefs.getString(KEY_WATCH_ORDER, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()),
        )
        root.put(KEY_DISABLED_WATCH_PKGS, JSONArray(disabledWatchPackages().toList()))
        root.put(KEY_SHOW_WATCH_ROW, isWatchRowVisible())
        root.put(KEY_APP_COLUMNS, appColumns())
        root.put(KEY_BACK_TO_AMBIENT, isBackToAmbientEnabled())
        root.put(KEY_LANGUAGE, language())
        root.put(KEY_THEME_PALETTE, themePalette().id)
        root.put(KEY_DYNAMIC_AMBIENT, isDynamicAmbientEnabled())
        root.put(KEY_WALLPAPER_DIM, wallpaperDim())
        root.put(KEY_AUTO_LAUNCH_TARGET, autoLaunchTarget())
        root.put(KEY_AUTO_LAUNCH_BOOT, isAutoLaunchOnBootEnabled())
        root.put(KEY_AUTO_LAUNCH_WAKE, isAutoLaunchOnWakeEnabled())
        root.put(KEY_UPDATE_CHECK_INTERVAL, updateCheckInterval().id)
        if (hasOverrideHomeKeyPref()) {
            root.put(KEY_OVERRIDE_HOME, isOverrideHomeKeyEnabled())
        }
        val remoteObj = JSONObject()
        for (btn in RemoteButton.entries) {
            val btnObj = JSONObject()
            btnObj.put("target", remoteButtonTarget(btn))
            btnObj.put("keyCode", remoteButtonCustomKeyCode(btn))
            btnObj.put("scanCode", remoteButtonCustomScanCode(btn))
            remoteObj.put(btn.id, btnObj)
        }
        root.put("remote_buttons", remoteObj)
        root.put(
            KEY_CUSTOM_REMOTE_KEYS,
            JSONArray(customKeyMappings().map { "${it.keyCode}:${it.scanCode}:${it.target}" }),
        )
        return root.toString(2)
    }

    fun importFromJson(jsonString: String): Boolean = runCatching {
        val root = JSONObject(jsonString)
        if (!root.has("version") && !root.has(KEY_ORDER) && !root.has(KEY_APP_COLUMNS)) {
            return false
        }
        prefs.edit {
            root.optJSONArray(KEY_ORDER)?.toStringList()?.let {
                putString(KEY_ORDER, it.joinToString("\n"))
            }
            root.optJSONArray(KEY_HIDDEN)?.toStringList()?.let {
                putStringSet(KEY_HIDDEN, it.toSet())
            }
            root.optJSONArray(KEY_UNHIDDEN_NON_TV)?.toStringList()?.let {
                putStringSet(KEY_UNHIDDEN_NON_TV, it.toSet())
            }
            root.optJSONArray(KEY_REMOVED_WATCH)?.toStringList()?.let {
                putStringSet(KEY_REMOVED_WATCH, it.toSet())
            }
            root.optJSONArray(KEY_WATCH_ORDER)?.toStringList()?.let {
                putString(KEY_WATCH_ORDER, it.joinToString("\n"))
            }
            root.optJSONArray(KEY_DISABLED_WATCH_PKGS)?.toStringList()?.let {
                putStringSet(KEY_DISABLED_WATCH_PKGS, it.toSet())
            }
            if (root.has(KEY_SHOW_WATCH_ROW)) putBoolean(KEY_SHOW_WATCH_ROW, root.optBoolean(KEY_SHOW_WATCH_ROW, true))
            if (root.has(KEY_APP_COLUMNS)) putInt(KEY_APP_COLUMNS, root.optInt(KEY_APP_COLUMNS, 6).coerceIn(4, 6))
            if (root.has(KEY_BACK_TO_AMBIENT)) putBoolean(KEY_BACK_TO_AMBIENT, root.optBoolean(KEY_BACK_TO_AMBIENT, true))
            if (root.has(KEY_LANGUAGE)) putString(KEY_LANGUAGE, AppLanguage.fromCode(root.optString(KEY_LANGUAGE)).code)
            if (root.has(KEY_THEME_PALETTE)) putString(KEY_THEME_PALETTE, AppThemePalette.fromId(root.optString(KEY_THEME_PALETTE)).id)
            if (root.has(KEY_DYNAMIC_AMBIENT)) putBoolean(KEY_DYNAMIC_AMBIENT, root.optBoolean(KEY_DYNAMIC_AMBIENT, false))
            if (root.has(KEY_WALLPAPER_DIM)) putInt(KEY_WALLPAPER_DIM, root.optInt(KEY_WALLPAPER_DIM, DEFAULT_WALLPAPER_DIM).coerceIn(0, 100))
            if (root.has(KEY_AUTO_LAUNCH_TARGET)) putString(KEY_AUTO_LAUNCH_TARGET, root.optString(KEY_AUTO_LAUNCH_TARGET, ""))
            if (root.has(KEY_AUTO_LAUNCH_BOOT)) putBoolean(KEY_AUTO_LAUNCH_BOOT, root.optBoolean(KEY_AUTO_LAUNCH_BOOT, true))
            if (root.has(KEY_AUTO_LAUNCH_WAKE)) putBoolean(KEY_AUTO_LAUNCH_WAKE, root.optBoolean(KEY_AUTO_LAUNCH_WAKE, true))
            if (root.has(KEY_UPDATE_CHECK_INTERVAL)) {
                putString(KEY_UPDATE_CHECK_INTERVAL, UpdateCheckInterval.fromId(root.optString(KEY_UPDATE_CHECK_INTERVAL)).id)
            }
            if (root.has(KEY_OVERRIDE_HOME)) putBoolean(KEY_OVERRIDE_HOME, root.optBoolean(KEY_OVERRIDE_HOME, false))

            val remoteObj = root.optJSONObject("remote_buttons")
            if (remoteObj != null) {
                for (btn in RemoteButton.entries) {
                    val btnObj = remoteObj.optJSONObject(btn.id) ?: continue
                    putString("$KEY_REMOTE_TARGET_PREFIX${btn.id}", btnObj.optString("target", RemoteButtons.TARGET_DEFAULT))
                    putInt("$KEY_REMOTE_KEYCODE_PREFIX${btn.id}", btnObj.optInt("keyCode", 0))
                    putInt("$KEY_REMOTE_SCANCODE_PREFIX${btn.id}", btnObj.optInt("scanCode", 0))
                }
            }
            root.optJSONArray(KEY_CUSTOM_REMOTE_KEYS)?.toStringList()?.let {
                putString(KEY_CUSTOM_REMOTE_KEYS, it.joinToString("\n"))
            }
        }
        true
    }.getOrDefault(false)

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).mapNotNull { idx -> optString(idx)?.takeIf { it.isNotBlank() } }

    companion object {
        const val DEFAULT_WALLPAPER_DIM = 45
        private const val KEY_ORDER = "order"
        private const val KEY_HIDDEN = "hidden"
        private const val KEY_UNHIDDEN_NON_TV = "unhidden_non_tv"
        private const val KEY_REMOVED_WATCH = "removed_watch"
        private const val KEY_WATCH_ORDER = "watch_order"
        private const val KEY_SHOW_WATCH_ROW = "show_watch_row"
        private const val KEY_DISABLED_WATCH_PKGS = "disabled_watch_pkgs"
        private const val KEY_APP_COLUMNS = "app_columns"
        private const val KEY_BACK_TO_AMBIENT = "back_to_ambient"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME_PALETTE = "theme_palette"
        private const val KEY_DYNAMIC_AMBIENT = "dynamic_ambient"
        private const val KEY_WALLPAPER_DIM = "wallpaper_dim"
        private const val KEY_AUTO_LAUNCH_TARGET = "auto_launch_target"
        private const val KEY_AUTO_LAUNCH_BOOT = "auto_launch_boot"
        private const val KEY_AUTO_LAUNCH_WAKE = "auto_launch_wake"
        private const val KEY_UPDATE_CHECK_INTERVAL = "update_check_interval"
        private const val KEY_LAST_UPDATE_CHECK_MS = "last_update_check_ms"
        private const val KEY_AVAILABLE_UPDATE_VERSION = "available_update_version"
        private const val KEY_OVERRIDE_HOME = "override_home"
        private const val KEY_ADVANCED_VISIBLE = "advanced_settings_visible"
        private const val KEY_REMOTE_TARGET_PREFIX = "remote_target_"
        private const val KEY_REMOTE_KEYCODE_PREFIX = "remote_keycode_"
        private const val KEY_REMOTE_SCANCODE_PREFIX = "remote_scancode_"
        private const val KEY_CUSTOM_REMOTE_KEYS = "custom_remote_keys"
    }
}
