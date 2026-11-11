package com.tk.toolkit.tv.launcher.data

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Base64
import com.tk.toolkit.tv.core.PinLockActivity
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data class Wrong(val remainingAttempts: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Int) : PinVerifyResult
}

/**
 * Manages 4-digit PIN hashing/verification, lock policy queries, and temporary unlock sessions
 * across both the launcher UI and [com.tk.toolkit.tv.core.ToolkitAccessibilityService].
 */
object AppLockManager {

    const val PIN_LENGTH = 4
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L
    private const val UNSEEN_GRACE_MS = 3_000L
    private const val DISMISS_COOLDOWN_MS = 1_500L

    val SETTINGS_PACKAGES = setOf(
        "com.android.tv.settings",
        "com.android.settings",
    )

    private data class TempUnlockEntry(
        val grantedAtElapsed: Long,
        @Volatile var seenForeground: Boolean = false,
    )

    private val random = SecureRandom()
    private val tempUnlocks = ConcurrentHashMap<String, TempUnlockEntry>()
    private val dismissCooldownUntil = ConcurrentHashMap<String, Long>()

    @Volatile
    private var failedAttempts = 0

    @Volatile
    private var lockoutUntilElapsed = 0L

    @Volatile
    private var customizeSessionUnlocked = false

    @Volatile
    var activeGuardPromptPackage: String? = null
        private set

    fun isLockActive(context: Context): Boolean = isLockActive(LauncherPrefs(context))

    fun isLockActive(prefs: LauncherPrefs): Boolean =
        prefs.isAppLockEnabled() && prefs.hasAppLockPin()

    fun isPackageLocked(context: Context, packageName: String): Boolean {
        val prefs = LauncherPrefs(context)
        if (!isLockActive(prefs)) return false
        if (packageName in prefs.lockedPackages()) return true
        return prefs.isLockSystemSettingsEnabled() && packageName in SETTINGS_PACKAGES
    }

    fun isSystemSettingsLocked(context: Context): Boolean {
        val prefs = LauncherPrefs(context)
        if (!isLockActive(prefs)) return false
        if (prefs.isLockSystemSettingsEnabled()) return true
        val locked = prefs.lockedPackages()
        return SETTINGS_PACKAGES.any { it in locked }
    }

    fun isCustomizeLocked(context: Context): Boolean {
        val prefs = LauncherPrefs(context)
        return isLockActive(prefs) && prefs.isLockCustomizeEnabled()
    }

    fun isAppActionsLocked(context: Context): Boolean {
        val prefs = LauncherPrefs(context)
        return isLockActive(prefs) && prefs.isLockAppActionsEnabled()
    }

    fun isSystemGuardActive(context: Context): Boolean {
        val prefs = LauncherPrefs(context)
        if (!isLockActive(prefs) || !prefs.isLockSystemGuardEnabled()) return false
        return prefs.lockedPackages().isNotEmpty() || prefs.isLockSystemSettingsEnabled()
    }

    fun monitoredPackages(context: Context): Set<String> {
        val prefs = LauncherPrefs(context)
        if (!isLockActive(prefs) || !prefs.isLockSystemGuardEnabled()) return emptySet()
        return buildSet {
            addAll(prefs.lockedPackages())
            if (prefs.isLockSystemSettingsEnabled()) {
                addAll(SETTINGS_PACKAGES)
            }
        }
    }

    fun isPinFormatValid(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    fun setPin(context: Context, pin: String) {
        require(isPinFormatValid(pin)) { "PIN must be $PIN_LENGTH digits" }
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)
        LauncherPrefs(context).apply {
            saveAppLockPin(hash, salt)
            setAppLockEnabled(true)
        }
        failedAttempts = 0
        lockoutUntilElapsed = 0L
        customizeSessionUnlocked = true
    }

    fun clearPin(context: Context) {
        LauncherPrefs(context).clearAppLockPin()
        failedAttempts = 0
        lockoutUntilElapsed = 0L
        customizeSessionUnlocked = false
        clearTempUnlocks()
    }

    fun currentLockoutRemainingSeconds(): Int {
        val remainingMs = lockoutUntilElapsed - SystemClock.elapsedRealtime()
        if (remainingMs <= 0L) return 0
        return ((remainingMs + 999L) / 1_000L).toInt().coerceAtLeast(1)
    }

