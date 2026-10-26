package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.model.SecurityStatus
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.core.content.getSystemService

/** Reads [SecurityStatus]: two global settings and the active network's transports. No permission beyond network state. */
internal class SecurityReader(private val context: Context) {

    fun read(): SecurityStatus {
        val resolver = context.contentResolver
        return SecurityStatus(
            developerOptionsOn = Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1,
            usbDebuggingOn = Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 1,
            vpnConnected = isVpnActive(),
        )
    }

    private fun isVpnActive(): Boolean {
        val connectivity = context.getSystemService<ConnectivityManager>() ?: return false
        val network = connectivity.activeNetwork ?: return false
        return connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }
}
