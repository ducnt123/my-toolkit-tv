package com.tk.toolkit.tv.launcher

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.toolkit.tv.launcher.data.AppRepository
import com.tk.toolkit.tv.launcher.data.ContinueWatching
import com.tk.toolkit.tv.launcher.data.CustomKeyMapping
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.LauncherPrefs
import com.tk.toolkit.tv.launcher.data.RemoteButton
import com.tk.toolkit.tv.launcher.data.RemoteButtonConfig
import com.tk.toolkit.tv.launcher.data.RemoteButtons
import com.tk.toolkit.tv.launcher.data.WatchItem
import com.tk.toolkit.tv.launcher.data.WatchNextRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherState(
    /** Apps shown on the home grid, in the user's order. */
    val apps: List<LauncherApp> = emptyList(),
    /** Apps the user hid, shown under "More Apps", alphabetically. */
    val hiddenApps: List<LauncherApp> = emptyList(),
    /** Continue Watching items whose app is installed and enabled, and that the user hasn't removed. */
    val watchItems: List<WatchItem> = emptyList(),
    /** Whether the Continue Watching row is enabled by the user. */
    val showWatchRow: Boolean = true,
    /** Number of app tiles per row in the Apps grid (4, 5, or 6). */
    val appColumns: Int = 6,
    /** Packages whose Continue Watching items are hidden by the user. */
    val disabledWatchPackages: Set<String> = emptySet(),
    /** Packages that currently have at least one item in Continue Watching (before source filtering). */
    val activeWatchPackages: Set<String> = emptySet(),
    /** Remote button mappings and learned keycodes. */
    val remoteButtons: List<RemoteButtonConfig> = emptyList(),
    /** User-defined custom remote key mappings. */
    val customKeyMappings: List<CustomKeyMapping> = emptyList(),
    /** Whether AccessibilityService overrides the Home button. */
    val overrideHomeKey: Boolean = false,
    /** Whether pressing Back at the first item starts the screensaver / Ambient Mode. */
    val backToAmbient: Boolean = true,
    val loaded: Boolean = false,
    /** App package or Continue Watching key currently being repositioned, if any. */
    val movingKey: String? = null,
    /** Incremented whenever the Home button is pressed to return focus to the first app. */
    val homeRequestCount: Int = 0,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val watchNextRepository = WatchNextRepository(application)
    private val prefs = LauncherPrefs(application)

    private val _state = MutableStateFlow(
        LauncherState(
            showWatchRow = prefs.isWatchRowVisible(),
            appColumns = prefs.appColumns(),
            disabledWatchPackages = prefs.disabledWatchPackages(),
            remoteButtons = prefs.remoteButtonConfigs(),
            customKeyMappings = prefs.customKeyMappings(),
            overrideHomeKey = RemoteButtons.shouldOverrideHomeKey(application),
            backToAmbient = prefs.isBackToAmbientEnabled(),
        ),
    )
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private val refreshDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val watchObserver = watchNextRepository.registerObserver { refresh() }

    init {
        refresh()
    }

    override fun onCleared() {
        watchNextRepository.unregisterObserver(watchObserver)
        super.onCleared()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val all = withContext(refreshDispatcher) { prefs.sort(repository.loadApps()) }
            val (watch, activePkgs) = withContext(refreshDispatcher) { loadWatchData(all) }
            val showWatch = prefs.isWatchRowVisible()
            val columns = prefs.appColumns()
            val disabledWatch = prefs.disabledWatchPackages()
            val remoteConfigs = prefs.remoteButtonConfigs()
            val customMappings = prefs.customKeyMappings()
            val overrideHome = RemoteButtons.shouldOverrideHomeKey(getApplication())
            val backToAmbient = prefs.isBackToAmbientEnabled()
            _state.update { current ->
                // Don't reshuffle under the user while they're moving something.
                if (current.movingKey != null) current
                else split(all).copy(
                    loaded = true,
                    watchItems = watch,
                    showWatchRow = showWatch,
                    appColumns = columns,
                    disabledWatchPackages = disabledWatch,
                    activeWatchPackages = activePkgs,
                    remoteButtons = remoteConfigs,
                    customKeyMappings = customMappings,
                    overrideHomeKey = overrideHome,
                    backToAmbient = backToAmbient,
                    homeRequestCount = current.homeRequestCount,
                )
            }
        }
    }

    private fun split(all: List<LauncherApp>): LauncherState {
        val hidden = prefs.hiddenPackages()
        val (hiddenApps, visible) = all.partition { it.packageName in hidden }
        return LauncherState(apps = visible, hiddenApps = hiddenApps.sortedBy { it.label.lowercase() })
    }

    private fun loadWatchData(allApps: List<LauncherApp>): Pair<List<WatchItem>, Set<String>> {
        val removed = prefs.removedWatchItems()
        val disabledPkgs = prefs.disabledWatchPackages()
        val appsByPackage = allApps.associateBy { it.packageName }
        val systemItems = watchNextRepository.loadWatchNext(appsByPackage)
            .filter { it.key !in removed }
        val pinnedItems = prefs.sortWatchItems(ContinueWatching.pinnedItems)
            .filter { it.key !in removed && (it.packageName in appsByPackage || repository.isInstalled(it.packageName)) }
            .map { item ->
                val appLabel = appsByPackage[item.packageName]?.label
                if (appLabel != null && item.appName.isBlank()) item.copy(appName = appLabel) else item
            }
        val combined = systemItems + pinnedItems
        val activePkgs = combined.mapTo(HashSet()) { it.packageName }
        val visible = combined.filter { it.packageName !in disabledPkgs }
        return visible to activePkgs
    }

    fun setWatchRowVisible(visible: Boolean) {
        prefs.setWatchRowVisible(visible)
        _state.update { it.copy(showWatchRow = visible) }
    }

    fun setAppColumns(columns: Int) {
        val clamped = columns.coerceIn(4, 6)
        prefs.setAppColumns(clamped)
        _state.update { it.copy(appColumns = clamped) }
    }

    fun setRemoteButtonTarget(button: RemoteButton, target: String) {
        prefs.setRemoteButtonTarget(button, target)
        _state.update { it.copy(remoteButtons = prefs.remoteButtonConfigs()) }
    }

    fun setRemoteButtonCustomKey(button: RemoteButton, keyCode: Int, scanCode: Int) {
        prefs.setRemoteButtonCustomKey(button, keyCode, scanCode)
        _state.update {
            it.copy(
                remoteButtons = prefs.remoteButtonConfigs(),
                customKeyMappings = prefs.customKeyMappings(),
            )
        }
    }

    fun upsertCustomKeyMapping(keyCode: Int, scanCode: Int, target: String) {
        prefs.upsertCustomKeyMapping(keyCode, scanCode, target)
        _state.update {
            it.copy(
                remoteButtons = prefs.remoteButtonConfigs(),
                customKeyMappings = prefs.customKeyMappings(),
            )
        }
    }

    fun removeCustomKeyMapping(keyCode: Int, scanCode: Int) {
        prefs.removeCustomKeyMapping(keyCode, scanCode)
        _state.update { it.copy(customKeyMappings = prefs.customKeyMappings()) }
    }

    fun setOverrideHomeKeyEnabled(enabled: Boolean) {
        prefs.setOverrideHomeKeyEnabled(enabled)
        _state.update { it.copy(overrideHomeKey = enabled) }
    }

    fun setBackToAmbientEnabled(enabled: Boolean) {
        prefs.setBackToAmbientEnabled(enabled)
        _state.update { it.copy(backToAmbient = enabled) }
    }

    fun setWatchPackageEnabled(packageName: String, enabled: Boolean) {
        prefs.setWatchPackageEnabled(packageName, enabled)
        _state.update { current ->
            val updatedDisabled = if (enabled) {
                current.disabledWatchPackages - packageName
            } else {
                current.disabledWatchPackages + packageName
            }
            current.copy(disabledWatchPackages = updatedDisabled)
        }
        refresh()
    }

    fun removeWatchItem(item: WatchItem) {
        if (!item.removable) return
        prefs.removeWatchItem(item.key)
        _state.update { current -> current.copy(watchItems = current.watchItems.filterNot { it.key == item.key }) }
        val id = item.watchNextId
        if (id != null) {
            viewModelScope.launch(refreshDispatcher) {
                watchNextRepository.removeFromSystem(id)
            }
        }
    }

    fun clearAllWatchItems() {
        val items = _state.value.watchItems.filter { it.removable }
        if (items.isEmpty()) return
        prefs.removeAllWatchItems(items.map { it.key })
        _state.update { current -> current.copy(watchItems = current.watchItems.filterNot { it.removable }) }
        val ids = items.mapNotNull { it.watchNextId }
        if (ids.isNotEmpty()) {
            viewModelScope.launch(refreshDispatcher) {
                ids.forEach { watchNextRepository.removeFromSystem(it) }
            }
        }
    }

    fun launchIntent(app: LauncherApp) = repository.launchIntent(app)

    fun watchItemIntent(item: WatchItem): Intent? {
        val all = _state.value.apps + _state.value.hiddenApps
        val app = all.firstOrNull { it.packageName == item.packageName }
        return item.intent(fallbackLaunch = { app?.let { repository.launchIntent(it) } })
    }

    /** Starts moving an app (by package) or a Continue Watching item (by key). */
    fun startMove(key: String) = _state.update { it.copy(movingKey = key) }

    /** Shifts the moving tile by [delta] positions within its own section, clamped to its bounds. */
    fun moveBy(delta: Int) = _state.update { current ->
        val key = current.movingKey ?: return@update current
        val appIndex = current.apps.indexOfFirst { it.packageName == key }
        if (appIndex >= 0) return@update current.copy(apps = current.apps.shifted(appIndex, delta))
        val watchIndex = current.watchItems.indexOfFirst { it.key == key }
        if (watchIndex >= 0) return@update current.copy(watchItems = current.watchItems.shifted(watchIndex, delta))
        current
    }

    private fun <T> List<T>.shifted(from: Int, delta: Int): List<T> {
        val to = (from + delta).coerceIn(0, lastIndex)
        return if (to == from) this else toMutableList().apply { add(to, removeAt(from)) }
    }

    fun endMove() {
        _state.update { it.copy(movingKey = null) }
        saveOrder()
    }

    fun onHomePressed() {
        val wasMoving = _state.value.movingKey != null
        _state.update { it.copy(movingKey = null, homeRequestCount = it.homeRequestCount + 1) }
        if (wasMoving) saveOrder()
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        prefs.setHidden(packageName, hidden)
        // Unhidden apps return to their saved position.
        _state.update { current ->
            split(prefs.sort(current.apps + current.hiddenApps)).copy(
                loaded = current.loaded,
                movingKey = current.movingKey,
                watchItems = current.watchItems,
                showWatchRow = current.showWatchRow,
                appColumns = current.appColumns,
                disabledWatchPackages = current.disabledWatchPackages,
                activeWatchPackages = current.activeWatchPackages,
                remoteButtons = current.remoteButtons,
                customKeyMappings = current.customKeyMappings,
                overrideHomeKey = current.overrideHomeKey,
                backToAmbient = current.backToAmbient,
                homeRequestCount = current.homeRequestCount,
            )
        }
    }

    private fun saveOrder() {
        val current = _state.value
        prefs.saveOrder((current.apps + current.hiddenApps).map { it.packageName })
        // Keeps removed and not-installed items' positions after the visible ones.
        val visible = current.watchItems.map { it.key }
        prefs.saveWatchOrder(
            visible + prefs.sortWatchItems(ContinueWatching.pinnedItems).map { it.key }.filter { it !in visible },
        )
    }
}
