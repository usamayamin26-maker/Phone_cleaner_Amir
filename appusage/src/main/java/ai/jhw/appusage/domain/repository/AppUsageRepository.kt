package ai.jhw.appusage.domain.repository

import ai.jhw.appusage.model.PowerUsageReport
import ai.jhw.appusage.model.RarelyUsedReport
import ai.jhw.appusage.model.UsagePeriod
import ai.jhw.appusage.model.UsageReport
import android.content.Intent

/**
 * App Usage Report and Power Usage. Everything needs Usage access, which the user grants in
 * system settings, never through a runtime dialog: check [hasUsageAccess] first and send the user
 * to [usageAccessSettingsIntent] when it is off. Without it every report comes back empty.
 */
interface AppUsageRepository {

    fun hasUsageAccess(): Boolean

    fun usageAccessSettingsIntent(): Intent

    suspend fun getUsageReport(period: UsagePeriod): UsageReport

    suspend fun getRarelyUsedApps(thresholdDays: Int = DEFAULT_RARELY_USED_DAYS): RarelyUsedReport

    /** Today's estimated power usage per app. See [ai.jhw.appusage.model.PowerUsageItem]. */
    suspend fun getPowerUsage(): PowerUsageReport

    companion object {
        const val DEFAULT_RARELY_USED_DAYS = 30
    }
}
