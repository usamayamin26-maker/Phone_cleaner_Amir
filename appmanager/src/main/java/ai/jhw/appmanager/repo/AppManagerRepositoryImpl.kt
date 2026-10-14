package ai.jhw.appmanager.repo

import ai.jhw.appmanager.domain.repository.AppManagerRepository
import ai.jhw.appmanager.model.InstalledApp
import ai.jhw.appmanager.model.InstalledAppsResult
import ai.jhw.appmanager.utils.AppSizeReader
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppManagerRepositoryImpl(private val context: Context) : AppManagerRepository {

    private val pm: PackageManager get() = context.packageManager

    override suspend fun getInstalledApps(includeSystemApps: Boolean): InstalledAppsResult =
        withContext(Dispatchers.IO) {
            val apps = launchableApplications()
                .filter { it.packageName != context.packageName }
                .filter { includeSystemApps || !it.isSystem() }
                .mapNotNull { info ->
                    try {
                        toInstalledApp(info)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.d(TAG, "${info.packageName} was uninstalled while listing")
                        null
                    }
                }
                .sortedByDescending { it.totalBytes }
            InstalledAppsResult(apps)
        }

    override fun isInstalled(packageName: String): Boolean = try {
        packageInfo(packageName)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    override fun uninstallIntent(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))

    override fun appDetailsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun toInstalledApp(info: ApplicationInfo): InstalledApp {
        val pkgInfo = packageInfo(info.packageName)
        val size = AppSizeReader.read(context, info)
        return InstalledApp(
            packageName = info.packageName,
            label = pm.getApplicationLabel(info).toString(),
            versionName = pkgInfo.versionName,
            installedAt = pkgInfo.firstInstallTime,
            updatedAt = pkgInfo.lastUpdateTime,
            isSystemApp = info.isSystem(),
            appBytes = size.appBytes,
            dataBytes = size.dataBytes,
            cacheBytes = size.cacheBytes,
            sizeSource = size.source,
        )
    }

    /** Apps with a launcher entry; the manifest's <queries> makes them visible from API 30. */
    private fun launchableApplications(): List<ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved.map { it.activityInfo.applicationInfo }.distinctBy { it.packageName }
    }

    private fun packageInfo(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }

    /** A preinstalled app the user updated (Chrome, YouTube) is still a system app. */
    private fun ApplicationInfo.isSystem(): Boolean = flags and ApplicationInfo.FLAG_SYSTEM != 0

    private companion object {
        const val TAG = "AppManagerRepositoryImpl"
    }
}
