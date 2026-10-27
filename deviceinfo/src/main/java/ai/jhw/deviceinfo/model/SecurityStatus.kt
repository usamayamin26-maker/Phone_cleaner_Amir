package ai.jhw.deviceinfo.model

/**
 * Three settings worth a look on a phone that is not used for development. Read-only: the app
 * never changes them, it only points at the system screen where the user can.
 *
 * The two developer settings are null when the platform does not let the app read them. "Off" is
 * never guessed: on a Pixel running Android 17 the old read came back 0 for both while they were
 * in fact on, which told the user the opposite of the truth.
 */
data class SecurityStatus(
    /**
     * Developer options are switched on (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED), or null
     * when the setting cannot be read.
     */
    val developerOptionsOn: Boolean?,
    /**
     * USB (Universal Serial Bus) debugging is switched on (Settings.Global.ADB_ENABLED), or null
     * when the setting cannot be read.
     */
    val usbDebuggingOn: Boolean?,
    /** The active network goes through a VPN (Virtual Private Network). */
    val vpnConnected: Boolean,
)
