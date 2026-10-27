package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.model.SecurityStatus
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import androidx.core.content.getSystemService

/**
 * Reads [SecurityStatus]: two developer settings and the active network's transports. No
 * permission beyond network state.
 *
 * **The two developer settings cannot simply be read.** Measured on a Pixel 6 Pro running
 * Android 17 (2026-10-27), with Developer options and USB (Universal Serial Bus) debugging both
 * on and `adb shell settings get global` answering 1 for both:
 *
 * | What the app asked | What it got |
 * |---|---|
 * | `Settings.Global.getString` | `"0"` |
 * | `Settings.Secure.getString` | `"0"` |
 * | `Settings.Global.getInt(default 0)` | `0` |
 * | `Settings.Global.getInt(default 1)` | `0` - so not the default, a real answer |
 * | every other global setting asked for in the same call (`stay_on_while_plugged_in` 15, `adb_wifi_enabled` 1, `boot_count` 22, `zen_mode`, `device_name`) | the true value |
 *
 * The platform answers these two, and only these two, with a substituted 0 - it hides from apps
 * that the phone is in developer mode. So a 0 does not mean off, and there is nothing in the
 * answer that tells the two apart. That leads to the rule below. [readFlag] has it in full.
 */
internal class SecurityReader(private val context: Context) {

    fun read(): SecurityStatus = SecurityStatus(
        developerOptionsOn = readFlag(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
        usbDebuggingOn = readFlag(Settings.Global.ADB_ENABLED),
        vpnConnected = isVpnActive(),
    )

    /**
     * One of the two developer settings, as true, false or unknown:
     *
     * - **`"1"` is on.** A platform that hides these settings hides the fact that they are *on*,
     *   so it never invents a 1. A 1 is the truth.
     * - **No value at all is off.** The setting has never been written, which is what a phone
     *   whose Developer options were never opened looks like, and the platform's own default for
     *   an absent row is off. A platform that hides the value substitutes a 0; it does not take
     *   the row away.
     * - **`"0"` is unknown.** It is both what a setting that was switched off looks like and what
     *   a hidden one looks like, and the app has no way to tell. Reporting it as off is the one
     *   answer that can be a lie, so it is not given.
     *
     * The value is read as a string on purpose: `getInt` takes a default and hands it back when
     * there is no value, so an absent setting and a real 0 arrive identically and the first rule
     * above cannot be applied at all.
     */
    private fun readFlag(name: String): Boolean? {
        val resolver = context.contentResolver
        // Settings.Secure is where both lived before API 17, and is still answered on some builds.
        @Suppress("DEPRECATION")
        val value = read(name) { Settings.Global.getString(resolver, name) }
            ?: read(name) { Settings.Secure.getString(resolver, name) }
        val result = when (value) {
            null -> false
            "1" -> true
            else -> null
        }
        Log.d(TAG, "$name read as ${value?.let { "\"$it\"" } ?: "no value"} -> ${result ?: "unknown"}")
        return result
    }

    private inline fun read(name: String, get: () -> String?): String? = try {
        get()?.trim()?.takeIf { it.isNotEmpty() }
    } catch (e: SecurityException) {
        // Some builds refuse the read outright instead of answering.
        Log.d(TAG, "$name may not be read", e)
        null
    }

    private fun isVpnActive(): Boolean {
        val connectivity = context.getSystemService<ConnectivityManager>() ?: return false
        val network = connectivity.activeNetwork ?: return false
        return connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }

    private companion object {
        const val TAG = "SecurityReader"
    }
}
