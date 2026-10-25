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

    /**
     * Set the first time Home opens and never cleared. Until then every launch is a first run and
     * the splash starts with the Language screen.
     */
    var hasOpenedMainScreen: Boolean
        get() = sharedPref.getBoolean("hasOpenedMainScreen", false)
        set(value) = prefEdit.putBoolean("hasOpenedMainScreen", value).apply()

    /**
     * Set by the Language screen just before it applies the chosen locale, which recreates it.
     * The recreated screen reads it, clears it and moves on once.
     */
    var isLocaleChangePending: Boolean
        get() = sharedPref.getBoolean("isLocaleChangePending", false)
        set(value) = prefEdit.putBoolean("isLocaleChangePending", value).apply()

    /**
     * Set the first time the storage permission dialog is launched and never cleared. Android
     * answers "never asked" and "denied for good" the same way, so this is what tells them apart:
     * asked before, no rationale owed and still not granted means the user has to go to settings.
     */
    var hasAskedStorageAccess: Boolean
        get() = sharedPref.getBoolean("hasAskedStorageAccess", false)
        set(value) = prefEdit.putBoolean("hasAskedStorageAccess", value).apply()
}
