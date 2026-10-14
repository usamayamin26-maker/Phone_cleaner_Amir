package ai.jhw.deviceinfo.model

/** The "Device Information" screen: who made the phone and what it runs. */
data class DeviceDetails(
    val brand: String,
    val manufacturer: String,
    val model: String,
    /** The product's code name, e.g. "raven". */
    val device: String,
    val board: String,
    val hardware: String,
    val androidVersion: String,
    val apiLevel: Int,
    /** "2026-09-05"; null below Android 6 or when the build does not say. */
    val securityPatch: String?,
    val buildId: String,
    val fingerprint: String,
    val kernelVersion: String?,
    /** Settings.Secure.ANDROID_ID - unique per app signing key, user and device. */
    val androidId: String?,
    /** Milliseconds since boot, sleep included. */
    val uptimeMillis: Long,
    val isRooted: Boolean,
)
