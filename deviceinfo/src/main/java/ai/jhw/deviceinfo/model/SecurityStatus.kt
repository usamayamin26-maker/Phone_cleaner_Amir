package ai.jhw.deviceinfo.model

/**
 * Settings worth a look on a phone that is not used for development. Read-only: the app never
 * changes them, it only points at the system screen where the user can.
 *
 * The debugging rows are null when nothing the app can read tells the truth about them. "Off" is
 * never guessed: on a Pixel running Android 17 the plain setting reads come back 0 for Developer
 * options and USB debugging while they are in fact on.
 */
data class SecurityStatus(
    /**
     * Developer options are switched on (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, or any kind
     * of debugging being on), or null when that cannot be told.
     */
    val developerOptionsOn: Boolean?,
    /**
     * USB (Universal Serial Bus) debugging is switched on (the persist.sys.usb.config property,
     * or Settings.Global.ADB_ENABLED), or null when that cannot be told.
     */
    val usbDebuggingOn: Boolean?,
    /** Wireless debugging is switched on (adb_wifi_enabled), or null when it cannot be read. */
    val wirelessDebuggingOn: Boolean?,
    /** The active network goes through a VPN (Virtual Private Network). */
    val vpnConnected: Boolean,
)
