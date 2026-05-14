package com.healthbot.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREF_NAME = "healthbot_prefs"
    private const val PREF_LANG = "language"

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(PREF_LANG, "en") ?: "en"
    }

    fun setLanguage(context: Context, lang: String) {
        getPrefs(context).edit().putString(PREF_LANG, lang).apply()
    }

    fun onAttach(context: Context): Context {
        val lang = getLanguage(context)
        return setLocale(context, lang)
    }

    fun setLocale(context: Context, lang: String): Context {
        setLanguage(context, lang)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}
