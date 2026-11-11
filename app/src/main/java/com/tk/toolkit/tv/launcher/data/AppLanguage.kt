package com.tk.toolkit.tv.launcher.data

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.tk.toolkit.tv.R
import java.util.Locale

/**
 * Supported display languages in Toolkit Launcher.
 *
 * To contribute a new language translation:
 * 1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<code>/strings.xml`
 *    and translate the string values.
 * 2. Add a corresponding entry to [AppLanguage] below.
 */
enum class AppLanguage(
    val code: String,
    val localeTag: String,
    val nativeName: String,
    @param:StringRes val nameRes: Int,
) {
    VI(code = "vi", localeTag = "vi-VN", nativeName = "Tiếng Việt", nameRes = R.string.lang_vi),
    EN(code = "en", localeTag = "en-US", nativeName = "English", nameRes = R.string.lang_en),
    ;

    val locale: Locale get() = Locale.forLanguageTag(localeTag)

    companion object {
        val DEFAULT = VI

        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DEFAULT
    }
}

object LocaleHelper {

    /**
     * Returns a [Context] configured with the user's selected [AppLanguage] (or the saved
     * preference if [languageCode] is not provided).
     */
    fun wrapContext(
        base: Context,
        languageCode: String = LauncherPrefs(base).language(),
    ): Context {
        val appLanguage = AppLanguage.fromCode(languageCode)
        val locale = appLanguage.locale
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(localeList)
        val configContext = base.createConfigurationContext(config)
        return object : ContextWrapper(base) {
            override fun getResources(): Resources = configContext.resources
            override fun getAssets(): AssetManager = configContext.assets
        }
    }

    /**
     * Provides localized [LocalContext] and [LocalConfiguration] inside a composition or [androidx.compose.ui.window.Dialog]
     * (since Compose's Dialog creates a separate window whose root ComposeView resets LocalContext to the Activity base context).
     */
    @Composable
    fun ProvideAppLocale(
        languageCode: String? = null,
        content: @Composable () -> Unit,
    ) {
        val baseContext = LocalContext.current
        val effectiveCode = languageCode ?: LauncherPrefs(baseContext).language()
        val localizedContext = remember(baseContext, effectiveCode) {
            wrapContext(baseContext, effectiveCode)
        }
        val localizedConfiguration = remember(localizedContext, effectiveCode) {
            Configuration(localizedContext.resources.configuration)
        }
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration,
            content = content,
        )
    }
}
