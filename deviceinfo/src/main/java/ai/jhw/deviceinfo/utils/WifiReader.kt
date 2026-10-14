package ai.jhw.deviceinfo.utils

import ai.jhw.deviceinfo.model.WifiDetails
import ai.jhw.deviceinfo.model.WifiStandard
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

internal object WifiReader {

    private const val UNKNOWN_SSID = "<unknown ssid>"
    private const val HIDDEN_BSSID = "02:00:00:00:00:00"

    // ScanResult.WIFI_STANDARD_11BE arrived in API 33; the value is fixed.
    private const val WIFI_STANDARD_11BE = 8

    fun read(context: Context): WifiDetails {
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)

        val network = connectivity?.activeNetwork
        val capabilities = network?.let { connectivity.getNetworkCapabilities(it) }
        val isConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        val wifiInfo: WifiInfo? = when {
            !isConnected -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                capabilities?.transportInfo as? WifiInfo ?: legacyInfo(wifiManager)
            else -> legacyInfo(wifiManager)
        }
        val links = if (isConnected) network?.let { connectivity.getLinkProperties(it) } else null

        val rssi = wifiInfo?.rssi?.takeIf { it in -127..0 }
        return WifiDetails(
            isWifiEnabled = wifiManager?.isWifiEnabled == true,
            isConnected = isConnected,
            ssid = wifiInfo?.ssid
                ?.removeSurrounding("\"")
                ?.takeUnless { it.isBlank() || it == UNKNOWN_SSID },
            bssid = wifiInfo?.bssid?.takeUnless { it.isBlank() || it == HIDDEN_BSSID },
            rssiDbm = rssi,
            signalLevel = rssi?.let { signalLevel(wifiManager, it) },
            linkSpeedMbps = wifiInfo?.linkSpeed?.takeIf { it > 0 },
            frequencyMhz = wifiInfo?.frequency?.takeIf { it > 0 },
            wifiStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiInfo?.let { standard(it.wifiStandard) }
            } else {
                null
            },
            ipv4Address = links?.address<Inet4Address>(),
            ipv6Address = links?.address<Inet6Address>(),
            gateway = links?.gateway(),
            dnsServers = links?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty(),
        )
    }

    @Suppress("DEPRECATION") // The only source of WifiInfo below API 29.
    private fun legacyInfo(wifiManager: WifiManager?): WifiInfo? = wifiManager?.connectionInfo

    /** 0..4, whatever scale the platform uses. */
    private fun signalLevel(wifiManager: WifiManager?, rssi: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiManager != null) {
            val max = wifiManager.maxSignalLevel.coerceAtLeast(1)
            wifiManager.calculateSignalLevel(rssi) * 4 / max
        } else {
            @Suppress("DEPRECATION")
            WifiManager.calculateSignalLevel(rssi, 5)
        }

    private fun standard(value: Int): WifiStandard = when (value) {
        ScanResult.WIFI_STANDARD_LEGACY -> WifiStandard.LEGACY
        ScanResult.WIFI_STANDARD_11N -> WifiStandard.WIFI_4
        ScanResult.WIFI_STANDARD_11AC -> WifiStandard.WIFI_5
        ScanResult.WIFI_STANDARD_11AX -> WifiStandard.WIFI_6
        WIFI_STANDARD_11BE -> WifiStandard.WIFI_7
        else -> WifiStandard.UNKNOWN
    }

    /** The first global (not link-local) address of the given family. */
    private inline fun <reified T : InetAddress> LinkProperties.address(): String? =
        linkAddresses
            .map { it.address }
            .filterIsInstance<T>()
            .firstOrNull { !it.isLinkLocalAddress && !it.isLoopbackAddress }
            ?.hostAddress
            ?.substringBefore('%')

    private fun LinkProperties.gateway(): String? =
        routes.firstOrNull { route ->
            val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                route.isDefaultRoute
            } else {
                route.destination.prefixLength == 0
            }
            isDefault && route.gateway != null
        }?.gateway?.hostAddress
}
