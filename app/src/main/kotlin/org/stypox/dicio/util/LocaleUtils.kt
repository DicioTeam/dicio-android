package org.stypox.dicio.util

import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleUtils {

    /**
     * Basically implements Android locale resolution (not sure if exactly the same, probably,
     * though), so it tries, in this order:<br></br>
     * 1. language + country (e.g. `en-US`)<br></br>
     * 2. parent language (e.g. `en`)<br></br>
     * 3. children of parent language (e.g. `en-US`, `en-GB`, `en-UK`, ...) and
     * locale names containing `+` (e.g. `b+en+001`)<br></br>
     * @param availableLocales a list of locales to try to resolve, ordered from most preferred one
     * to least preferred one. The first locale in the list which can be
     * resolved is going to be used.
     * @param supportedLocales a set of all supported locales.
     * @return a non-null [LocaleResolutionResult] with a locale chosen from
     * `availableLocales` and the corresponding locale string.
     * @see [
     * Android locale resolution](https://developer.android.com/guide/topics/resources/multilingual-support)
     *
     * @throws UnsupportedLocaleException if the locale resolution failed.
     */
    @Throws(UnsupportedLocaleException::class)
    fun resolveSupportedLocaleOrThrow(
        availableLocales: LocaleListCompat,
        supportedLocales: Collection<String>
    ): LocaleResolutionResult {
        var unsupportedLocaleException: UnsupportedLocaleException? = null
        for (i in 0 until availableLocales.size()) {
            try {
                val supportedLocaleString = resolveLocaleString(
                    availableLocales[i]!!, supportedLocales
                )
                return LocaleResolutionResult(availableLocales[i]!!, supportedLocaleString)
            } catch (e: UnsupportedLocaleException) {
                if (unsupportedLocaleException == null) {
                    unsupportedLocaleException = e
                }
            }
        }
        if (unsupportedLocaleException == null) {
            throw UnsupportedLocaleException()
        } else {
            throw unsupportedLocaleException
        }
    }

    /**
     * @see resolveSupportedLocaleOrThrow
     */
    @Throws(UnsupportedLocaleException::class)
    fun resolveLocaleString(
        locale: Locale,
        supportedLocales: Collection<String>
    ): String {
        // normalize the locales so that they are lowercase and use a dash as a separator
        val normalizedLocales = supportedLocales.associateBy {
            it.lowercase().replace('_', '-')
        }

        // first try with full locale name (e.g. en-US)
        val full = (locale.language + "-" + locale.country).lowercase()
        normalizedLocales[full]?.let { return it }

        // then try with only base language (e.g. en)
        val onlyLanguage = locale.language.lowercase()
        normalizedLocales[onlyLanguage]?.let { return it }

        // then try with children languages of locale base language (e.g. en-US, en-GB, b+en+001, …)
        for ((supportedLocalePlus, originalSupportedLocale) in normalizedLocales) {
            for (supportedLocale in supportedLocalePlus.split("[+#]".toRegex())) {
                if (supportedLocale.split("-".toRegex(), limit = 2)[0] == onlyLanguage) {
                    return originalSupportedLocale
                }
            }
        }

        throw UnsupportedLocaleException(locale)
    }

    /**
     * Parses a `LANGUAGE` or `LANGUAGE_COUNTRY` string string into a [Locale],
     * e.g. "EN" -> [Locale]`("en")`, "EN_IN" -> [Locale]`("en", "in")`.
     */
    fun parseLanguageCountry(languageCountry: String): Locale {
        val languageCountryArr = languageCountry
            .lowercase()
            .split("[_-]".toRegex())

        return if (languageCountryArr.size == 1) {
            Locale.Builder()
                .setLanguage(languageCountryArr[0])
                .build()
        } else {
            Locale.Builder()
                .setLanguage(languageCountryArr[0])
                .setRegion(languageCountryArr[1])
                .build()
        }
    }

    /**
     * Like [resolveSupportedLocaleOrThrow], but returns null instead of throwing an exception.
     */
    fun resolveSupportedLocale(
        availableLocales: LocaleListCompat,
        supportedLocales: Collection<String>
    ): LocaleResolutionResult? {
        return try {
            resolveSupportedLocaleOrThrow(availableLocales, supportedLocales)
        } catch (_: UnsupportedLocaleException) {
            null
        }
    }

    /**
     * Uses [resolveSupportedLocaleOrThrow] to find a supported locale string in [supportedLocales]
     * matching [currentLocale], and returns it. This is NOT meant to be used for locale resolution
     * when the app starts, but only to select the correct item from a list using the app's current
     * locale (that has already been determined, hence the parameter name [currentLocale]).
     */
    fun resolveSupportedLocale(
        currentLocale: Locale,
        supportedLocales: Collection<String>
    ): String? {
        return resolveSupportedLocale(
            availableLocales = LocaleListCompat.create(currentLocale),
            supportedLocales = supportedLocales
        )?.supportedLocaleString
    }

    /**
     * Uses [resolveSupportedLocale] to find a supported locale string matching [currentLocale] in
     * the keys of [supportedLocalesAndValues], and returns the corresponding value.
     */
    fun <T> resolveValueForSupportedLocale(
        currentLocale: Locale,
        supportedLocalesAndValues: Map<String, T>
    ): T? {
        return resolveSupportedLocale(
            availableLocales = LocaleListCompat.create(currentLocale),
            supportedLocales = supportedLocalesAndValues.keys
        )?.let {
            supportedLocalesAndValues[it.supportedLocaleString]
        }
    }

    /**
     * Returns whether the [currentLocale] matches with any of the [supportedLocales] using
     * [resolveSupportedLocale].
     */
    fun isLocaleSupported(currentLocale: Locale, supportedLocales: List<String>): Boolean {
        return resolveSupportedLocale(
            LocaleListCompat.create(currentLocale),
            supportedLocales
        ) != null
    }

    class UnsupportedLocaleException : Exception {
        constructor(locale: Locale) : super("Unsupported locale: $locale")
        constructor() : super("No locales provided")
    }

    class LocaleResolutionResult(
        var availableLocale: Locale,
        var supportedLocaleString: String
    )
}
