package ai.jhw.deviceinfo.utils

/** One read of a setting or a system property: what the app was told, if anything. */
internal sealed interface Reading {
    /** No such setting: it has never been written. */
    data object Absent : Reading

    /** The platform refused the read, or hid the property from the app. */
    data object Unreadable : Reading

    data class Value(val text: String) : Reading
}

/**
 * Turns what the app could read into on, off or unknown (null) for the three debugging rows.
 * "Off" is only ever given on a read that was shown to tell the truth; anything else is unknown.
 *
 * Measured on a Pixel 6 Pro running Android 17 (2026-10-27) with Developer options and Wireless
 * debugging on, once with USB (Universal Serial Bus) debugging off and once with it on:
 *
 * | Signal, as the app reads it | USB debugging off | USB debugging on | Truthful |
 * |---|---|---|---|
 * | `development_settings_enabled` (truly 1) | `"0"` | `"0"` | no: hidden |
 * | `adb_enabled` (truly 0, then 1) | `"0"` | `"0"` | no: hidden |
 * | `adb_wifi_enabled` (truly 1) | `"1"` | `"1"` | yes |
 * | `persist.sys.usb.config` (getprop) | `""` | `"adb"` | yes, both ways |
 * | `init.svc.adbd` (getprop) | `running` | `running` | yes: wireless debugging runs it |
 * | `sys.usb.config`, `sys.usb.state` | not readable | not readable | - |
 *
 * The platform hides only the first two, by answering `"0"`, so a 1 from them is still the truth.
 */
internal object SecurityRules {

    /** `adb_wifi_enabled`: read truthfully, so a 0 is off. */
    fun wirelessDebugging(setting: Reading): Boolean? = when (setting) {
        Reading.Absent -> false
        Reading.Unreadable -> null
        is Reading.Value -> when (setting.text) {
            "1" -> true
            "0" -> false
            else -> null
        }
    }

    /**
     * USB debugging. `persist.sys.usb.config` holds the USB functions the phone keeps across
     * reboots, and "adb" is among them exactly when USB debugging is on; when the property can
     * be read it decides, both ways. Only one thing overrides an "off" from it: a running adbd
     * on a user build with Wireless debugging not known to be on, since something must then be
     * running adbd and the property cannot be trusted.
     *
     * Without the property, `adb_enabled` "1" is on, an absent row is off (the platform's
     * default), and "0" is unknown: that is what a hidden setting reads.
     */
    fun usbDebugging(
        setting: Reading,
        usbConfig: Reading,
        adbdRunsOnUserBuild: Boolean,
        wirelessDebuggingOn: Boolean?,
    ): Boolean? {
        val functions = (usbConfig as? Reading.Value)?.text?.split(',')?.map { it.trim() }
        return when {
            functions != null && "adb" in functions -> true
            setting == Reading.Value("1") -> true
            functions != null -> if (adbdRunsOnUserBuild && wirelessDebuggingOn != true) null else false
            setting == Reading.Absent -> false
            else -> null
        }
    }

    /**
     * Developer options. Either kind of debugging, or adbd running on a user build, can only be
     * on while Developer options are, so any of them proves it. Nothing readable proves them off
     * except an absent row: `development_settings_enabled` "0" is what the platform hides them
     * with.
     */
    fun developerOptions(
        setting: Reading,
        usbDebuggingOn: Boolean?,
        wirelessDebuggingOn: Boolean?,
        adbdRunsOnUserBuild: Boolean,
    ): Boolean? = when {
        setting == Reading.Value("1") -> true
        usbDebuggingOn == true || wirelessDebuggingOn == true || adbdRunsOnUserBuild -> true
        setting == Reading.Absent -> false
        else -> null
    }
}
