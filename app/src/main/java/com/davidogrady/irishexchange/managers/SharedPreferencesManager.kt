package com.davidogrady.irishexchange.managers
import android.content.Context

class SharedPreferencesManager(context: Context, fileName: String) {

    private val sharedPreferences = context.getSharedPreferences(fileName, 0)
    private val editor = sharedPreferences.edit()

    fun storePreferenceKey(key: String, value: String) {
        editor.putString(key, value)
        editor.apply()
    }

    fun getStoredPreferenceString(key: String) : String? {
       return sharedPreferences.getString(key, "")
    }

    fun removeStoredPreference(key: String) {
        editor.remove(key)
        editor.apply()
    }

}