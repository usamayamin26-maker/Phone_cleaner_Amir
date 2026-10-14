package ai.jhw.appusage.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/** An app with a launcher icon - the apps a usage report is about. */
internal data class LaunchableApp(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val installedAt: Long,
)

internal object LaunchableApps {

    /** Every app with a launcher entry for this user, keyed by package, this app excluded. */
    fun load(context: Context): Map<String, LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved
            .map { it.activityInfo.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .associate { info ->
                info.packageName to LaunchableApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    // A preinstalled app the user updated (Chrome, YouTube) is still a system app.
                    isSystemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    installedAt = firstInstallTime(pm, info.packageName),
                )
            }
    }

    private fun firstInstallTime(pm: PackageManager, packageName: String): Long = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).firstInstallTime
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0).firstInstallTime
        }
    } catch (_: PackageManager.NameNotFoundException) {
        0L
    }
}
