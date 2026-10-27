package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.model.SecurityStatus
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.getSystemService
import java.io.IOException

/**
 * Reads [SecurityStatus]: the debugging settings, two system properties and the active network's
 * transports. No permission beyond network state.
 *
 * **The developer settings cannot simply be read.** On Android 17 the platform answers
 * `development_settings_enabled` and `adb_enabled`, and only those two, with a substituted `"0"`:
 * it hides from apps that the phone is in developer mode. Every other setting, including
 * `adb_wifi_enabled`, comes back true. So the rows are worked out from the reads that were
 * measured to tell the truth; [SecurityRules] has the measurements and the rules.
 */
internal class SecurityReader(private val context: Context) {

    fun read(): SecurityStatus {
        val properties = readSystemProperties()
        // On a user build adbd only runs while USB or Wireless debugging is on. Emulators and
        // userdebug builds run it regardless, so there it proves nothing.
        val adbdRunsOnUserBuild = Build.TYPE == "user" && properties[ADBD_STATE_PROPERTY] == "running"
        val usbConfig = properties[USB_CONFIG_PROPERTY]?.let { Reading.Value(it) } ?: Reading.Unreadable

        val wireless = SecurityRules.wirelessDebugging(readSetting(ADB_WIFI_ENABLED))
        val usb = SecurityRules.usbDebugging(
            setting = readSetting(Settings.Global.ADB_ENABLED),
            usbConfig = usbConfig,
            adbdRunsOnUserBuild = adbdRunsOnUserBuild,
            wirelessDebuggingOn = wireless,
        )
        val developer = SecurityRules.developerOptions(
            setting = readSetting(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
            usbDebuggingOn = usb,
            wirelessDebuggingOn = wireless,
            adbdRunsOnUserBuild = adbdRunsOnUserBuild,
        )
        Log.d(
            TAG,
            "$USB_CONFIG_PROPERTY=$usbConfig, $ADBD_STATE_PROPERTY=${properties[ADBD_STATE_PROPERTY]} " +
                "(${Build.TYPE} build) -> developer options ${developer.label()}, " +
                "USB debugging ${usb.label()}, wireless debugging ${wireless.label()}",
        )
        return SecurityStatus(
            developerOptionsOn = developer,
            usbDebuggingOn = usb,
            wirelessDebuggingOn = wireless,
            vpnConnected = isVpnActive(),
        )
    }

    /**
     * A global setting as a [Reading]. It is read as a string on purpose: `getInt` takes a default
     * and hands it back when there is no value, so an absent setting and a real 0 would arrive
     * identically.
     */
    private fun readSetting(name: String): Reading {
        val resolver = context.contentResolver
        var refused = false
        fun get(read: () -> String?): String? = try {
            read()?.trim()?.takeIf { it.isNotEmpty() }
        } catch (e: SecurityException) {
            // Some builds refuse the read outright instead of answering.
            Log.d(TAG, "$name may not be read", e)
            refused = true
            null
        }
        // Settings.Secure is where the first two lived before API 17, and is still answered on
        // some builds.
        @Suppress("DEPRECATION")
        val value = get { Settings.Global.getString(resolver, name) }
            ?: get { Settings.Secure.getString(resolver, name) }
        val reading = when {
            value != null -> Reading.Value(value)
            refused -> Reading.Unreadable
            else -> Reading.Absent
        }
        Log.d(TAG, "$name read as $reading")
        return reading
    }

    /**
     * The two system properties the rules use, from one `getprop` listing. The listing only holds
     * what the app may read, so a property missing from it is unreadable rather than empty. The
     * rest of the listing is dropped unread: it also names other apps.
     */
    private fun readSystemProperties(): Map<String, String> = try {
        val process = ProcessBuilder("getprop").redirectErrorStream(true).start()
        // The listing is read to its end first, so getprop has exited by the time it is waited for.
        val lines = process.inputStream.bufferedReader().use { it.readLines() }
        process.waitFor()
        lines.mapNotNull { PROPERTY_LINE.matchEntire(it.trim()) }
            .map { it.groupValues[1] to it.groupValues[2] }
            .filter { (name, _) -> name == USB_CONFIG_PROPERTY || name == ADBD_STATE_PROPERTY }
            .toMap()
    } catch (e: IOException) {
        Log.d(TAG, "getprop could not be run", e)
        emptyMap()
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        emptyMap()
    }

    private fun isVpnActive(): Boolean {
        val connectivity = context.getSystemService<ConnectivityManager>() ?: return false
        val network = connectivity.activeNetwork ?: return false
        return connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }

    private fun Boolean?.label(): String = when (this) {
        true -> "on"
        false -> "off"
        null -> "unknown"
    }

    private companion object {
        const val TAG = "SecurityReader"

        /** Settings.Global.ADB_WIFI_ENABLED, which the SDK keeps hidden. */
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
        const val USB_CONFIG_PROPERTY = "persist.sys.usb.config"
        const val ADBD_STATE_PROPERTY = "init.svc.adbd"

        /** One `getprop` line: `[name]: [value]`. */
        val PROPERTY_LINE = Regex("""\[([^\]]+)]: \[(.*)]""")
    }
}
