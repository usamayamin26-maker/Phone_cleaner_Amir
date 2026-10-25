package com.phone.cleaner.utils

import android.content.Context

class SharedPref(context: Context) {

    private val sharedPref = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    private val prefEdit = sharedPref.edit()

    var appLanguage: String
        get() = sharedPref.getString("appLanguage", "en") ?: "en"
        set(value) = prefEdit.putString("appLanguage", value).apply()

    /**
     * When the battery last reached 100% while charging, in epoch milliseconds; 0 until it has been
     * seen once. The system does not keep this, so the Battery Info screen records it itself.
     */
    var lastFullChargeAt: Long
        get() = sharedPref.getLong("lastFullChargeAt", 0L)
        set(value) = prefEdit.putLong("lastFullChargeAt", value).apply()

    /** Set when the user leaves onboarding, by Continue or by Skip; onboarding is not shown again. */
    var isOnboardingDone: Boolean
        get() = sharedPref.getBoolean("isOnboardingDone", false)
        set(value) = prefEdit.putBoolean("isOnboardingDone", value).apply()
}
