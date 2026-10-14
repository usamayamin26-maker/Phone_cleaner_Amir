package ai.jhw.appusage.model

/** The two report periods of the App Usage Report. */
enum class UsagePeriod {
    /** Since midnight today, in the device's time zone. */
    TODAY,

    /** The last seven days, ending now. */
    LAST_7_DAYS,
}

/** One app's foreground time in a period. Times in milliseconds. */
data class AppUsage(
    val packageName: String,
    /** The app's own name as the device shows it - device data, not app text. */
    val label: String,
    val foregroundTimeMillis: Long,
    /** When the app was last in the foreground, epoch milliseconds; 0 when not in the period. */
    val lastUsedAt: Long,
    val isSystemApp: Boolean,
)

data class UsageReport(
    val period: UsagePeriod,
    val startMillis: Long,
    val endMillis: Long,
    /** Most used first; apps with no foreground time in the period are left out. */
    val apps: List<AppUsage>,
) {
    val totalForegroundMillis: Long get() = apps.sumOf { it.foregroundTimeMillis }
}

/** An installed app nobody has opened for [RarelyUsedReport.thresholdDays] days. */
data class RarelyUsedApp(
    val packageName: String,
    val label: String,
    /** Epoch milliseconds, or 0 when the system has no record of it ever being opened. */
    val lastUsedAt: Long,
    val installedAt: Long,
)

data class RarelyUsedReport(
    val thresholdDays: Int,
    /** Longest unused first; apps installed within the threshold are left out. */
    val apps: List<RarelyUsedApp>,
)

/**
 * Power Usage - an ESTIMATE. Android gives apps no per-app battery figures (the BATTERY_STATS
 * permission is for system apps only), so each app's share is its share of today's screen-on
 * foreground time. The app must label it as an estimate and offer the system's battery usage
 * screen for the real numbers.
 */
data class PowerUsageItem(
    val packageName: String,
    val label: String,
    val foregroundTimeMillis: Long,
    /** 0..100, this app's share of all foreground time in the period. */
    val estimatedSharePercent: Float,
)

data class PowerUsageReport(
    val startMillis: Long,
    val endMillis: Long,
    /** Largest share first. */
    val items: List<PowerUsageItem>,
)
