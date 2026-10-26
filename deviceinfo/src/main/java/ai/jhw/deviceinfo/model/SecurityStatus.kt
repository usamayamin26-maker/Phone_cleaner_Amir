package ai.jhw.deviceinfo.model

/**
 * Three settings worth a look on a phone that is not used for development. Read-only: the app
 * never changes them, it only points at the system screen where the user can.
 */
data class SecurityStatus(
    /** Developer options are switched on (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED). */
    val developerOptionsOn: Boolean,
    /** USB (Universal Serial Bus) debugging is switched on (Settings.Global.ADB_ENABLED). */
    val usbDebuggingOn: Boolean,
    /** The active network goes through a VPN (Virtual Private Network). */
    val vpnConnected: Boolean,
)
