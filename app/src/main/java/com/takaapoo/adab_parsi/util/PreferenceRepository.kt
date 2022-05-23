package com.takaapoo.adab_parsi.util

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class PreferenceRepository(sharedPreferences: SharedPreferences) {

    private val themePreference = sharedPreferences.getString("theme", "2")

//    private val preferenceChangedListener =
//        SharedPreferences.OnSharedPreferenceChangeListener {sharedPreferences, key ->
//            when (key) {
//                "theme" -> sharedPreferences.getString(key, "2")?.let { setThemeMode(it) }
//            }
//        }

    init {
//        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangedListener)
        themePreference?.let { setThemeMode(it) }
    }

    private fun setThemeMode(theme: String){
        AppCompatDelegate.setDefaultNightMode(when(theme){
            "0" -> AppCompatDelegate.MODE_NIGHT_NO
            "1" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        })
    }


}