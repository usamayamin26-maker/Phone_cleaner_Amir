package ai.jhw.battery.model

/**
 * One reading of the battery, straight from the platform. Values the device does not report are
 * null; the app decides how to show them. Nothing here is user-visible text.
 */
data class BatteryInfo(
    /** 0..100, or null when the platform reports no level. */
    val levelPercent: Int?,
    val health: BatteryHealth,
    val status: BatteryStatus,
    val plugType: BatteryPlugType,
    /** Degrees Celsius, e.g. 34.0. */
    val temperatureCelsius: Float?,
    /** Millivolts, e.g. 4265. */
    val voltageMillivolts: Int?,
    /** The design capacity in mAh (milliampere-hours), e.g. 5000. */
    val capacityMah: Int?,
    /** How [capacityMah] was obtained, so the app can tell an estimate from the spec value. */
    val capacitySource: CapacitySource,
    /** Battery chemistry as the device names it, e.g. "Li-ion". Device data, not app text. */
    val technology: String?,
    /** Instantaneous current in microamperes; negative while discharging on most devices. */
    val currentMicroAmps: Int?,
    /** Charge cycles, reported from Android 14 (API 34) on some devices. */
    val cycleCount: Int?,
    /** Milliseconds until full while charging (API 28+), or null when unknown. */
    val chargeTimeRemainingMillis: Long?,
    val isPowerSaveMode: Boolean,
) {
    val isCharging: Boolean
        get() = status == BatteryStatus.CHARGING || status == BatteryStatus.FULL

    /** True while plugged in and at 100% - the moment the app records as "last fully charged". */
    val isFullyCharged: Boolean
        get() = status == BatteryStatus.FULL ||
            (plugType != BatteryPlugType.NONE && levelPercent == 100)
}

enum class BatteryHealth { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, FAILURE, UNKNOWN }

enum class BatteryStatus { CHARGING, DISCHARGING, NOT_CHARGING, FULL, UNKNOWN }

enum class BatteryPlugType { NONE, AC, USB, WIRELESS, DOCK }

enum class CapacitySource {
    /** The manufacturer's design capacity from the device's power profile. */
    POWER_PROFILE,

    /** Charge counter divided by the current level - an estimate of today's real capacity. */
    CHARGE_COUNTER_ESTIMATE,

    UNKNOWN,
}
