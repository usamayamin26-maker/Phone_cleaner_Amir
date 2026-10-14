package ai.jhw.deviceinfo.model

/**
 * The "WiFi Information" screen. Without location permission Android hides the network name
 * (SSID (Service Set Identifier)) and BSSID (Basic Service Set Identifier), so both are often null;
 * everything else is readable.
 */
data class WifiDetails(
    val isWifiEnabled: Boolean,
    val isConnected: Boolean,
    val ssid: String?,
    val bssid: String?,
    /** Received signal strength in dBm (decibel-milliwatts), e.g. -55. */
    val rssiDbm: Int?,
    /** 0 (none) to 4 (excellent). */
    val signalLevel: Int?,
    val linkSpeedMbps: Int?,
    val frequencyMhz: Int?,
    /** The WiFi generation from API 30; null below it. */
    val wifiStandard: WifiStandard?,
    val ipv4Address: String?,
    val ipv6Address: String?,
    val gateway: String?,
    val dnsServers: List<String>,
)

enum class WifiStandard { LEGACY, WIFI_4, WIFI_5, WIFI_6, WIFI_7, UNKNOWN }
