package ai.jhw.appusage.repo

import ai.jhw.appusage.domain.repository.AppUsageRepository
import ai.jhw.appusage.model.AppUsage
import ai.jhw.appusage.model.PowerUsageItem
import ai.jhw.appusage.model.PowerUsageReport
import ai.jhw.appusage.model.RarelyUsedApp
import ai.jhw.appusage.model.RarelyUsedReport
import ai.jhw.appusage.model.UsagePeriod
import ai.jhw.appusage.model.UsageReport
import ai.jhw.appusage.utils.ForegroundTime
import ai.jhw.appusage.utils.LaunchableApps
import ai.jhw.appusage.utils.UsageAccess
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AppUsageRepositoryImpl(private val context: Context) : AppUsageRepository {

    private val usm: UsageStatsManager?
        get() = context.getSystemService(UsageStatsManager::class.java)

    override fun hasUsageAccess(): Boolean = UsageAccess.isGranted(context)

    override fun usageAccessSettingsIntent(): Intent {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // From Android 10 the package URI opens this app's own toggle on most devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.data = Uri.fromParts("package", context.packageName, null)
        }
        return intent
    }

    override suspend fun getUsageReport(period: UsagePeriod): UsageReport =
        withContext(Dispatchers.IO) {
            val end = System.currentTimeMillis()
            val start = when (period) {
                UsagePeriod.TODAY -> startOfToday()
                UsagePeriod.LAST_7_DAYS -> end - TimeUnit.DAYS.toMillis(7)
            }
            val manager = usm
            if (!hasUsageAccess() || manager == null) {
                Log.w(TAG, "No usage access; returning an empty $period report")
                return@withContext UsageReport(period, start, end, emptyList())
            }

            val apps = LaunchableApps.load(context)
            val time = ForegroundTime.compute(manager, start, end)
            val usage = time.timeMillis
                .filter { (pkg, millis) -> millis > 0 && pkg in apps }
                .map { (pkg, millis) ->
                    val app = apps.getValue(pkg)
                    AppUsage(
                        packageName = pkg,
                        label = app.label,
                        foregroundTimeMillis = millis,
                        lastUsedAt = time.lastUsedAt[pkg] ?: 0L,
                        isSystemApp = app.isSystemApp,
                    )
                }
                .sortedByDescending { it.foregroundTimeMillis }
            UsageReport(period, start, end, usage)
        }

    override suspend fun getRarelyUsedApps(thresholdDays: Int): RarelyUsedReport =
        withContext(Dispatchers.IO) {
            val manager = usm
            if (!hasUsageAccess() || manager == null) {
                return@withContext RarelyUsedReport(thresholdDays, emptyList())
            }
            val end = System.currentTimeMillis()
            val start = end - TimeUnit.DAYS.toMillis(thresholdDays.toLong())

            // Aggregated stats are coarse, but "opened at all in N days" only needs the date.
            val lastUsed = manager.queryAndAggregateUsageStats(start, end)
                .mapValues { it.value.lastTimeUsed }

            val apps = LaunchableApps.load(context).values
                // Preinstalled apps cannot be uninstalled, so they are not suggested.
                .filter { !it.isSystemApp }
                // An app installed within the window has not had the chance to be unused.
                .filter { it.installedAt in 1 until start }
                .filter { (lastUsed[it.packageName] ?: 0L) < start }
                .map {
                    RarelyUsedApp(
                        packageName = it.packageName,
                        label = it.label,
                        lastUsedAt = lastUsed[it.packageName] ?: 0L,
                        installedAt = it.installedAt,
                    )
                }
                .sortedBy { if (it.lastUsedAt == 0L) it.installedAt else it.lastUsedAt }
            RarelyUsedReport(thresholdDays, apps)
        }

    override suspend fun getPowerUsage(): PowerUsageReport {
        val today = getUsageReport(UsagePeriod.TODAY)
        val total = today.totalForegroundMillis
        val items = if (total <= 0) {
            emptyList()
        } else {
            today.apps.map {
                PowerUsageItem(
                    packageName = it.packageName,
                    label = it.label,
                    foregroundTimeMillis = it.foregroundTimeMillis,
                    estimatedSharePercent = it.foregroundTimeMillis * 100f / total,
                )
            }
        }
        return PowerUsageReport(today.startMillis, today.endMillis, items)
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val TAG = "AppUsageRepositoryImpl"
    }
}