    fun verifyPin(context: Context, pin: String): PinVerifyResult {
        val lockedSecs = currentLockoutRemainingSeconds()
        if (lockedSecs > 0) return PinVerifyResult.LockedOut(lockedSecs)

        val prefs = LauncherPrefs(context)
        val storedHash = prefs.appLockPinHash()
        val storedSalt = prefs.appLockPinSalt()
        if (storedHash.isBlank() || storedSalt.isBlank()) return PinVerifyResult.Success

        val candidate = hashPin(pin, storedSalt)
        val matched = MessageDigest.isEqual(
            storedHash.toByteArray(Charsets.UTF_8),
            candidate.toByteArray(Charsets.UTF_8),
        )
        if (matched) {
            failedAttempts = 0
            lockoutUntilElapsed = 0L
            return PinVerifyResult.Success
        }

        failedAttempts += 1
        if (failedAttempts >= MAX_ATTEMPTS) {
            failedAttempts = 0
            lockoutUntilElapsed = SystemClock.elapsedRealtime() + LOCKOUT_DURATION_MS
            return PinVerifyResult.LockedOut((LOCKOUT_DURATION_MS / 1_000L).toInt())
        }
        return PinVerifyResult.Wrong(MAX_ATTEMPTS - failedAttempts)
    }

    fun isCustomizeUnlockedInSession(): Boolean = customizeSessionUnlocked

    fun setCustomizeUnlockedInSession(unlocked: Boolean) {
        customizeSessionUnlocked = unlocked
    }

    fun grantTempUnlock(packageName: String) {
        val now = SystemClock.elapsedRealtime()
        if (packageName in SETTINGS_PACKAGES) {
            SETTINGS_PACKAGES.forEach { tempUnlocks[it] = TempUnlockEntry(now) }
        } else {
            tempUnlocks[packageName] = TempUnlockEntry(now)
        }
        dismissCooldownUntil.remove(packageName)
    }

    fun grantTempUnlockSettings() {
        val now = SystemClock.elapsedRealtime()
        SETTINGS_PACKAGES.forEach {
            tempUnlocks[it] = TempUnlockEntry(now)
            dismissCooldownUntil.remove(it)
        }
    }

    fun isTempUnlocked(packageName: String): Boolean = tempUnlocks.containsKey(packageName)

    fun markPackageSeenForeground(packageName: String) {
        tempUnlocks[packageName]?.seenForeground = true
    }

    fun onReturnedToHome() {
        customizeSessionUnlocked = false
        val now = SystemClock.elapsedRealtime()
        val iterator = tempUnlocks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next().value
            if (entry.seenForeground || (now - entry.grantedAtElapsed) >= UNSEEN_GRACE_MS) {
                iterator.remove()
            }
        }
    }

    fun onScreenOff() {
        customizeSessionUnlocked = false
        clearTempUnlocks()
    }

    fun clearTempUnlocks() {
        tempUnlocks.clear()
        dismissCooldownUntil.clear()
    }

    fun shouldBlockPackageInGuard(context: Context, packageName: String): Boolean {
        if (!isSystemGuardActive(context)) return false
        if (!isPackageLocked(context, packageName)) return false
        if (isTempUnlocked(packageName)) {
            markPackageSeenForeground(packageName)
            return false
        }
        if (activeGuardPromptPackage == packageName) return false
        val cooldownUntil = dismissCooldownUntil[packageName] ?: 0L
        if (SystemClock.elapsedRealtime() < cooldownUntil) return false
        return true
    }

    fun onGuardPromptOpened(packageName: String) {
        activeGuardPromptPackage = packageName
    }

    fun onGuardPromptClosed(packageName: String, unlocked: Boolean) {
        if (activeGuardPromptPackage == packageName) {
            activeGuardPromptPackage = null
        }
        if (unlocked) {
            grantTempUnlock(packageName)
        } else {
            dismissCooldownUntil[packageName] = SystemClock.elapsedRealtime() + DISMISS_COOLDOWN_MS
        }
    }

    fun openGuardForPackage(
        context: Context,
        packageName: String,
        actionType: String = PinLockActivity.ACTION_VERIFY_AND_RESUME,
    ) {
        val intent = PinLockActivity.createIntent(
            context = context,
            packageName = packageName,
            actionType = actionType,
        )
        context.startActivity(intent)
    }

    fun openGuardForSystemSettings(context: Context) {
        val intent = PinLockActivity.createIntent(
            context = context,
            packageName = SETTINGS_PACKAGES.first(),
            actionType = PinLockActivity.ACTION_OPEN_SETTINGS,
        )
        context.startActivity(intent)
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}

