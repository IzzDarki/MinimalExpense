package com.izzdarki.minimalexpense.data

import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.izzdarki.minimalexpense.R

class SettingsManager(context: Context) {

    private val preferences = getPreferences(context)
    private val resources = context.resources

    val isModeBudget get()
    = preferences.getInt(resources.getString(R.string.mode_key), 0) == 1

    val currencySymbol get()
        = preferences.getString(
            resources.getString(R.string.currency_key),
            resources.getString(R.string.currency_default)
        )!!

    companion object {
        private fun getPreferences(context: Context): SharedPreferences {
            if (preferences == null)
                preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences!!
        }

        private var preferences: SharedPreferences? = null
    }
}